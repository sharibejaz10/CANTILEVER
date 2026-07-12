package com.bank.model;

import java.math.BigDecimal;

public class Account {
    private int accountId;
    private int userId;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String status;

    public Account() {}

    public Account(int accountId, int userId, String accountNumber, String accountType,
                    BigDecimal balance, String status) {
        this.accountId = accountId;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.status = status;
    }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Account[%s] Type=%s Balance=%.2f Status=%s",
                accountNumber, accountType, balance, status);
    }
}
