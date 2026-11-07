package com.example.bank_transfer_app.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank_transfer_app.model.Customer;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    // 1. Find customer by account number
    @Query("""
        SELECT c FROM Customer c
        WHERE c.accountNumber = :accountNumber
    """)
    Optional<Customer> getByAccountNumber(
            @Param("accountNumber") String accountNumber
    );


    // 2. Count customer accounts
    @Query("""
        SELECT COUNT(c) FROM Customer c
    """)
    long countAccounts();


    // 3. Count customers with a particular email
    @Query("""
        SELECT COUNT(c) FROM Customer c
        WHERE LOWER(c.email) = LOWER(:email)
    """)
    long countEmails(
            @Param("email") String email
    );


    // 4. Get all customers ordered by name
    @Query("""
        SELECT c FROM Customer c
        ORDER BY c.name ASC
    """)
    List<Customer> findAllOrdered();


    // 5. Search customers by name
    @Query("""
        SELECT c FROM Customer c
        WHERE LOWER(c.name)
        LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<Customer> searchByName(
            @Param("keyword") String keyword
    );


    // 6. Calculate the total balance
    @Query("""
        SELECT SUM(c.balance) FROM Customer c
    """)
    BigDecimal getTotalBalance();


    // 7. Count customers using native SQL
    @Query(
        value = "SELECT COUNT(*) FROM customers",
        nativeQuery = true
    )
    long countCustomersNative();


    // 8. Debit money only if sufficient balance exists
    @Transactional
    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true
    )
    @Query("""
        UPDATE Customer c
        SET c.balance = c.balance - :amount
        WHERE c.accountNumber = :accountNumber
        AND c.balance >= :amount
    """)
    int debit(
            @Param("accountNumber") String accountNumber,
            @Param("amount") BigDecimal amount
    );


    // 9. Credit money to an account
    @Transactional
    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true
    )
    @Query("""
        UPDATE Customer c
        SET c.balance = c.balance + :amount
        WHERE c.accountNumber = :accountNumber
    """)
    int credit(
            @Param("accountNumber") String accountNumber,
            @Param("amount") BigDecimal amount
    );

}
