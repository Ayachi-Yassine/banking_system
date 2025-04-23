package dao;

import database.Database;
import model.Account;
import model.Transaction; // Assuming Transaction model exists

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Transaction operations.
 */
public class TransactionDAO {

    /**
     * Saves a transaction record to the database.
     *
     * @param transaction The Transaction object to save.
     * @return true if the transaction was saved successfully, false otherwise.
     */
    public boolean saveTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (account_id, type, amount, transaction_date, related_account_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, transaction.getAccountId());
            pstmt.setString(2, transaction.getType().name()); // Store enum name
            pstmt.setBigDecimal(3, transaction.getAmount());
            pstmt.setTimestamp(4, Timestamp.valueOf(transaction.getTransactionDate() != null ? transaction.getTransactionDate() : LocalDateTime.now()));

            if (transaction.getRelatedAccountId() != null) {
                pstmt.setInt(5, transaction.getRelatedAccountId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        transaction.setId(generatedKeys.getInt(1));
                        return true; // Indicate success
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves the transaction history for a specific account, ordered by date descending.
     *
     * @param accountId The ID of the account.
     * @param limit     The maximum number of transactions to retrieve (0 for no limit).
     * @return A list of Transaction objects.
     */
    public List<Transaction> getHistoryByAccountId(int accountId, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        // Use StringBuilder for dynamic query building
        StringBuilder sqlBuilder = new StringBuilder("SELECT id, account_id, type, amount, transaction_date, related_account_id FROM transactions WHERE account_id = ? ORDER BY transaction_date DESC");
        if (limit > 0) {
            sqlBuilder.append(" LIMIT ?");
        }
        String sql = sqlBuilder.toString();

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accountId);
            if (limit > 0) {
                pstmt.setInt(2, limit);
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving transaction history for account ID " + accountId + ": " + e.getMessage());
        }
        return transactions;
    }

    /**
     * Retrieves the transaction history for a specific user by joining with the accounts table.
     * Ordered by date descending.
     *
     * @param userId The ID of the user.
     * @param limit  The maximum number of transactions to retrieve (0 for no limit).
     * @return A list of Transaction objects for the user's account(s).
     */
    public List<Transaction> getHistoryByUserId(int userId, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        // This query assumes a user might have multiple accounts in the future,
        // although the current setup implies one. It fetches transactions for all accounts of the user.
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT t.id, t.account_id, t.type, t.amount, t.transaction_date, t.related_account_id " +
                        "FROM transactions t JOIN accounts a ON t.account_id = a.id " +
                        "WHERE a.user_id = ? " +
                        "ORDER BY t.transaction_date DESC"
        );

        if (limit > 0) {
            sqlBuilder.append(" LIMIT ?");
        }
        String sql = sqlBuilder.toString();

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            if (limit > 0) {
                pstmt.setInt(2, limit);
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving transaction history for user ID " + userId + ": " + e.getMessage());
        }
        return transactions;
    }


    // Helper method to map ResultSet to Transaction object
    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getInt("id"));
        transaction.setAccountId(rs.getInt("account_id"));
        transaction.setType(Transaction.TransactionType.valueOf(rs.getString("type"))); // Convert string back to enum
        transaction.setAmount(rs.getBigDecimal("amount"));
        Timestamp ts = rs.getTimestamp("transaction_date");
        transaction.setTransactionDate(ts != null ? ts.toLocalDateTime() : null);
        transaction.setRelatedAccountId(rs.getObject("related_account_id", Integer.class)); // Handle potential NULL
        return transaction;
    }
}