package com.library.dao;

import com.library.db.DBConnection;
import com.library.model.Staff;

import java.sql.*;

public class StaffDAO {

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT staff_id FROM staff WHERE username = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Staff createStaff(String username, String passwordHash, String fullName, String role) throws SQLException {
        String sql = "INSERT INTO staff (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, fullName);
            ps.setString(4, role);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Staff(keys.getInt(1), username, fullName, role);
                }
            }
        }
        return null;
    }

    public Staff findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM staff WHERE username = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Staff staff = new Staff(rs.getInt("staff_id"), rs.getString("username"),
                            rs.getString("full_name"), rs.getString("role"));
                    staff.setPasswordHash(rs.getString("password_hash"));
                    return staff;
                }
            }
        }
        return null;
    }
}
