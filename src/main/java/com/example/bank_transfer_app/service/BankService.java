package com.example.bank_transfer_app.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank_transfer_app.dto.TransferRequest;
import com.example.bank_transfer_app.exception.CustomerNotFoundException;
import com.example.bank_transfer_app.exception.InsufficientBalanceException;
import com.example.bank_transfer_app.exception.InvalidTransferException;
import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.repository.BankTransactionRepository;
import com.example.bank_transfer_app.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    // =========================================
    // CUSTOMER OPERATIONS
    // =========================================

    // Get all customers ordered by name
    public List<Customer> getAllCustomers() {
        return customerRepository.findAllOrdered();
    }

    // Search customers using their names
    public List<Customer> searchCustomers(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return getAllCustomers();
        }

        return customerRepository.searchByName(keyword.trim());
    }

    // Find a customer using account number
    public Customer getCustomer(String accountNumber) {

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new CustomerNotFoundException("Account number is required.");
        }

        return customerRepository
                .getByAccountNumber(accountNumber.trim())
                .orElseThrow(() ->
                        new CustomerNotFoundException(
                                "Customer not found: " + accountNumber
                        )
                );
    }

    // Calculate total balance of all customers
    public BigDecimal getTotalBalance() {

        BigDecimal total = customerRepository.getTotalBalance();

        return total == null ? BigDecimal.ZERO : total;
    }

    // =========================================
    // CREATE CUSTOMER
    // =========================================

    @Transactional
    public Customer createCustomer(
            String accountNumber,
            String name,
            String email,
            BigDecimal openingBalance) {

        // Validate customer details
        if (accountNumber == null || accountNumber.isBlank()
                || name == null || name.isBlank()
                || email == null || email.isBlank()) {

            throw new InvalidTransferException(
                    "Customer details cannot be empty."
            );
        }

        // Validate opening balance
        if (openingBalance == null
                || openingBalance.compareTo(BigDecimal.ZERO) < 0) {

            throw new InvalidTransferException(
                    "Opening balance cannot be negative."
            );
        }

        // Check whether account number already exists
        if (customerRepository
                .getByAccountNumber(accountNumber.trim())
                .isPresent()) {

            throw new InvalidTransferException(
                    "Account number already exists."
            );
        }

        // Check whether email already exists
        if (customerRepository.countEmails(email.trim()) > 0) {

            throw new InvalidTransferException(
                    "Email address already exists."
            );
        }

        // Create and save customer
        Customer customer = new Customer(
                accountNumber.trim(),
                name.trim(),
                email.trim(),
                openingBalance
        );

        return customerRepository.save(customer);
    }

    // =========================================
    // MONEY TRANSFER
    // =========================================

    @Transactional(rollbackFor = Exception.class)
    public void transfer(TransferRequest request) {

        // 1. Validate transfer request
        if (request == null) {
            throw new InvalidTransferException(
                    "Transfer request cannot be empty."
            );
        }

        // 2. Validate transfer amount
        if (request.getAmount() == null
                || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidTransferException(
                    "Transfer amount must be greater than zero."
            );
        }

        // 3. Validate sender and receiver accounts
        String senderAccount = request.getSenderAccount();
        String receiverAccount = request.getReceiverAccount();

        if (senderAccount == null || senderAccount.isBlank()
                || receiverAccount == null || receiverAccount.isBlank()) {

            throw new InvalidTransferException(
                    "Sender and receiver accounts are required."
            );
        }

        senderAccount = senderAccount.trim();
        receiverAccount = receiverAccount.trim();

        // 4. Sender and receiver cannot be the same
        if (senderAccount.equals(receiverAccount)) {

            throw new InvalidTransferException(
                    "Sender and receiver cannot be the same account."
            );
        }

        // 5. Check whether sender exists
        getCustomer(senderAccount);

        // 6. Check whether receiver exists
        getCustomer(receiverAccount);

        // 7. Debit money from sender account
        int debitResult = customerRepository.debit(
                senderAccount,
                request.getAmount()
        );

        if (debitResult == 0) {

            throw new InsufficientBalanceException(
                    "Insufficient balance in sender account."
            );
        }

        // 8. Simulate system failure after debit
        if (request.isSimulateFailure()) {

            throw new RuntimeException(
                    "SIMULATED SYSTEM FAILURE AFTER DEBIT."
            );
        }

        // 9. Credit money to receiver account
        int creditResult = customerRepository.credit(
                receiverAccount,
                request.getAmount()
        );

        if (creditResult == 0) {

            throw new CustomerNotFoundException(
                    "Receiver account could not be credited."
            );
        }

        // 10. Save successful transaction record
        BankTransaction transaction = new BankTransaction(
                senderAccount,
                receiverAccount,
                request.getAmount(),
                TransactionStatus.SUCCESS,
                "Money transferred successfully."
        );

        transactionRepository.save(transaction);
    }

    // =========================================
    // TRANSACTION REPORTS
    // =========================================

    // Get latest transactions
    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }

    // Get transaction history of an account
    public List<BankTransaction> getAccountHistory(
            String accountNumber) {

        return transactionRepository.findHistory(accountNumber);
    }

    // Count successful transactions
    public long getSuccessfulCount() {

        return transactionRepository.countByStatus(
                TransactionStatus.SUCCESS
        );
    }

    // Count failed transactions
    public long getFailedCount() {

        return transactionRepository.countByStatus(
                TransactionStatus.FAILED
        );
    }
}
