-- Library Management System - Database Schema
-- Run this script in MySQL before starting the application.

DROP DATABASE IF EXISTS library_system;
CREATE DATABASE library_system;
USE library_system;

CREATE TABLE staff (
    staff_id      INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          ENUM('ADMIN','LIBRARIAN') DEFAULT 'LIBRARIAN',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE books (
    book_id           INT AUTO_INCREMENT PRIMARY KEY,
    isbn              VARCHAR(20) UNIQUE NOT NULL,
    title             VARCHAR(255) NOT NULL,
    author            VARCHAR(150) NOT NULL,
    category          VARCHAR(80),
    publisher         VARCHAR(150),
    publication_year  INT,
    total_copies      INT NOT NULL DEFAULT 1,
    available_copies  INT NOT NULL DEFAULT 1,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (available_copies >= 0 AND available_copies <= total_copies)
);

CREATE TABLE members (
    member_id       INT AUTO_INCREMENT PRIMARY KEY,
    membership_no   VARCHAR(20) UNIQUE NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(100),
    phone           VARCHAR(20),
    status          ENUM('ACTIVE','SUSPENDED') DEFAULT 'ACTIVE',
    joined_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE loans (
    loan_id       INT AUTO_INCREMENT PRIMARY KEY,
    book_id       INT NOT NULL,
    member_id     INT NOT NULL,
    checkout_date DATE NOT NULL,
    due_date      DATE NOT NULL,
    return_date   DATE NULL,
    status        ENUM('CHECKED_OUT','RETURNED') DEFAULT 'CHECKED_OUT',
    fine_amount   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE
);

CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
CREATE INDEX idx_loans_book ON loans(book_id);
CREATE INDEX idx_loans_member ON loans(member_id);
CREATE INDEX idx_loans_status ON loans(status);
