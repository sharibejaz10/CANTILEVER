-- Simple Banking System - Database Schema
-- Run this script in MySQL before starting the application.

DROP DATABASE IF EXISTS banking_system;
CREATE DATABASE banking_system;
USE banking_system;

CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100),
    phone         VARCHAR(20),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    account_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL,
    account_number  VARCHAR(20) UNIQUE NOT NULL,
    account_type    ENUM('SAVINGS','CHECKING') DEFAULT 'SAVINGS',
    balance         DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status          ENUM('ACTIVE','CLOSED') DEFAULT 'ACTIVE',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE transactions (
    transaction_id    INT AUTO_INCREMENT PRIMARY KEY,
    account_id        INT NOT NULL,
    transaction_type  ENUM('DEPOSIT','WITHDRAW','TRANSFER_IN','TRANSFER_OUT') NOT NULL,
    amount            DECIMAL(15,2) NOT NULL,
    balance_after     DECIMAL(15,2) NOT NULL,
    description       VARCHAR(255),
    transaction_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE
);

CREATE INDEX idx_accounts_user ON accounts(user_id);
CREATE INDEX idx_transactions_account ON transactions(account_id);
