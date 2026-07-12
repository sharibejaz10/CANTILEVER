package com.library.ui;

import com.library.db.DBConnection;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.model.Staff;
import com.library.service.LibraryException;
import com.library.service.LibraryService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class ConsoleApp {

    private static final Scanner sc = new Scanner(System.in);
    private static final LibraryService service = new LibraryService();
    private static Staff currentStaff;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Welcome to Library Management System");
        System.out.println("=========================================");

        try {
            DBConnection.getConnection(); // fail fast if DB is unreachable
        } catch (RuntimeException e) {
            System.out.println("Could not connect to the database: " + e.getMessage());
            return;
        }

        boolean running = true;
        while (running) {
            if (currentStaff == null) {
                running = showAuthMenu();
            } else {
                running = showMainMenu();
            }
        }

        DBConnection.closeConnection();
        System.out.println("Goodbye!");
    }

    // ---------- Authentication menu ----------

    private static boolean showAuthMenu() {
        System.out.println("\n1. Register staff account\n2. Login\n3. Exit");
        System.out.print("Choose an option: ");
        String choice = sc.nextLine().trim();

        switch (choice) {
            case "1": registerStaff(); break;
            case "2": login(); break;
            case "3": return false;
            default: System.out.println("Invalid option.");
        }
        return true;
    }

    private static void registerStaff() {
        try {
            System.out.print("Choose a username: ");
            String username = sc.nextLine().trim();
            System.out.print("Choose a password: ");
            String password = sc.nextLine().trim();
            System.out.print("Full name: ");
            String fullName = sc.nextLine().trim();
            System.out.print("Role (ADMIN/LIBRARIAN) [LIBRARIAN]: ");
            String role = sc.nextLine().trim();
            if (role.isEmpty()) role = "LIBRARIAN";

            Staff staff = service.registerStaff(username, password, fullName, role.toUpperCase());
            System.out.println("Registration successful! You can now log in as '" + staff.getUsername() + "'.");
        } catch (LibraryException e) {
            System.out.println("Registration failed: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static void login() {
        try {
            System.out.print("Username: ");
            String username = sc.nextLine().trim();
            System.out.print("Password: ");
            String password = sc.nextLine().trim();

            currentStaff = service.login(username, password);
            System.out.println("Welcome, " + currentStaff.getFullName() + " (" + currentStaff.getRole() + ")");
        } catch (LibraryException e) {
            System.out.println("Login failed: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // ---------- Main menu ----------

    private static boolean showMainMenu() {
        System.out.println("\n--- Main Menu (" + currentStaff.getUsername() + ") ---");
        System.out.println(" 1. Add book");
        System.out.println(" 2. Search / list books");
        System.out.println(" 3. Update book");
        System.out.println(" 4. Delete book");
        System.out.println(" 5. Register member");
        System.out.println(" 6. Search / list members");
        System.out.println(" 7. Checkout book");
        System.out.println(" 8. Return book");
        System.out.println(" 9. View active loans (all)");
        System.out.println("10. View overdue loans");
        System.out.println("11. View member loan history");
        System.out.println("12. Logout");
        System.out.println("13. Exit");
        System.out.print("Choose an option: ");
        String choice = sc.nextLine().trim();

        try {
            switch (choice) {
                case "1": addBook(); break;
                case "2": searchBooks(); break;
                case "3": updateBook(); break;
                case "4": deleteBook(); break;
                case "5": addMember(); break;
                case "6": searchMembers(); break;
                case "7": checkout(); break;
                case "8": returnBook(); break;
                case "9": viewActiveLoans(); break;
                case "10": viewOverdueLoans(); break;
                case "11": viewMemberHistory(); break;
                case "12": currentStaff = null; System.out.println("Logged out."); break;
                case "13": return false;
                default: System.out.println("Invalid option.");
            }
        } catch (LibraryException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return true;
    }

    // ---------- Book operations ----------

    private static void addBook() throws LibraryException, SQLException {
        System.out.print("ISBN: ");
        String isbn = sc.nextLine().trim();
        System.out.print("Title: ");
        String title = sc.nextLine().trim();
        System.out.print("Author: ");
        String author = sc.nextLine().trim();
        System.out.print("Category: ");
        String category = sc.nextLine().trim();
        System.out.print("Publisher: ");
        String publisher = sc.nextLine().trim();
        System.out.print("Publication year: ");
        Integer year = readOptionalInt();
        System.out.print("Number of copies: ");
        int copies = readInt();

        Book book = service.addBook(isbn, title, author, category, publisher, year, copies);
        System.out.println("Book added successfully with ID " + book.getBookId() + ".");
    }

    private static void searchBooks() throws SQLException {
        System.out.print("Search keyword (title/author/ISBN/category, blank for all): ");
        String keyword = sc.nextLine().trim();
        List<Book> books = keyword.isEmpty() ? service.listAllBooks() : service.searchBooks(keyword);
        if (books.isEmpty()) {
            System.out.println("No books found.");
            return;
        }
        for (Book b : books) System.out.println("  " + b);
    }

    private static void updateBook() throws LibraryException, SQLException {
        System.out.print("ISBN of book to update: ");
        String isbn = sc.nextLine().trim();
        Book book = service.getBookByIsbn(isbn);

        System.out.println("Leave blank to keep current value.");
        System.out.print("Title [" + book.getTitle() + "]: ");
        String title = sc.nextLine().trim();
        if (!title.isEmpty()) book.setTitle(title);

        System.out.print("Author [" + book.getAuthor() + "]: ");
        String author = sc.nextLine().trim();
        if (!author.isEmpty()) book.setAuthor(author);

        System.out.print("Category [" + book.getCategory() + "]: ");
        String category = sc.nextLine().trim();
        if (!category.isEmpty()) book.setCategory(category);

        System.out.print("Total copies [" + book.getTotalCopies() + "]: ");
        String copiesStr = sc.nextLine().trim();
        if (!copiesStr.isEmpty()) book.setTotalCopies(Integer.parseInt(copiesStr));

        service.updateBook(book);
        System.out.println("Book updated successfully.");
    }

    private static void deleteBook() throws LibraryException, SQLException {
        System.out.print("ISBN of book to delete: ");
        String isbn = sc.nextLine().trim();
        Book book = service.getBookByIsbn(isbn);
        service.deleteBook(book.getBookId());
        System.out.println("Book deleted.");
    }

    // ---------- Member operations ----------

    private static void addMember() throws LibraryException, SQLException {
        System.out.print("Full name: ");
        String name = sc.nextLine().trim();
        System.out.print("Email: ");
        String email = sc.nextLine().trim();
        System.out.print("Phone: ");
        String phone = sc.nextLine().trim();

        Member member = service.addMember(name, email, phone);
        System.out.println("Member registered! Membership number: " + member.getMembershipNo());
    }

    private static void searchMembers() throws SQLException {
        System.out.print("Search keyword (name/membership no/email, blank for all): ");
        String keyword = sc.nextLine().trim();
        List<Member> members = keyword.isEmpty() ? service.listAllMembers() : service.searchMembers(keyword);
        if (members.isEmpty()) {
            System.out.println("No members found.");
            return;
        }
        for (Member m : members) System.out.println("  " + m);
    }

    // ---------- Checkout / return ----------

    private static void checkout() throws LibraryException, SQLException {
        System.out.print("Book ISBN: ");
        String isbn = sc.nextLine().trim();
        System.out.print("Member number: ");
        String membershipNo = sc.nextLine().trim();

        Loan loan = service.checkoutBook(isbn, membershipNo);
        System.out.println("Checked out successfully. Loan #" + loan.getLoanId() + ", due back on " + loan.getDueDate() + ".");
    }

    private static void returnBook() throws LibraryException, SQLException {
        System.out.print("Loan ID: ");
        int loanId = readInt();
        BigDecimal fine = service.returnBook(loanId);
        if (fine.signum() > 0) {
            System.out.println("Book returned. Overdue fine: $" + fine);
        } else {
            System.out.println("Book returned on time. No fine due.");
        }
    }

    private static void viewActiveLoans() throws SQLException {
        List<Loan> loans = service.getAllActiveLoans();
        if (loans.isEmpty()) {
            System.out.println("No active loans.");
            return;
        }
        for (Loan l : loans) System.out.println("  " + l);
    }

    private static void viewOverdueLoans() throws SQLException {
        List<Loan> loans = service.getOverdueLoans();
        if (loans.isEmpty()) {
            System.out.println("No overdue loans. 🎉");
            return;
        }
        for (Loan l : loans) System.out.println("  " + l);
    }

    private static void viewMemberHistory() throws LibraryException, SQLException {
        System.out.print("Member number: ");
        String membershipNo = sc.nextLine().trim();
        List<Loan> loans = service.getLoanHistoryForMember(membershipNo, 30);
        if (loans.isEmpty()) {
            System.out.println("No loan history for this member.");
            return;
        }
        for (Loan l : loans) System.out.println("  " + l);
    }

    // ---------- Input helpers ----------

    private static int readInt() {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid number, please try again: ");
            }
        }
    }

    private static Integer readOptionalInt() {
        String line = sc.nextLine().trim();
        if (line.isEmpty()) return null;
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
