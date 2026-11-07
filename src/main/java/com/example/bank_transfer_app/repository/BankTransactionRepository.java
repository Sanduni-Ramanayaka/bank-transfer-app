package com.example.bank_transfer_app.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.model.TransactionType;
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

    
    // 5. Get successful transfers above a given amount
    @Query("""
        SELECT t FROM BankTransaction t
        WHERE t.amount > :minAmount
        AND t.status = :status
        ORDER BY t.amount DESC
    """)
    List<BankTransaction> findLargeTransfers(
            @Param("minAmount") java.math.BigDecimal minAmount,
            @Param("status") TransactionStatus status
    );


    // 6. Calculate total successful amount sent by an account
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM BankTransaction t
        WHERE t.senderAccount = :account
        AND t.status = :status
    """)
    java.math.BigDecimal getTotalSentByAccount(
            @Param("account") String account,
            @Param("status") TransactionStatus status
    );


    // Find successful transfers above a given amount
@Query("""
    SELECT t FROM BankTransaction t
    WHERE t.amount > :minAmount
    AND t.status = :status
    AND t.type = :type
    ORDER BY t.amount DESC
""")
List<BankTransaction> findLargeTransfers(
        @Param("minAmount") BigDecimal minAmount,
        @Param("status") TransactionStatus status,
        @Param("type") TransactionType type
);

// Calculate total successful transfers sent by an account
@Query("""
    SELECT COALESCE(SUM(t.amount), 0)
    FROM BankTransaction t
    WHERE t.senderAccount = :account
    AND t.status = :status
    AND t.type = :type
""")
BigDecimal getTotalSentByAccount(
        @Param("account") String account,
        @Param("status") TransactionStatus status,
        @Param("type") TransactionType type
);

}
