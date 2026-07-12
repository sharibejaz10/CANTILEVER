package com.bank.dao;

import com.bank.db.DBConnection;
import com.bank.model.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    public void logTransaction(int accountId, String type, java.math.BigDecimal amount,
                                java.math.BigDecimal balanceAfter, String description, Connection conn) throws SQLException {
        String sql = "INSERT INTO transactions (account_id, transaction_type, amount, balance_after, description) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, type);
            ps.setBigDecimal(3, amount);
            ps.setBigDecimal(4, balanceAfter);
            ps.setString(5, description);
            ps.executeUpdate();
        }
    }

    public List<Transaction> getHistory(int accountId, int limit) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY transaction_date DESC LIMIT ?";
        List<Transaction> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction t = new Transaction(
                            rs.getString("transaction_type"),
                            rs.getBigDecimal("amount"),
                            rs.getBigDecimal("balance_after"),
                            rs.getString("description")
                    );
                    t.setTransactionId(rs.getInt("transaction_id"));
                    t.setAccountId(rs.getInt("account_id"));
                    t.setTransactionDate(rs.getTimestamp("transaction_date"));
                    list.add(t);
                }
            }
        }
        return list;
    }
}
