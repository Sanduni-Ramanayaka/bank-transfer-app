package com.example.bank_transfer_app.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TransferRequest {

    @NotBlank(message = "Sender account is required.")
    private String senderAccount;

    @NotBlank(message = "Receiver account is required.")
    private String receiverAccount;

    @NotNull(message = "Transfer amount is required.")
    @DecimalMin(
            value = "0.01",
            message = "Amount must be greater than zero."
    )
    private BigDecimal amount;

    private boolean simulateFailure;


    // Getters and setters

    public String getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }


    public String getReceiverAccount() {
        return receiverAccount;
    }

    public void setReceiverAccount(String receiverAccount) {
        this.receiverAccount = receiverAccount;
    }


    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

}
