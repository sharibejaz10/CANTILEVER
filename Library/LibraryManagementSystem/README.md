# Library Management System

A console-based (with optional Swing GUI) library application built with **Java, JDBC, and MySQL**.

## Features

- Staff accounts with login (passwords stored as SHA-256 hashes)
- Full book inventory management: add, search, update, delete, track total vs. available copies
- Member registration with auto-generated membership numbers
- Checkout books with business rules:
  - Blocks checkout if no copies are available
  - Blocks checkout if the member already has that book out
  - Caps members at 5 active loans at a time
  - Blocks checkout for suspended members
  - 14-day loan period by default
- Return books with automatic overdue fine calculation ($0.50/day late)
- View all active loans, overdue loans, and a member's full loan history
- Optional Swing GUI in addition to the console app

## Project Structure

```
LibraryManagementSystem/
├── database/
│   └── schema.sql               # MySQL schema (run this first)
├── lib/                         # Place mysql-connector-j-x.x.x.jar here
├── src/com/library/
│   ├── db/DBConnection.java     # JDBC connection manager
│   ├── model/                   # Book, Member, Loan, Staff POJOs
│   ├── dao/                     # BookDAO, MemberDAO, LoanDAO, StaffDAO
│   ├── service/                 # LibraryService (business logic), LibraryException
│   ├── util/PasswordUtil.java   # SHA-256 password hashing
│   └── ui/
│       ├── ConsoleApp.java      # Console entry point (main)
│       └── LibraryGUI.java      # Optional Swing GUI entry point (main)
└── README.md
```

## Prerequisites

1. **JDK 8+** installed
2. **MySQL Server** running locally (or reachable)
3. **MySQL Connector/J** (JDBC driver) — download the `mysql-connector-j-x.x.x.jar`
   from https://dev.mysql.com/downloads/connector/j/ and place it in the `lib/` folder.

## Setup

### 1. Create the database

```bash
mysql -u root -p < database/schema.sql
```

This creates a `library_system` database with `staff`, `books`, `members`, and `loans` tables.

### 2. Configure the connection

Edit `src/com/library/db/DBConnection.java` and set your MySQL credentials:

```java
private static final String URL = "jdbc:mysql://localhost:3306/library_system?useSSL=false&serverTimezone=UTC";
private static final String USER = "root";
private static final String PASSWORD = "your_mysql_password";
```

### 3. Compile

From the project root, with the MySQL connector jar in `lib/`:

```bash
javac -cp "lib/*" -d out $(find src -name "*.java")
```

### 4. Run

**Console app:**
```bash
java -cp "out:lib/*" com.library.ui.ConsoleApp
```
(On Windows, use `;` instead of `:` as the classpath separator: `-cp "out;lib/*"`)

**Swing GUI (optional):**
```bash
java -cp "out:lib/*" com.library.ui.LibraryGUI
```

## Usage Walkthrough

1. Register a staff account (username, password, name, role).
2. Log in.
3. Add books to the inventory (ISBN, title, author, category, copies).
4. Register members (an auto-generated membership number like `MEM12345` is issued).
5. Checkout a book using its ISBN and the member's membership number.
6. Return a book using the loan ID shown at checkout / in the active loans list.
7. Check the overdue list any time to see who owes fines.

## Notes on Design

- **Security**: passwords are hashed (SHA-256) before storage — plain text passwords are never saved.
- **Data integrity**: checkout/return use JDBC transactions with row locking (`SELECT ... FOR UPDATE`)
  on the book row, so concurrent checkouts can't oversell the last copy of a book.
- **Business rules live in the service layer**: `LibraryService` enforces loan limits, duplicate-checkout
  prevention, and fine calculation, so both the console app and the Swing GUI get identical behavior.
- **Fines**: calculated automatically at return time as `$0.50 × days overdue` (configurable via
  `LibraryService.FINE_PER_DAY` and `LOAN_PERIOD_DAYS`).
- **Extensibility**: add reservations/holds, email/SMS due-date reminders, barcode scanning input,
  or reporting (most-borrowed books, member activity) by extending `LibraryService` and adding DAO methods.

## Troubleshooting

- `Could not connect to the database`: verify MySQL is running, and the URL/user/password in
  `DBConnection.java` are correct.
- `ClassNotFoundException: com.mysql.cj.jdbc.Driver`: the MySQL connector jar isn't on your classpath —
  double check the `-cp` argument includes `lib/*`.
