package com.library.dao;

import com.library.db.DBConnection;
import com.library.model.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    public Member addMember(Member member) throws SQLException {
        String sql = "INSERT INTO members (membership_no, full_name, email, phone, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, member.getMembershipNo());
            ps.setString(2, member.getFullName());
            ps.setString(3, member.getEmail());
            ps.setString(4, member.getPhone());
            ps.setString(5, member.getStatus());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    member.setMemberId(keys.getInt(1));
                    return member;
                }
            }
        }
        return null;
    }

    public boolean membershipNoExists(String membershipNo) throws SQLException {
        String sql = "SELECT member_id FROM members WHERE membership_no = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, membershipNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Member findById(int memberId) throws SQLException {
        String sql = "SELECT * FROM members WHERE member_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public Member findByMembershipNo(String membershipNo) throws SQLException {
        String sql = "SELECT * FROM members WHERE membership_no = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, membershipNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Member> search(String keyword) throws SQLException {
        String sql = "SELECT * FROM members WHERE full_name LIKE ? OR membership_no LIKE ? OR email LIKE ? ORDER BY full_name";
        List<Member> members = new ArrayList<>();
        String like = "%" + keyword + "%";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) members.add(mapRow(rs));
            }
        }
        return members;
    }

    public List<Member> findAll() throws SQLException {
        String sql = "SELECT * FROM members ORDER BY full_name";
        List<Member> members = new ArrayList<>();
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) members.add(mapRow(rs));
        }
        return members;
    }

    public void updateStatus(int memberId, String status) throws SQLException {
        String sql = "UPDATE members SET status = ? WHERE member_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, memberId);
            ps.executeUpdate();
        }
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        return new Member(
                rs.getInt("member_id"),
                rs.getString("membership_no"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("status")
        );
    }
}
