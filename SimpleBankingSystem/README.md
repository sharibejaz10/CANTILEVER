# Simple Banking System

A console-based (with optional Swing GUI) banking application built with **Java, JDBC, and MySQL**.

## Features

- User registration & login (passwords stored as SHA-256 hashes)
- Open multiple accounts per user (Savings / Checking)
- Deposit and withdraw funds
- Transfer funds between accounts (atomic, transactional — uses `COMMIT`/`ROLLBACK`)
- Transaction history per account
- Input validation and clear error handling (insufficient funds, invalid accounts, etc.)
- Optional Swing GUI in addition to the console app

## Project Structure

```
SimpleBankingSystem/
├── database/
│   └── schema.sql              # MySQL schema (run this first)
├── lib/                        # Place mysql-connector-j-x.x.x.jar here
├── src/com/bank/
│   ├── db/DBConnection.java    # JDBC connection manager
│   ├── model/                  # User, Account, Transaction POJOs
│   ├── dao/                    # UserDAO, AccountDAO, TransactionDAO
│   ├── service/                # BankingService (business logic), BankingException
│   ├── util/PasswordUtil.java  # SHA-256 password hashing
│   └── ui/
│       ├── ConsoleApp.java     # Console entry point (main)
│       └── BankingGUI.java     # Optional Swing GUI entry point (main)
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

This creates a `banking_system` database with `users`, `accounts`, and `transactions` tables.

### 2. Configure the connection

Edit `src/com/bank/db/DBConnection.java` and set your MySQL credentials:

```java
private static final String URL = "jdbc:mysql://localhost:3306/banking_system?useSSL=false&serverTimezone=UTC";
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
java -cp "out:lib/*" com.bank.ui.ConsoleApp
```
(On Windows, use `;` instead of `:` as the classpath separator: `-cp "out;lib/*"`)

**Swing GUI (optional):**
```bash
java -cp "out:lib/*" com.bank.ui.BankingGUI
```

## Usage Walkthrough

1. Register a new user (username, password, name, email, phone).
2. Log in.
3. Open one or more accounts (Savings/Checking) with an opening balance.
4. Use Deposit / Withdraw / Transfer with the generated 10-digit account number.
5. View transaction history for any account you own.

## Notes on Design

- **Security**: passwords are hashed (SHA-256) before storage — plain text passwords are never saved.
- **Data integrity**: transfers use JDBC transactions (`setAutoCommit(false)` + `commit()`/`rollback()`)
  so a transfer either fully succeeds (both balances updated, both ledger rows written) or fully fails.
- **Layered architecture**: UI → Service (business rules) → DAO (SQL) → DB, so the console app and the
  Swing GUI both reuse the same `BankingService` without duplicating logic.
- **Extensibility**: add interest calculation, account statements (PDF/CSV export), admin roles, or
  scheduled/recurring transfers by extending `BankingService` and adding new DAO methods.

## Troubleshooting

- `Could not connect to the database`: verify MySQL is running, and the URL/user/password in
  `DBConnection.java` are correct.
- `ClassNotFoundException: com.mysql.cj.jdbc.Driver`: the MySQL connector jar isn't on your classpath —
  double check the `-cp` argument includes `lib/*`.
