CREATE DATABASE IF NOT EXISTS bank_app;

USE bank_app;

-- Table for users
CREATE TABLE IF NOT EXISTS users (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- Increased length for hashed passwords (bcrypt)
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Table for accounts
CREATE TABLE IF NOT EXISTS accounts (
                                        id INT AUTO_INCREMENT PRIMARY KEY,
                                        user_id INT NOT NULL,
                                        balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00, -- Using DECIMAL for currency
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE -- Cascade delete if user is removed
    );

-- Table for transactions
CREATE TABLE IF NOT EXISTS transactions (
                                            id INT AUTO_INCREMENT PRIMARY KEY,
                                            account_id INT NOT NULL,
                                            type ENUM('DEPOSIT', 'WITHDRAW', 'TRANSFER_OUT', 'TRANSFER_IN') NOT NULL, -- Added specific transfer types
    amount DECIMAL(15, 2) NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    related_account_id INT NULL, -- Optional: To link transfer transactions
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE, -- Cascade delete if account is removed
    FOREIGN KEY (related_account_id) REFERENCES accounts(id) ON DELETE SET NULL -- Keep transaction history even if related account deleted
    );

-- Add indexes for performance
CREATE INDEX idx_user_id ON accounts(user_id);
CREATE INDEX idx_account_id ON transactions(account_id);
CREATE INDEX idx_transaction_date ON transactions(transaction_date);
CREATE INDEX idx_username ON users(username);

-- Note: The 'balance' column mentioned in model/User.java is not directly in the users table here.
-- It should be fetched by joining with the accounts table or calculated as needed in the application logic.
-- The DAO methods will handle retrieving the balance from the 'accounts' table.