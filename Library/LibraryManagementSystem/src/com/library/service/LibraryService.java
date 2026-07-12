package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.LoanDAO;
import com.library.dao.MemberDAO;
import com.library.dao.StaffDAO;
import com.library.db.DBConnection;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.model.Staff;
import com.library.util.PasswordUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/**
 * Contains all core library operations: staff login, book inventory management,
 * member management, checkout, return, and loan/fine tracking.
 */
public class LibraryService {

    public static final int LOAN_PERIOD_DAYS = 14;
    public static final BigDecimal FINE_PER_DAY = new BigDecimal("0.50");
    public static final int MAX_ACTIVE_LOANS_PER_MEMBER = 5;

    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final LoanDAO loanDAO = new LoanDAO();
    private final StaffDAO staffDAO = new StaffDAO();
    private final Random random = new Random();

    // ---------- Staff authentication ----------

    public Staff registerStaff(String username, String password, String fullName, String role) throws LibraryException, SQLException {
        if (username == null || username.trim().isEmpty() || password == null || password.length() < 4) {
            throw new LibraryException("Username is required and password must be at least 4 characters.");
        }
        if (staffDAO.usernameExists(username)) {
            throw new LibraryException("Username already exists.");
        }
        String hash = PasswordUtil.hash(password);
        return staffDAO.createStaff(username, hash, fullName, role == null ? "LIBRARIAN" : role);
    }

    public Staff login(String username, String password) throws LibraryException, SQLException {
        Staff staff = staffDAO.findByUsername(username);
        if (staff == null || !PasswordUtil.verify(password, staff.getPasswordHash())) {
            throw new LibraryException("Invalid username or password.");
        }
        return staff;
    }

    // ---------- Book inventory management ----------

    public Book addBook(String isbn, String title, String author, String category,
                         String publisher, Integer year, int copies) throws LibraryException, SQLException {
        if (isbn == null || isbn.trim().isEmpty() || title == null || title.trim().isEmpty()) {
            throw new LibraryException("ISBN and title are required.");
        }
        if (copies < 1) {
            throw new LibraryException("Total copies must be at least 1.");
        }
        if (bookDAO.isbnExists(isbn)) {
            throw new LibraryException("A book with ISBN " + isbn + " already exists.");
        }
        Book book = new Book(0, isbn, title, author, category, publisher, year, copies, copies);
        return bookDAO.addBook(book);
    }

    public void updateBook(Book book) throws LibraryException, SQLException {
        if (book.getTotalCopies() < 1) {
            throw new LibraryException("Total copies must be at least 1.");
        }
        Book existing = bookDAO.findById(book.getBookId());
        if (existing == null) throw new LibraryException("Book not found.");
        int onLoan = existing.getTotalCopies() - existing.getAvailableCopies();
        if (book.getTotalCopies() < onLoan) {
            throw new LibraryException("Cannot set total copies below the " + onLoan + " currently checked out.");
        }
        bookDAO.updateBook(book);
        // Adjust available copies to reflect the new total, keeping the same number on loan.
        int newAvailable = book.getTotalCopies() - onLoan;
        Connection conn = DBConnection.getConnection();
        bookDAO.updateAvailableCopies(book.getBookId(), newAvailable, conn);
    }

    public void deleteBook(int bookId) throws LibraryException, SQLException {
        Book book = bookDAO.findById(bookId);
        if (book == null) throw new LibraryException("Book not found.");
        if (book.getAvailableCopies() < book.getTotalCopies()) {
            throw new LibraryException("Cannot delete a book that has copies currently checked out.");
        }
        bookDAO.deleteBook(bookId);
    }

    public List<Book> searchBooks(String keyword) throws SQLException {
        return bookDAO.search(keyword);
    }

    public List<Book> listAllBooks() throws SQLException {
        return bookDAO.findAll();
    }

    public Book getBookByIsbn(String isbn) throws LibraryException, SQLException {
        Book book = bookDAO.findByIsbn(isbn);
        if (book == null) throw new LibraryException("Book not found for ISBN: " + isbn);
        return book;
    }

    // ---------- Member management ----------

