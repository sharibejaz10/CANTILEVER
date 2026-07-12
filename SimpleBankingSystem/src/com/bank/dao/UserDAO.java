package com.bank.dao;

import com.bank.db.DBConnection;
import com.bank.model.User;

import java.sql.*;

public class UserDAO {

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE username = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public User createUser(String username, String passwordHash, String fullName, String email, String phone) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, phone) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, fullName);
            ps.setString(4, email);
            ps.setString(5, phone);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    User user = new User(keys.getInt(1), username, fullName, email, phone);
                    return user;
                }
            }
        }
        return null;
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone")
        );
        user.setPasswordHash(rs.getString("password_hash"));
        return user;
    }
}
