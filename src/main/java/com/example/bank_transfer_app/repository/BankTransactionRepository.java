package com.example.bank_transfer_app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {


    // 1. Get all transactions, newest first
    @Query("""
        SELECT t FROM BankTransaction t
        ORDER BY t.createdAt DESC
    """)
    List<BankTransaction> findAllLatest();


    // 2. Get transaction history for an account
    @Query("""
        SELECT t FROM BankTransaction t
        WHERE t.senderAccount = :account
        OR t.receiverAccount = :account
        ORDER BY t.createdAt DESC
    """)
    List<BankTransaction> findHistory(
            @Param("account") String account
    );


    // 3. Get transactions by status
    @Query("""
        SELECT t FROM BankTransaction t
        WHERE t.status = :status
        ORDER BY t.createdAt DESC
    """)
    List<BankTransaction> findByStatusLatest(
            @Param("status") TransactionStatus status
    );


    // 4. Count transactions by status
    @Query("""
        SELECT COUNT(t) FROM BankTransaction t
        WHERE t.status = :status
    """)
    long countByStatus(
            @Param("status") TransactionStatus status
    );

}
