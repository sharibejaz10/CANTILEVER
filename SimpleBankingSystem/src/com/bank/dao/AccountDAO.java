package com.bank.dao;

import com.bank.db.DBConnection;
import com.bank.model.Account;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccountDAO {

    public Account createAccount(int userId, String accountNumber, String accountType, BigDecimal openingBalance) throws SQLException {
        String sql = "INSERT INTO accounts (user_id, account_number, account_type, balance) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, accountNumber);
            ps.setString(3, accountType);
            ps.setBigDecimal(4, openingBalance);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Account(keys.getInt(1), userId, accountNumber, accountType, openingBalance, "ACTIVE");
                }
            }
        }
        return null;
    }

    public Account findByAccountNumber(String accountNumber) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public Account findById(int accountId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Account> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE user_id = ?";
        List<Account> accounts = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) accounts.add(mapRow(rs));
            }
        }
        return accounts;
    }

    public void updateBalance(int accountId, BigDecimal newBalance, Connection conn) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        return new Account(
                rs.getInt("account_id"),
                rs.getInt("user_id"),
                rs.getString("account_number"),
                rs.getString("account_type"),
                rs.getBigDecimal("balance"),
                rs.getString("status")
        );
    }
}
