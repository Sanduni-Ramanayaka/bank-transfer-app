package com.example.bank_transfer_app.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_account", length = 20)
    private String senderAccount;

    @Column(name = "receiver_account", length = 20)
    private String receiverAccount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20)
    private TransactionType type;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Required by JPA
    protected BankTransaction() {
    }

    public BankTransaction(
        String senderAccount,
        String receiverAccount,
        BigDecimal amount,
        TransactionStatus status,
        String description,
        TransactionType type) {

    this.senderAccount = senderAccount;
    this.receiverAccount = receiverAccount;
    this.amount = amount;
    this.status = status;
    this.description = description;
    this.type = type;
    this.createdAt = LocalDateTime.now();
}
    @PrePersist
    public void beforeInsert() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public String getReceiverAccount() {
        return receiverAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TransactionType getType() {
    return type;
    }
}
