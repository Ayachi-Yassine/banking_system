package dao;

import database.Database;
import model.Account;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime; // Use java.time

/**
 * Data Access Object for Account operations.
 */
public class AccountDAO {

    /**
     * Creates a new bank account for a user.
     *
     * @param account The Account object containing user ID and initial balance.
     * @return The created Account object with its generated ID, or null if creation fails.
     */
    public Account createAccount(Account account) {
        String sql = "INSERT INTO accounts (user_id, balance) VALUES (?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, account.getUserId());
            pstmt.setBigDecimal(2, account.getBalance());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        account.setId(generatedKeys.getInt(1));
                        // Retrieve created_at timestamp if needed (though not strictly necessary for the returned object)
                        // account.setCreatedAt(getAccountById(account.getId()).getCreatedAt()); // Example
                        return account;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating account: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves an account by its ID.
     *
     * @param accountId The ID of the account.
     * @return The Account object if found, null otherwise.
     */
    public Account getAccountById(int accountId) {
        String sql = "SELECT id, user_id, balance, created_at FROM accounts WHERE id = ?";
        Account account = null;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accountId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                account = mapResultSetToAccount(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving account by ID " + accountId + ": " + e.getMessage());
        }
        return account;
    }


    /**
     * Retrieves the account associated with a specific user ID.
     * Assumes one primary account per user for simplicity based on specs.
     *
     * @param userId The ID of the user.
     * @return The Account object if found, null otherwise.
     */
    public Account getAccountByUserId(int userId) {
        String sql = "SELECT id, user_id, balance, created_at FROM accounts WHERE user_id = ?";
        Account account = null;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            // Assuming one account per user as per simple model
            if (rs.next()) {
                account = mapResultSetToAccount(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving account for user ID " + userId + ": " + e.getMessage());
        }
        return account;
    }

    /**
     * Updates the balance of a specific account. USE WITH CAUTION - prefer deposit/withdraw/transfer.
     * This method bypasses transaction logging.
     *
     * @param accountId The ID of the account to update.
     * @param newBalance The new balance for the account.
     * @return true if the update was successful, false otherwise.
     */
    private boolean updateBalance(Connection conn, int accountId, BigDecimal newBalance) throws SQLException {
        // This method assumes it's called within a transaction (needs connection passed)
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, newBalance);
            pstmt.setInt(2, accountId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
        // Exception handling is done by the calling method (deposit, withdraw, transfer)
    }


    /**
     * Deposits an amount into an account. Uses a transaction.
     *
     * @param accountId The ID of the account.
     * @param amount    The positive amount to deposit.
     * @return true if the deposit was successful, false otherwise.
     */
    public boolean deposit(int accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.err.println("Deposit amount must be positive.");
            return false;
        }

        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Lock the row for update (optional but safer in concurrent environments)
            Account account = getAccountByIdForUpdate(conn, accountId);
            if (account == null) {
                conn.rollback();
                return false; // Account not found
            }

            BigDecimal newBalance = account.getBalance().add(amount);
            if (updateBalance(conn, accountId, newBalance)) {
                // Log the transaction (should be done by the service/controller layer ideally)
                // Example: transactionDAO.saveTransaction(new Transaction(accountId, DEPOSIT, amount, null));
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error during deposit transaction: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println("Rollback failed: " + ex.getMessage());}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ex) { /* ignore */ }
                // Connection should not be closed here if obtained from Singleton pool managed elsewhere
            }
        }
    }

    /**
     * Withdraws an amount from an account. Uses a transaction.
     *
     * @param accountId The ID of the account.
     * @param amount    The positive amount to withdraw.
     * @return true if the withdrawal was successful (sufficient funds), false otherwise.
     */
    public boolean withdraw(int accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.err.println("Withdrawal amount must be positive.");
            return false;
        }

        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Lock the row for update
            Account account = getAccountByIdForUpdate(conn, accountId);
            if (account == null) {
                conn.rollback();
                return false; // Account not found
            }


            if (account.getBalance().compareTo(amount) < 0) {
                System.err.println("Insufficient funds for withdrawal.");
                conn.rollback(); // No need to proceed
                return false;
            }

            BigDecimal newBalance = account.getBalance().subtract(amount);
            if (updateBalance(conn, accountId, newBalance)) {
                // Log the transaction (should be done by the service/controller layer ideally)
                // Example: transactionDAO.saveTransaction(new Transaction(accountId, WITHDRAW, amount, null));
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error during withdrawal transaction: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println("Rollback failed: " + ex.getMessage()); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ex) { /* ignore */ }
            }
        }
    }


    /**
     * Transfers an amount from one account to another. Uses a transaction.
     *
     * @param fromAccountId The ID of the source account.
     * @param toAccountId   The ID of the destination account.
     * @param amount        The positive amount to transfer.
     * @return true if the transfer was successful, false otherwise.
     */
    public boolean transfer(int fromAccountId, int toAccountId, BigDecimal amount) {
        if (fromAccountId == toAccountId) {
            System.err.println("Cannot transfer to the same account.");
            return false;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.err.println("Transfer amount must be positive.");
            return false;
        }

        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Lock rows for update (order by ID to prevent deadlocks)
            int firstLockId = Math.min(fromAccountId, toAccountId);
            int secondLockId = Math.max(fromAccountId, toAccountId);

            Account firstAccount = getAccountByIdForUpdate(conn, firstLockId);
            Account secondAccount = getAccountByIdForUpdate(conn, secondLockId);

            Account fromAccount = (fromAccountId == firstLockId) ? firstAccount : secondAccount;
            Account toAccount = (toAccountId == firstLockId) ? firstAccount : secondAccount;


            if (fromAccount == null || toAccount == null) {
                System.err.println("One or both accounts not found.");
                conn.rollback();
                return false;
            }

            // Check sufficient funds in the source account
            if (fromAccount.getBalance().compareTo(amount) < 0) {
                System.err.println("Insufficient funds for transfer from account " + fromAccountId);
                conn.rollback();
                return false;
            }

            // Perform the balance updates
            BigDecimal fromNewBalance = fromAccount.getBalance().subtract(amount);
            BigDecimal toNewBalance = toAccount.getBalance().add(amount);

            boolean withdrawSuccess = updateBalance(conn, fromAccountId, fromNewBalance);
            boolean depositSuccess = updateBalance(conn, toAccountId, toNewBalance);

            if (withdrawSuccess && depositSuccess) {
                // Log transactions (should be done by service/controller ideally)
                // Example: transactionDAO.saveTransaction(new Transaction(fromAccountId, TRANSFER_OUT, amount, toAccountId));
                // Example: transactionDAO.saveTransaction(new Transaction(toAccountId, TRANSFER_IN, amount, fromAccountId));
                conn.commit();
                return true;
            } else {
                System.err.println("Failed to update balances during transfer.");
                conn.rollback();
                return false;
            }

        } catch (SQLException e) {
            System.err.println("Error during transfer transaction: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println("Rollback failed: " + ex.getMessage()); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ex) { /* ignore */ }
            }
        }
    }

    /**
     * Retrieves an account by ID for update, locking the row.
     * Must be called within an active transaction with auto-commit set to false.
     *
     * @param conn      The active database connection (within a transaction).
     * @param accountId The ID of the account to retrieve and lock.
     * @return The Account object if found, null otherwise.
     * @throws SQLException if a database error occurs.
     */
    private Account getAccountByIdForUpdate(Connection conn, int accountId) throws SQLException {
        // "FOR UPDATE" locks the selected row(s) until the transaction is committed or rolled back
        String sql = "SELECT id, user_id, balance, created_at FROM accounts WHERE id = ? FOR UPDATE";
        Account account = null;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    account = mapResultSetToAccount(rs);
                }
            }
        } // Let SQLException propagate to the calling transactional method
        return account;
    }

    // Helper method to map ResultSet to Account object
    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getInt("id"));
        account.setUserId(rs.getInt("user_id"));
        account.setBalance(rs.getBigDecimal("balance"));
        Timestamp ts = rs.getTimestamp("created_at");
        account.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return account;
    }
}