    public Member addMember(String fullName, String email, String phone) throws LibraryException, SQLException {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new LibraryException("Member name is required.");
        }
        String membershipNo = generateMembershipNo();
        Member member = new Member(0, membershipNo, fullName, email, phone, "ACTIVE");
        return memberDAO.addMember(member);
    }

    public List<Member> searchMembers(String keyword) throws SQLException {
        return memberDAO.search(keyword);
    }

    public List<Member> listAllMembers() throws SQLException {
        return memberDAO.findAll();
    }

    public Member getMemberByMembershipNo(String membershipNo) throws LibraryException, SQLException {
        Member member = memberDAO.findByMembershipNo(membershipNo);
        if (member == null) throw new LibraryException("Member not found: " + membershipNo);
        return member;
    }

    public void setMemberStatus(int memberId, String status) throws SQLException {
        memberDAO.updateStatus(memberId, status);
    }

    private String generateMembershipNo() {
        int number = 10000 + random.nextInt(89999);
        return "MEM" + number;
    }

    // ---------- Checkout / Return ----------

    public Loan checkoutBook(String isbn, String membershipNo) throws LibraryException, SQLException {
        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            Book book = bookDAO.findByIsbn(isbn);
            if (book == null) throw new LibraryException("Book not found for ISBN: " + isbn);
            Member member = memberDAO.findByMembershipNo(membershipNo);
            if (member == null) throw new LibraryException("Member not found: " + membershipNo);
            if (!"ACTIVE".equals(member.getStatus())) {
                throw new LibraryException("Member account is not active (status: " + member.getStatus() + ").");
            }

            // Re-fetch the book row with a lock to avoid race conditions on available_copies.
            Book locked = bookDAO.findByIdForUpdate(book.getBookId(), conn);
            if (locked.getAvailableCopies() < 1) {
                throw new LibraryException("No available copies of \"" + locked.getTitle() + "\" right now.");
            }

            List<Loan> activeLoans = loanDAO.findActiveLoansByMember(member.getMemberId());
            if (activeLoans.size() >= MAX_ACTIVE_LOANS_PER_MEMBER) {
                throw new LibraryException("Member has reached the maximum of " + MAX_ACTIVE_LOANS_PER_MEMBER + " active loans.");
            }
            Loan existingLoan = loanDAO.findActiveLoan(locked.getBookId(), member.getMemberId());
            if (existingLoan != null) {
                throw new LibraryException("Member already has this book checked out.");
            }

            LocalDate today = LocalDate.now();
            LocalDate dueDate = today.plusDays(LOAN_PERIOD_DAYS);

            bookDAO.updateAvailableCopies(locked.getBookId(), locked.getAvailableCopies() - 1, conn);
            Loan loan = loanDAO.createLoan(locked.getBookId(), member.getMemberId(), today, dueDate, conn);

            conn.commit();
            loan.setBookTitle(locked.getTitle());
            loan.setMemberName(member.getFullName());
            return loan;
        } catch (SQLException | LibraryException e) {
            safeRollback(conn);
            throw e;
        } finally {
            restoreAutoCommit(conn);
        }
    }

    public BigDecimal returnBook(int loanId) throws LibraryException, SQLException {
        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            Loan loan = loanDAO.findById(loanId, conn);
            if (loan == null) throw new LibraryException("Loan not found: " + loanId);
            if ("RETURNED".equals(loan.getStatus())) {
                throw new LibraryException("This loan has already been returned.");
            }

            LocalDate today = LocalDate.now();
            BigDecimal fine = calculateFine(loan.getDueDate(), today);

            loanDAO.markReturned(loanId, today, fine, conn);

            Book book = bookDAO.findByIdForUpdate(loan.getBookId(), conn);
            bookDAO.updateAvailableCopies(book.getBookId(), book.getAvailableCopies() + 1, conn);

            conn.commit();
            return fine;
        } catch (SQLException | LibraryException e) {
            safeRollback(conn);
            throw e;
        } finally {
            restoreAutoCommit(conn);
        }
    }

    private BigDecimal calculateFine(LocalDate dueDate, LocalDate returnDate) {
        long overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        if (overdueDays <= 0) return BigDecimal.ZERO;
        return FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays));
    }

    // ---------- Loan queries ----------

    public List<Loan> getActiveLoansForMember(String membershipNo) throws LibraryException, SQLException {
        Member member = getMemberByMembershipNo(membershipNo);
        return loanDAO.findActiveLoansByMember(member.getMemberId());
    }

    public List<Loan> getAllActiveLoans() throws SQLException {
        return loanDAO.findAllActiveLoans();
    }

    public List<Loan> getOverdueLoans() throws SQLException {
        return loanDAO.findOverdueLoans();
    }

    public List<Loan> getLoanHistoryForMember(String membershipNo, int limit) throws LibraryException, SQLException {
        Member member = getMemberByMembershipNo(membershipNo);
        return loanDAO.findHistoryByMember(member.getMemberId(), limit);
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
