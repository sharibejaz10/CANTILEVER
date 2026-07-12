package com.bank.ui;

import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.service.BankingException;
import com.bank.service.BankingService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** Optional Swing GUI front-end for the Simple Banking System. */
public class BankingGUI extends JFrame {

    private final BankingService service = new BankingService();
    private User currentUser;

    private CardLayout cardLayout;
    private JPanel cards;

    // Login/register fields
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;

    // Dashboard components
    private JLabel welcomeLabel;
    private DefaultTableModel accountsTableModel;
    private JTable accountsTable;
    private DefaultTableModel historyTableModel;
    private JTable historyTable;

    public BankingGUI() {
        setTitle("Simple Banking System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.add(buildAuthPanel(), "auth");
        cards.add(buildDashboardPanel(), "dashboard");
        add(cards);

        cardLayout.show(cards, "auth");
    }

    // ---------- Auth panel ----------

    private JPanel buildAuthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Simple Banking System", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        panel.add(title, gc);

        gc.gridwidth = 1;
        gc.gridy = 1; gc.gridx = 0;
        panel.add(new JLabel("Username:"), gc);
        gc.gridx = 1;
        loginUsernameField = new JTextField(15);
        panel.add(loginUsernameField, gc);

        gc.gridy = 2; gc.gridx = 0;
        panel.add(new JLabel("Password:"), gc);
        gc.gridx = 1;
        loginPasswordField = new JPasswordField(15);
        panel.add(loginPasswordField, gc);

        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> doRegisterDialog());

        JPanel btnPanel = new JPanel();
        btnPanel.add(loginBtn);
        btnPanel.add(registerBtn);
        gc.gridy = 3; gc.gridx = 0; gc.gridwidth = 2;
        panel.add(btnPanel, gc);

