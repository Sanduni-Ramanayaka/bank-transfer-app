package com.example.bank_transfer_app.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number",
            nullable = false,
            unique = true,
            length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false,
            unique = true,
            length = 150)
    private String email;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Required by JPA
    protected Customer() {
    }

    public Customer(String accountNumber,
                    String name,
                    String email,
                    BigDecimal balance) {

        this.accountNumber = accountNumber;
        this.name = name;
        this.email = email;
        this.balance = balance;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void beforeInsert() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
