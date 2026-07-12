package com.bank.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Transaction {
    private int transactionId;
    private int accountId;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private Timestamp transactionDate;

    public Transaction() {}

    public Transaction(String transactionType, BigDecimal amount, BigDecimal balanceAfter, String description) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getTransactionDate() { return transactionDate; }
    public void setTransactionDate(Timestamp transactionDate) { this.transactionDate = transactionDate; }

    @Override
    public String toString() {
        return String.format("[%s] %-12s Amount=%.2f  BalanceAfter=%.2f  %s",
                transactionDate, transactionType, amount, balanceAfter,
                description == null ? "" : description);
    }
}
