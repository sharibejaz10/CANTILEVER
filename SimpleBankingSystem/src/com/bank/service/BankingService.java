package com.bank.service;

import com.bank.dao.AccountDAO;
import com.bank.dao.TransactionDAO;
import com.bank.dao.UserDAO;
import com.bank.db.DBConnection;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.util.PasswordUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

/**
 * Contains all core banking operations: registration, login, account creation,
 * deposit, withdraw, transfer and transaction history.
 */
public class BankingService {

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final Random random = new Random();

    // ---------- User management ----------

    public User register(String username, String password, String fullName, String email, String phone) throws BankingException, SQLException {
        if (username == null || username.trim().isEmpty() || password == null || password.length() < 4) {
            throw new BankingException("Username is required and password must be at least 4 characters.");
        }
        if (userDAO.usernameExists(username)) {
            throw new BankingException("Username already exists.");
        }
        String hash = PasswordUtil.hash(password);
        return userDAO.createUser(username, hash, fullName, email, phone);
    }

    public User login(String username, String password) throws BankingException, SQLException {
        User user = userDAO.findByUsername(username);
        if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new BankingException("Invalid username or password.");
        }
        return user;
    }

    // ---------- Account management ----------

    public Account openAccount(int userId, String accountType, BigDecimal openingBalance) throws BankingException, SQLException {
        if (openingBalance == null || openingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BankingException("Opening balance cannot be negative.");
        }
        String accountNumber = generateAccountNumber();
        return accountDAO.createAccount(userId, accountNumber, accountType, openingBalance);
    }

    public List<Account> getAccounts(int userId) throws SQLException {
        return accountDAO.findByUserId(userId);
    }

    public Account getAccount(String accountNumber) throws BankingException, SQLException {
        Account account = accountDAO.findByAccountNumber(accountNumber);
        if (account == null) throw new BankingException("Account not found: " + accountNumber);
        return account;
    }

    private String generateAccountNumber() {
        // 10-digit account number
        long number = 1000000000L + (long) (random.nextDouble() * 8999999999L);
        return String.valueOf(number);
    }

    // ---------- Transactions ----------

    public BigDecimal deposit(String accountNumber, BigDecimal amount) throws BankingException, SQLException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Deposit amount must be positive.");
        }
        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            Account account = accountDAO.findByAccountNumber(accountNumber);
            if (account == null) throw new BankingException("Account not found: " + accountNumber);

            BigDecimal newBalance = account.getBalance().add(amount);
            accountDAO.updateBalance(account.getAccountId(), newBalance, conn);
            transactionDAO.logTransaction(account.getAccountId(), "DEPOSIT", amount, newBalance, "Cash deposit", conn);

            conn.commit();
            return newBalance;
        } catch (SQLException | BankingException e) {
            safeRollback(conn);
            throw e;
        } finally {
            restoreAutoCommit(conn);
        }
    }

    public BigDecimal withdraw(String accountNumber, BigDecimal amount) throws BankingException, SQLException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Withdrawal amount must be positive.");
        }
        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            Account account = accountDAO.findByAccountNumber(accountNumber);
            if (account == null) throw new BankingException("Account not found: " + accountNumber);
            if (account.getBalance().compareTo(amount) < 0) {
                throw new BankingException("Insufficient funds. Current balance: " + account.getBalance());
            }

            BigDecimal newBalance = account.getBalance().subtract(amount);
            accountDAO.updateBalance(account.getAccountId(), newBalance, conn);
            transactionDAO.logTransaction(account.getAccountId(), "WITHDRAW", amount, newBalance, "Cash withdrawal", conn);

            conn.commit();
            return newBalance;
        } catch (SQLException | BankingException e) {
            safeRollback(conn);
            throw e;
        } finally {
            restoreAutoCommit(conn);
        }
    }

    /** Transfers funds between two accounts atomically. */
    public void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) throws BankingException, SQLException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Transfer amount must be positive.");
        }
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new BankingException("Cannot transfer to the same account.");
        }

        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            Account from = accountDAO.findByAccountNumber(fromAccountNumber);
            Account to = accountDAO.findByAccountNumber(toAccountNumber);
            if (from == null) throw new BankingException("Source account not found: " + fromAccountNumber);
            if (to == null) throw new BankingException("Destination account not found: " + toAccountNumber);
            if (from.getBalance().compareTo(amount) < 0) {
                throw new BankingException("Insufficient funds. Current balance: " + from.getBalance());
            }

            BigDecimal fromNewBalance = from.getBalance().subtract(amount);
            BigDecimal toNewBalance = to.getBalance().add(amount);

            accountDAO.updateBalance(from.getAccountId(), fromNewBalance, conn);
            accountDAO.updateBalance(to.getAccountId(), toNewBalance, conn);

            transactionDAO.logTransaction(from.getAccountId(), "TRANSFER_OUT", amount, fromNewBalance,
                    "Transfer to " + toAccountNumber, conn);
            transactionDAO.logTransaction(to.getAccountId(), "TRANSFER_IN", amount, toNewBalance,
                    "Transfer from " + fromAccountNumber, conn);

            conn.commit();
        } catch (SQLException | BankingException e) {
            safeRollback(conn);
            throw e;
        } finally {
            restoreAutoCommit(conn);
        }
    }

    public List<Transaction> getHistory(String accountNumber, int limit) throws BankingException, SQLException {
        Account account = accountDAO.findByAccountNumber(accountNumber);
        if (account == null) throw new BankingException("Account not found: " + accountNumber);
        return transactionDAO.getHistory(account.getAccountId(), limit);
    }

    // ---------- Helpers ----------

    private void safeRollback(Connection conn) {
        try {
            if (conn != null) conn.rollback();
        } catch (SQLException ex) {
            System.err.println("Rollback failed: " + ex.getMessage());
        }
    }

    private void restoreAutoCommit(Connection conn) {
        try {
            if (conn != null) conn.setAutoCommit(true);
        } catch (SQLException ex) {
            System.err.println("Failed to restore auto-commit: " + ex.getMessage());
        }
    }
}
