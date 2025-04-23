package dao;

import database.Database;
import model.User;
import util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User operations (CRUD).
 */
public class UserDAO {

    private static final int MAX_LOGIN_ATTEMPTS = 3;

    /**
     * Registers a new user in the database.
     * Hashes the password before storing.
     *
     * @param user The User object containing registration data (username, plain password, role).
     * @return The created User object with its generated ID, or null if registration fails (e.g., username exists).
     */
    public User register(User user) {
        // Hash the password before storing
        String hashedPassword = PasswordUtil.hashPassword(user.getPassword());
        user.setPassword(hashedPassword); // Store hashed password in the object for insertion
        user.setLocked(false); // Ensure user is not locked initially
        user.setFailedAttempts(0); // Ensure failed attempts are reset

        String sql = "INSERT INTO users (username, password, role, locked, failed_attempts) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole().name()); // Store enum name as string
            pstmt.setBoolean(4, user.isLocked());
            pstmt.setInt(5, user.getFailedAttempts());


            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                        return user; // Return user with ID
                    }
                }
            }
        } catch (SQLException e) {
            // Log error (e.g., unique constraint violation for username)
            System.err.println("Error registering user: " + e.getMessage());
            // Check for unique constraint violation (MySQL error code 1062)
            if (e.getErrorCode() == 1062) {
                System.err.println("Username '" + user.getUsername() + "' already exists.");
            }
        }
        return null; // Return null if registration failed
    }

    /**
     * Attempts to log in a user. Verifies username and password.
     * Handles account locking based on failed attempts.
     *
     * @param username    The username provided by the user.
     * @param plainPassword The plain text password provided by the user.
     * @return The User object if login is successful, null otherwise (invalid credentials, locked account, or user not found).
     */
    public User login(String username, String plainPassword) {
        User user = getUserByUsername(username);

        if (user == null) {
            return null; // User not found
        }

        if (user.isLocked()) {
            System.out.println("Account for user '" + username + "' is locked.");
            return null; // Account locked
        }

        if (PasswordUtil.verifyPassword(plainPassword, user.getPassword())) {
            // Login successful, reset failed attempts
            resetFailedAttempts(username);
            return user; // Return the authenticated user object
        } else {
            // Login failed, increment failed attempts
            incrementFailedAttempts(username);
            // Check if account should be locked now
            if (getFailedAttempts(username) >= MAX_LOGIN_ATTEMPTS) {
                lockAccount(username);
                System.out.println("Account for user '" + username + "' locked due to too many failed login attempts.");
            }
            return null; // Invalid password
        }
    }

    /**
     * Retrieves a user by their username.
     *
     * @param username The username to search for.
     * @return The User object if found, null otherwise.
     */
    public User getUserByUsername(String username) {
        String sql = "SELECT id, username, password, role, locked, failed_attempts FROM users WHERE username = ?";
        User user = null;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password")); // Store the hash
                user.setRole(User.Role.valueOf(rs.getString("role"))); // Convert string back to enum
                user.setLocked(rs.getBoolean("locked"));
                user.setFailedAttempts(rs.getInt("failed_attempts"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving user by username: " + e.getMessage());
        }
        return user;
    }

    /**
     * Retrieves all users (for admin purposes).
     *
     * @return A list of all User objects.
     */
    public List<User> getAllUsers() {
        String sql = "SELECT id, username, role, locked, failed_attempts FROM users ORDER BY username";
        List<User> users = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                // Note: Password hash is not retrieved for security in list views
                user.setRole(User.Role.valueOf(rs.getString("role")));
                user.setLocked(rs.getBoolean("locked"));
                user.setFailedAttempts(rs.getInt("failed_attempts"));
                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving all users: " + e.getMessage());
        }
        return users;
    }


    /**
     * Updates the lock status of a user account.
     *
     * @param username The username of the account to update.
     * @param locked   true to lock the account, false to unlock.
     * @return true if the update was successful, false otherwise.
     */
    public boolean updateUserLockStatus(String username, boolean locked) {
        String sql = "UPDATE users SET locked = ?, failed_attempts = CASE WHEN ? = false THEN 0 ELSE failed_attempts END WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, locked);
            pstmt.setBoolean(2, locked); // For the CASE statement condition
            pstmt.setString(3, username);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating lock status for user " + username + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a user from the database.
     * Note: Dependent records (accounts, transactions) might be handled by DB constraints (ON DELETE CASCADE).
     *
     * @param userId The ID of the user to delete.
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user with ID " + userId + ": " + e.getMessage());
            return false;
        }
    }

    // --- Helper methods for login attempts ---

    private void incrementFailedAttempts(String username) {
        String sql = "UPDATE users SET failed_attempts = failed_attempts + 1 WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error incrementing failed attempts for user " + username + ": " + e.getMessage());
        }
    }

    private void resetFailedAttempts(String username) {
        String sql = "UPDATE users SET failed_attempts = 0 WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error resetting failed attempts for user " + username + ": " + e.getMessage());
        }
    }

    private int getFailedAttempts(String username) {
        String sql = "SELECT failed_attempts FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("failed_attempts");
            }
        } catch (SQLException e) {
            System.err.println("Error getting failed attempts for user " + username + ": " + e.getMessage());
        }
        return 0; // Return 0 if user not found or error occurs
    }

    private void lockAccount(String username) {
        updateUserLockStatus(username, true);
    }

}