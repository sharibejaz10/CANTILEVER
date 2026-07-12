package com.library.dao;

import com.library.db.DBConnection;
import com.library.model.Loan;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO {

    public Loan createLoan(int bookId, int memberId, LocalDate checkoutDate, LocalDate dueDate, Connection conn) throws SQLException {
        String sql = "INSERT INTO loans (book_id, member_id, checkout_date, due_date, status) VALUES (?, ?, ?, ?, 'CHECKED_OUT')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bookId);
            ps.setInt(2, memberId);
            ps.setDate(3, Date.valueOf(checkoutDate));
            ps.setDate(4, Date.valueOf(dueDate));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    Loan loan = new Loan();
                    loan.setLoanId(keys.getInt(1));
                    loan.setBookId(bookId);
                    loan.setMemberId(memberId);
                    loan.setCheckoutDate(checkoutDate);
                    loan.setDueDate(dueDate);
                    loan.setStatus("CHECKED_OUT");
                    loan.setFineAmount(BigDecimal.ZERO);
                    return loan;
                }
            }
        }
        return null;
    }

    public Loan findById(int loanId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM loans WHERE loan_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /** Finds the active (CHECKED_OUT) loan for a given book+member pair, if any. */
    public Loan findActiveLoan(int bookId, int memberId) throws SQLException {
        String sql = "SELECT * FROM loans WHERE book_id = ? AND member_id = ? AND status = 'CHECKED_OUT' ORDER BY loan_id DESC LIMIT 1";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ps.setInt(2, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public void markReturned(int loanId, LocalDate returnDate, BigDecimal fine, Connection conn) throws SQLException {
        String sql = "UPDATE loans SET return_date = ?, status = 'RETURNED', fine_amount = ? WHERE loan_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(returnDate));
            ps.setBigDecimal(2, fine);
            ps.setInt(3, loanId);
            ps.executeUpdate();
        }
    }

    public List<Loan> findActiveLoansByMember(int memberId) throws SQLException {
        String sql = "SELECT l.*, b.title AS book_title, m.full_name AS member_name FROM loans l " +
                "JOIN books b ON l.book_id = b.book_id JOIN members m ON l.member_id = m.member_id " +
                "WHERE l.member_id = ? AND l.status = 'CHECKED_OUT' ORDER BY l.due_date";
        return runJoinedQuery(sql, memberId);
    }

    public List<Loan> findAllActiveLoans() throws SQLException {
        String sql = "SELECT l.*, b.title AS book_title, m.full_name AS member_name FROM loans l " +
                "JOIN books b ON l.book_id = b.book_id JOIN members m ON l.member_id = m.member_id " +
                "WHERE l.status = 'CHECKED_OUT' ORDER BY l.due_date";
        return runJoinedQuery(sql, null);
    }

    public List<Loan> findOverdueLoans() throws SQLException {
        String sql = "SELECT l.*, b.title AS book_title, m.full_name AS member_name FROM loans l " +
                "JOIN books b ON l.book_id = b.book_id JOIN members m ON l.member_id = m.member_id " +
                "WHERE l.status = 'CHECKED_OUT' AND l.due_date < CURDATE() ORDER BY l.due_date";
        return runJoinedQuery(sql, null);
    }

    public List<Loan> findHistoryByMember(int memberId, int limit) throws SQLException {
        String sql = "SELECT l.*, b.title AS book_title, m.full_name AS member_name FROM loans l " +
                "JOIN books b ON l.book_id = b.book_id JOIN members m ON l.member_id = m.member_id " +
                "WHERE l.member_id = ? ORDER BY l.checkout_date DESC LIMIT ?";
        List<Loan> loans = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) loans.add(mapJoinedRow(rs));
            }
        }
        return loans;
    }

    private List<Loan> runJoinedQuery(String sql, Integer memberId) throws SQLException {
        List<Loan> loans = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            if (memberId != null) ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) loans.add(mapJoinedRow(rs));
            }
        }
        return loans;
    }

    private Loan mapRow(ResultSet rs) throws SQLException {
        Loan loan = new Loan();
        loan.setLoanId(rs.getInt("loan_id"));
        loan.setBookId(rs.getInt("book_id"));
        loan.setMemberId(rs.getInt("member_id"));
        loan.setCheckoutDate(rs.getDate("checkout_date").toLocalDate());
        loan.setDueDate(rs.getDate("due_date").toLocalDate());
        Date returnDate = rs.getDate("return_date");
        loan.setReturnDate(returnDate == null ? null : returnDate.toLocalDate());
        loan.setStatus(rs.getString("status"));
        loan.setFineAmount(rs.getBigDecimal("fine_amount"));
        return loan;
    }

    private Loan mapJoinedRow(ResultSet rs) throws SQLException {
        Loan loan = mapRow(rs);
        loan.setBookTitle(rs.getString("book_title"));
        loan.setMemberName(rs.getString("member_name"));
        return loan;
    }
}
