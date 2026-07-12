package com.library.ui;

import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.model.Staff;
import com.library.service.LibraryException;
import com.library.service.LibraryService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** Optional Swing GUI front-end for the Library Management System. */
public class LibraryGUI extends JFrame {

    private final LibraryService service = new LibraryService();
    private Staff currentStaff;

    private CardLayout cardLayout;
    private JPanel cards;

    // Login fields
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;

    // Dashboard
    private JLabel welcomeLabel;
    private DefaultTableModel booksTableModel;
    private JTable booksTable;
    private DefaultTableModel loansTableModel;
    private JTable loansTable;

    public LibraryGUI() {
        setTitle("Library Management System");
        setSize(950, 650);
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

        JLabel title = new JLabel("Library Management System", SwingConstants.CENTER);
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
        JButton registerBtn = new JButton("Register Staff");
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
            currentStaff = service.login(username, password);
            welcomeLabel.setText("Welcome, " + currentStaff.getFullName() + "  (" + currentStaff.getRole() + ")");
            refreshBooks();
            refreshActiveLoans();
            cardLayout.show(cards, "dashboard");
        } catch (LibraryException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login failed", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doRegisterDialog() {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JTextField fullName = new JTextField();
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"LIBRARIAN", "ADMIN"});

        Object[] fields = {
                "Username:", username,
                "Password:", password,
                "Full name:", fullName,
                "Role:", roleBox
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Register Staff Account", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                Staff staff = service.registerStaff(username.getText().trim(), new String(password.getPassword()),
                        fullName.getText().trim(), (String) roleBox.getSelectedItem());
                JOptionPane.showMessageDialog(this, "Registration successful! You can now log in as '" + staff.getUsername() + "'.");
            } catch (LibraryException ex) {
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

        booksTableModel = new DefaultTableModel(
                new Object[]{"ID", "ISBN", "Title", "Author", "Category", "Available/Total"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        booksTable = new JTable(booksTableModel);
        JScrollPane booksScroll = new JScrollPane(booksTable);
        booksScroll.setBorder(BorderFactory.createTitledBorder("Book Inventory"));

        loansTableModel = new DefaultTableModel(
                new Object[]{"Loan ID", "Book", "Member", "Checkout", "Due", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        loansTable = new JTable(loansTableModel);
        JScrollPane loansScroll = new JScrollPane(loansTable);
        loansScroll.setBorder(BorderFactory.createTitledBorder("Active Loans"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, booksScroll, loansScroll);
        splitPane.setResizeWeight(0.55);
        panel.add(splitPane, BorderLayout.CENTER);

        JPanel actionsTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBookBtn = new JButton("Add Book");
        JButton searchBookBtn = new JButton("Search Books");
        JButton addMemberBtn = new JButton("Register Member");
        JButton searchMemberBtn = new JButton("Search Members");
        addBookBtn.addActionListener(e -> addBookDialog());
        searchBookBtn.addActionListener(e -> searchBooksDialog());
        addMemberBtn.addActionListener(e -> addMemberDialog());
        searchMemberBtn.addActionListener(e -> searchMembersDialog());
        actionsTop.add(addBookBtn);
        actionsTop.add(searchBookBtn);
        actionsTop.add(addMemberBtn);
        actionsTop.add(searchMemberBtn);

        JPanel actionsBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton checkoutBtn = new JButton("Checkout Book");
        JButton returnBtn = new JButton("Return Book");
        JButton overdueBtn = new JButton("View Overdue");
        JButton refreshBtn = new JButton("Refresh");
        JButton logoutBtn = new JButton("Logout");
        checkoutBtn.addActionListener(e -> checkoutDialog());
        returnBtn.addActionListener(e -> returnDialog());
        overdueBtn.addActionListener(e -> overdueDialog());
        refreshBtn.addActionListener(e -> { refreshBooks(); refreshActiveLoans(); });
        logoutBtn.addActionListener(e -> {
            currentStaff = null;
            cardLayout.show(cards, "auth");
        });
        actionsBottom.add(checkoutBtn);
        actionsBottom.add(returnBtn);
        actionsBottom.add(overdueBtn);
        actionsBottom.add(refreshBtn);
        actionsBottom.add(logoutBtn);

        JPanel south = new JPanel(new GridLayout(2, 1));
        south.add(actionsTop);
        south.add(actionsBottom);
        panel.add(south, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshBooks() {
        booksTableModel.setRowCount(0);
        try {
            List<Book> books = service.listAllBooks();
            for (Book b : books) {
                booksTableModel.addRow(new Object[]{
                        b.getBookId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getCategory(),
                        b.getAvailableCopies() + "/" + b.getTotalCopies()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshActiveLoans() {
        loansTableModel.setRowCount(0);
        try {
            List<Loan> loans = service.getAllActiveLoans();
            for (Loan l : loans) {
                loansTableModel.addRow(new Object[]{
                        l.getLoanId(), l.getBookTitle(), l.getMemberName(),
                        l.getCheckoutDate(), l.getDueDate(), l.getStatus()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addBookDialog() {
        JTextField isbn = new JTextField();
        JTextField title = new JTextField();
        JTextField author = new JTextField();
        JTextField category = new JTextField();
        JTextField publisher = new JTextField();
        JTextField year = new JTextField();
        JTextField copies = new JTextField("1");

        Object[] fields = {
                "ISBN:", isbn, "Title:", title, "Author:", author, "Category:", category,
                "Publisher:", publisher, "Publication year:", year, "Copies:", copies
        };
        int result = JOptionPane.showConfirmDialog(this, fields, "Add Book", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                Integer yr = year.getText().trim().isEmpty() ? null : Integer.parseInt(year.getText().trim());
                int copiesVal = Integer.parseInt(copies.getText().trim());
                Book book = service.addBook(isbn.getText().trim(), title.getText().trim(), author.getText().trim(),
                        category.getText().trim(), publisher.getText().trim(), yr, copiesVal);
                JOptionPane.showMessageDialog(this, "Book added with ID " + book.getBookId() + ".");
                refreshBooks();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid number in year/copies.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (LibraryException | SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void searchBooksDialog() {
        String keyword = JOptionPane.showInputDialog(this, "Search keyword (blank for all):");
        if (keyword == null) return;
        booksTableModel.setRowCount(0);
        try {
            List<Book> books = keyword.trim().isEmpty() ? service.listAllBooks() : service.searchBooks(keyword.trim());
            for (Book b : books) {
                booksTableModel.addRow(new Object[]{
                        b.getBookId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getCategory(),
                        b.getAvailableCopies() + "/" + b.getTotalCopies()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addMemberDialog() {
        JTextField name = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();
        Object[] fields = {"Full name:", name, "Email:", email, "Phone:", phone};
        int result = JOptionPane.showConfirmDialog(this, fields, "Register Member", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                Member member = service.addMember(name.getText().trim(), email.getText().trim(), phone.getText().trim());
                JOptionPane.showMessageDialog(this, "Member registered! Number: " + member.getMembershipNo());
            } catch (LibraryException | SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void searchMembersDialog() {
        String keyword = JOptionPane.showInputDialog(this, "Search keyword (name/membership no/email):");
        if (keyword == null || keyword.trim().isEmpty()) return;
        try {
            List<Member> members = service.searchMembers(keyword.trim());
            StringBuilder sb = new StringBuilder();
            for (Member m : members) sb.append(m).append("\n");
            JTextArea area = new JTextArea(sb.length() == 0 ? "No members found." : sb.toString());
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Search Results", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void checkoutDialog() {
        JTextField isbn = new JTextField();
        JTextField memberNo = new JTextField();
        Object[] fields = {"Book ISBN:", isbn, "Member number:", memberNo};
        int result = JOptionPane.showConfirmDialog(this, fields, "Checkout Book", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                Loan loan = service.checkoutBook(isbn.getText().trim(), memberNo.getText().trim());
                JOptionPane.showMessageDialog(this, "Checked out. Loan #" + loan.getLoanId() + ", due " + loan.getDueDate());
                refreshBooks();
                refreshActiveLoans();
            } catch (LibraryException | SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void returnDialog() {
        String loanIdStr = JOptionPane.showInputDialog(this, "Loan ID to return:");
        if (loanIdStr == null || loanIdStr.trim().isEmpty()) return;
        try {
            int loanId = Integer.parseInt(loanIdStr.trim());
            BigDecimal fine = service.returnBook(loanId);
            if (fine.signum() > 0) {
                JOptionPane.showMessageDialog(this, "Book returned. Overdue fine: $" + fine);
            } else {
                JOptionPane.showMessageDialog(this, "Book returned on time. No fine due.");
            }
            refreshBooks();
            refreshActiveLoans();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid loan ID.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (LibraryException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void overdueDialog() {
        try {
            List<Loan> loans = service.getOverdueLoans();
            StringBuilder sb = new StringBuilder();
            for (Loan l : loans) sb.append(l).append("\n");
            JTextArea area = new JTextArea(sb.length() == 0 ? "No overdue loans." : sb.toString());
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Overdue Loans", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibraryGUI().setVisible(true));
    }
}
