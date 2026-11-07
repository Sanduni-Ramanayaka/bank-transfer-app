package com.example.bank_transfer_app.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.example.bank_transfer_app.model.TransactionType;
import com.example.bank_transfer_app.repository.BankTransactionRepository;
import com.example.bank_transfer_app.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    // Bank account used to receive transfer fees
    private static final String BANK_FEE_ACCOUNT = "999999";

    // Transfer fee percentage: 1%
    private static final BigDecimal TRANSFER_FEE_RATE =
            new BigDecimal("0.01");

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
            throw new CustomerNotFoundException(
                    "Account number is required."
            );
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
    // MONEY TRANSFER WITH 1% FEE
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

        // 5. Validate that the accounts exist
        getCustomer(senderAccount);
        getCustomer(receiverAccount);

        // 6. Calculate the 1% transfer fee
        BigDecimal transferAmount = request.getAmount();

        BigDecimal fee = transferAmount
                .multiply(TRANSFER_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        // Total amount deducted from sender
        BigDecimal totalDebit = transferAmount.add(fee);

        // 7. Debit transfer amount and fee from sender
        int debitResult = customerRepository.debit(
                senderAccount,
                totalDebit
        );

        if (debitResult == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance to complete the transfer "
                    + "including the 1% fee."
            );
        }

        // 8. Simulate system failure to test rollback
        if (request.isSimulateFailure()) {
            throw new RuntimeException(
                    "SIMULATED SYSTEM FAILURE AFTER DEBIT."
            );
        }

        // 9. Credit transfer amount to receiver
        int receiverCreditResult = customerRepository.credit(
                receiverAccount,
                transferAmount
        );

        if (receiverCreditResult == 0) {
            throw new CustomerNotFoundException(
                    "Receiver account could not be credited."
            );
        }

        // 10. Credit transfer fee to bank account
        int feeCreditResult = customerRepository.credit(
                BANK_FEE_ACCOUNT,
                fee
        );

        if (feeCreditResult == 0) {
            throw new CustomerNotFoundException(
                    "Bank fee account could not be credited."
            );
        }

        // 11. Save successful transfer record
        BankTransaction transaction = new BankTransaction(
                senderAccount,
                receiverAccount,
                transferAmount,
                TransactionStatus.SUCCESS,
                "Money transferred successfully. Transfer fee: "
                        + fee,
                TransactionType.TRANSFER
        );

        transactionRepository.save(transaction);
    }

    // =========================================
    // DEPOSIT MONEY
    // =========================================

    @Transactional(rollbackFor = Exception.class)
    public void deposit(
            String accountNumber,
            BigDecimal amount) {

        // Validate deposit amount
        validateAmount(amount);

        // Check whether customer exists
        Customer customer = getCustomer(accountNumber);

        // Credit the customer's account
        int result = customerRepository.credit(
                customer.getAccountNumber(),
                amount
        );

        if (result == 0) {
            throw new CustomerNotFoundException(
                    "Deposit could not be completed."
            );
        }

        // Save deposit transaction record
        BankTransaction transaction = new BankTransaction(
                null,
                customer.getAccountNumber(),
                amount,
                TransactionStatus.SUCCESS,
                "Money deposited successfully.",
                TransactionType.DEPOSIT
        );

        transactionRepository.save(transaction);
    }

    // =========================================
    // WITHDRAW MONEY
    // =========================================

    @Transactional(rollbackFor = Exception.class)
    public void withdraw(
            String accountNumber,
            BigDecimal amount) {

        // Validate withdrawal amount
        validateAmount(amount);

        // Check whether customer exists
        Customer customer = getCustomer(accountNumber);

        // Debit money from the customer's account
        int result = customerRepository.debit(
                customer.getAccountNumber(),
                amount
        );

        if (result == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance for withdrawal."
            );
        }

        // Save withdrawal transaction record
        BankTransaction transaction = new BankTransaction(
                customer.getAccountNumber(),
                null,
                amount,
                TransactionStatus.SUCCESS,
                "Money withdrawn successfully.",
                TransactionType.WITHDRAWAL
        );

        transactionRepository.save(transaction);
    }

    // Validate deposit and withdrawal amounts
    private void validateAmount(BigDecimal amount) {

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidTransferException(
                    "Amount must be greater than zero."
            );
        }
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

    // Get successful transfers above a given amount
    public List<BankTransaction> getLargeTransfers(
            BigDecimal minAmount) {

        if (minAmount == null
                || minAmount.compareTo(BigDecimal.ZERO) < 0) {

            throw new InvalidTransferException(
                    "Minimum amount cannot be negative."
            );
        }

        return transactionRepository.findLargeTransfers(
                minAmount,
                TransactionStatus.SUCCESS,
                TransactionType.TRANSFER
        );
    }

    // Calculate total successful transfer amount sent by an account
    public BigDecimal getTotalSentByAccount(
            String accountNumber) {

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new InvalidTransferException(
                    "Account number is required."
            );
        }

        BigDecimal total = transactionRepository.getTotalSentByAccount(
                accountNumber.trim(),
                TransactionStatus.SUCCESS,
                TransactionType.TRANSFER
        );

        return total == null ? BigDecimal.ZERO : total;
    }
}
