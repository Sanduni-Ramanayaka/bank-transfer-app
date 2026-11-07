package com.example.bank_transfer_app.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.repository.BankTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionAuditService {


    private final BankTransactionRepository transactionRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            String senderAccount,
            String receiverAccount,
            BigDecimal amount,
            String reason) {

        BankTransaction failedTransaction = new BankTransaction(
                senderAccount,
                receiverAccount,
                amount,
                TransactionStatus.FAILED,
                reason
        );

        transactionRepository.save(failedTransaction);

    }

}
