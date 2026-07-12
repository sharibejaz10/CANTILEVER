package com.bank.ui;

import com.bank.db.DBConnection;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.service.BankingException;
import com.bank.service.BankingService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class ConsoleApp {

    private static final Scanner sc = new Scanner(System.in);
    private static final BankingService service = new BankingService();
    private static User currentUser;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Welcome to Simple Banking System");
        System.out.println("=========================================");

        try {
            DBConnection.getConnection(); // fail fast if DB is unreachable
        } catch (RuntimeException e) {
            System.out.println("Could not connect to the database: " + e.getMessage());
            return;
        }

        boolean running = true;
        while (running) {
            if (currentUser == null) {
                running = showAuthMenu();
            } else {
                running = showMainMenu();
            }
        }

        DBConnection.closeConnection();
        System.out.println("Thank you for using Simple Banking System. Goodbye!");
    }

    // ---------- Authentication menu ----------

    private static boolean showAuthMenu() {
        System.out.println("\n1. Register\n2. Login\n3. Exit");
        System.out.print("Choose an option: ");
        String choice = sc.nextLine().trim();

        switch (choice) {
            case "1": register(); break;
            case "2": login(); break;
            case "3": return false;
            default: System.out.println("Invalid option.");
        }
        return true;
    }

    private static void register() {
        try {
            System.out.print("Choose a username: ");
            String username = sc.nextLine().trim();
            System.out.print("Choose a password: ");
            String password = sc.nextLine().trim();
            System.out.print("Full name: ");
            String fullName = sc.nextLine().trim();
            System.out.print("Email: ");
            String email = sc.nextLine().trim();
            System.out.print("Phone: ");
            String phone = sc.nextLine().trim();

            User user = service.register(username, password, fullName, email, phone);
            System.out.println("Registration successful! You can now log in as '" + user.getUsername() + "'.");
        } catch (BankingException e) {
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

            currentUser = service.login(username, password);
            System.out.println("Welcome back, " + currentUser.getFullName() + "!");
        } catch (BankingException e) {
            System.out.println("Login failed: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // ---------- Main banking menu ----------

    private static boolean showMainMenu() {
        System.out.println("\n--- Main Menu (" + currentUser.getUsername() + ") ---");
        System.out.println("1. Open new account");
        System.out.println("2. View my accounts");
        System.out.println("3. Deposit");
        System.out.println("4. Withdraw");
        System.out.println("5. Transfer");
        System.out.println("6. Transaction history");
        System.out.println("7. Logout");
        System.out.println("8. Exit");
        System.out.print("Choose an option: ");
        String choice = sc.nextLine().trim();

        try {
            switch (choice) {
                case "1": openAccount(); break;
                case "2": viewAccounts(); break;
                case "3": deposit(); break;
                case "4": withdraw(); break;
                case "5": transfer(); break;
                case "6": history(); break;
                case "7": currentUser = null; System.out.println("Logged out."); break;
                case "8": return false;
                default: System.out.println("Invalid option.");
            }
        } catch (BankingException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return true;
    }

    private static void openAccount() throws BankingException, SQLException {
        System.out.print("Account type (SAVINGS/CHECKING): ");
        String type = sc.nextLine().trim().toUpperCase();
        if (!type.equals("SAVINGS") && !type.equals("CHECKING")) type = "SAVINGS";

        System.out.print("Opening balance: ");
        BigDecimal balance = readAmount();

        Account account = service.openAccount(currentUser.getUserId(), type, balance);
        System.out.println("Account created successfully! Account number: " + account.getAccountNumber());
    }

    private static void viewAccounts() throws SQLException {
        List<Account> accounts = service.getAccounts(currentUser.getUserId());
        if (accounts.isEmpty()) {
            System.out.println("You have no accounts yet. Open one from the main menu.");
            return;
        }
        System.out.println("Your accounts:");
        for (Account a : accounts) {
            System.out.println("  " + a);
        }
    }

    private static void deposit() throws BankingException, SQLException {
        System.out.print("Account number: ");
        String accNum = sc.nextLine().trim();
        System.out.print("Amount to deposit: ");
        BigDecimal amount = readAmount();

        BigDecimal newBalance = service.deposit(accNum, amount);
        System.out.println("Deposit successful. New balance: " + newBalance);
    }

    private static void withdraw() throws BankingException, SQLException {
        System.out.print("Account number: ");
        String accNum = sc.nextLine().trim();
        System.out.print("Amount to withdraw: ");
        BigDecimal amount = readAmount();

        BigDecimal newBalance = service.withdraw(accNum, amount);
        System.out.println("Withdrawal successful. New balance: " + newBalance);
    }

    private static void transfer() throws BankingException, SQLException {
        System.out.print("From account number: ");
        String from = sc.nextLine().trim();
        System.out.print("To account number: ");
        String to = sc.nextLine().trim();
        System.out.print("Amount to transfer: ");
        BigDecimal amount = readAmount();

        service.transfer(from, to, amount);
        System.out.println("Transfer of " + amount + " completed successfully.");
    }

    private static void history() throws BankingException, SQLException {
        System.out.print("Account number: ");
        String accNum = sc.nextLine().trim();
        List<Transaction> transactions = service.getHistory(accNum, 20);
        if (transactions.isEmpty()) {
            System.out.println("No transactions found for this account.");
            return;
        }
        System.out.println("Recent transactions:");
        for (Transaction t : transactions) {
            System.out.println("  " + t);
        }
    }

    private static BigDecimal readAmount() {
        while (true) {
            try {
                return new BigDecimal(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid number, please enter a valid amount: ");
            }
        }
    }
}