        return panel;
    }

    private void doLogin() {
        String username = loginUsernameField.getText().trim();
        String password = new String(loginPasswordField.getPassword());
        try {
            currentUser = service.login(username, password);
            welcomeLabel.setText("Welcome, " + currentUser.getFullName() + "  (user: " + currentUser.getUsername() + ")");
            refreshAccounts();
            cardLayout.show(cards, "dashboard");
        } catch (BankingException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login failed", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doRegisterDialog() {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JTextField fullName = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();

        Object[] fields = {
                "Username:", username,
                "Password:", password,
                "Full name:", fullName,
                "Email:", email,
                "Phone:", phone
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Register New User", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                User user = service.register(username.getText().trim(), new String(password.getPassword()),
                        fullName.getText().trim(), email.getText().trim(), phone.getText().trim());
                JOptionPane.showMessageDialog(this, "Registration successful! You can now log in as '" + user.getUsername() + "'.");
            } catch (BankingException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration failed", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ---------- Dashboard panel ----------

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        welcomeLabel = new JLabel();
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(welcomeLabel, BorderLayout.NORTH);

        // Accounts table
        accountsTableModel = new DefaultTableModel(new Object[]{"Account #", "Type", "Balance", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        accountsTable = new JTable(accountsTableModel);
        JScrollPane accountsScroll = new JScrollPane(accountsTable);
        accountsScroll.setBorder(BorderFactory.createTitledBorder("My Accounts"));

        // History table
        historyTableModel = new DefaultTableModel(new Object[]{"Date", "Type", "Amount", "Balance After", "Description"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        historyTable = new JTable(historyTableModel);
        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Transaction History"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, accountsScroll, historyScroll);
        splitPane.setResizeWeight(0.4);
        panel.add(splitPane, BorderLayout.CENTER);

        // Action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton openAccountBtn = new JButton("Open Account");
        JButton depositBtn = new JButton("Deposit");
        JButton withdrawBtn = new JButton("Withdraw");
        JButton transferBtn = new JButton("Transfer");
        JButton historyBtn = new JButton("View History");
        JButton refreshBtn = new JButton("Refresh");
        JButton logoutBtn = new JButton("Logout");

        openAccountBtn.addActionListener(e -> openAccountDialog());
        depositBtn.addActionListener(e -> depositDialog());
        withdrawBtn.addActionListener(e -> withdrawDialog());
        transferBtn.addActionListener(e -> transferDialog());
        historyBtn.addActionListener(e -> historyDialog());
        refreshBtn.addActionListener(e -> refreshAccounts());
        logoutBtn.addActionListener(e -> {
            currentUser = null;
            cardLayout.show(cards, "auth");
        });

        actions.add(openAccountBtn);
        actions.add(depositBtn);
        actions.add(withdrawBtn);
        actions.add(transferBtn);
        actions.add(historyBtn);
        actions.add(refreshBtn);
        actions.add(logoutBtn);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshAccounts() {
        accountsTableModel.setRowCount(0);
        try {
            List<Account> accounts = service.getAccounts(currentUser.getUserId());
            for (Account a : accounts) {
                accountsTableModel.addRow(new Object[]{
                        a.getAccountNumber(), a.getAccountType(), a.getBalance(), a.getStatus()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openAccountDialog() {
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"SAVINGS", "CHECKING"});
        JTextField balanceField = new JTextField("0.00");
        Object[] fields = {"Account type:", typeBox, "Opening balance:", balanceField};

        int result = JOptionPane.showConfirmDialog(this, fields, "Open New Account", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                BigDecimal balance = new BigDecimal(balanceField.getText().trim());
                Account account = service.openAccount(currentUser.getUserId(), (String) typeBox.getSelectedItem(), balance);
                JOptionPane.showMessageDialog(this, "Account created! Number: " + account.getAccountNumber());
                refreshAccounts();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid balance amount.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (BankingException | SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void depositDialog() {
        JTextField accField = new JTextField();
        JTextField amountField = new JTextField();
        Object[] fields = {"Account number:", accField, "Amount:", amountField};
        int result = JOptionPane.showConfirmDialog(this, fields, "Deposit", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                BigDecimal amount = new BigDecimal(amountField.getText().trim());
                BigDecimal newBalance = service.deposit(accField.getText().trim(), amount);
                JOptionPane.showMessageDialog(this, "Deposit successful. New balance: " + newBalance);
                refreshAccounts();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (BankingException | SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void withdrawDialog() {
        JTextField accField = new JTextField();
        JTextField amountField = new JTextField();
        Object[] fields = {"Account number:", accField, "Amount:", amountField};
        int result = JOptionPane.showConfirmDialog(this, fields, "Withdraw", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                BigDecimal amount = new BigDecimal(amountField.getText().trim());
                BigDecimal newBalance = service.withdraw(accField.getText().trim(), amount);
                JOptionPane.showMessageDialog(this, "Withdrawal successful. New balance: " + newBalance);
                refreshAccounts();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (BankingException | SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void transferDialog() {
        JTextField fromField = new JTextField();
        JTextField toField = new JTextField();
        JTextField amountField = new JTextField();
        Object[] fields = {"From account:", fromField, "To account:", toField, "Amount:", amountField};
        int result = JOptionPane.showConfirmDialog(this, fields, "Transfer", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                BigDecimal amount = new BigDecimal(amountField.getText().trim());
                service.transfer(fromField.getText().trim(), toField.getText().trim(), amount);
                JOptionPane.showMessageDialog(this, "Transfer completed successfully.");
                refreshAccounts();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (BankingException | SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void historyDialog() {
        String accNum = JOptionPane.showInputDialog(this, "Enter account number:");
        if (accNum == null || accNum.trim().isEmpty()) return;

        historyTableModel.setRowCount(0);
        try {
            List<Transaction> transactions = service.getHistory(accNum.trim(), 50);
            for (Transaction t : transactions) {
                historyTableModel.addRow(new Object[]{
                        t.getTransactionDate(), t.getTransactionType(), t.getAmount(),
                        t.getBalanceAfter(), t.getDescription()
                });
            }
        } catch (BankingException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankingGUI().setVisible(true));
    }
}
