package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton class to manage the MySQL database connection.
 */
public class Database {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/bank_app"; // Replace with your DB URL
    private static final String DB_USER = "root"; // Replace with your DB username
    private static final String DB_PASSWORD = ""; // Replace with your DB password

    private static Connection connection = null;

    // Private constructor to prevent instantiation
    private Database() {
    }

    /**
     * Returns the singleton instance of the database connection.
     * Establishes the connection if it doesn't exist.
     *
     * @return The active database connection.
     * @throws SQLException if a database access error occurs.
     */
    public static Connection getConnection() throws SQLException {
        // Use double-checked locking for thread safety (optional for simple desktop apps, crucial for concurrent env)
        if (connection == null || connection.isClosed()) {
            synchronized (Database.class) {
                if (connection == null || connection.isClosed()) {
                    try {
                        // Ensure the JDBC driver is loaded (often not needed with modern JDBC)
                        // Class.forName("com.mysql.cj.jdbc.Driver");
                        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                        System.out.println("Database connection established.");
                    } catch (SQLException e) {
                        System.err.println("Database connection failed: " + e.getMessage());
                        throw e; // Re-throw the exception to be handled by the caller (DAO)
                    }
                }
            }
        }
        return connection;
    }

    /**
     * Closes the database connection ifs it is open.
     */
    public static void closeConnection() {
        if (connection != null) {
            synchronized (Database.class) {
                if (connection != null) {
                    try {
                        if (!connection.isClosed()) {
                            connection.close();
                            connection = null; // Reset connection variable
                            System.out.println("Database connection closed.");
                        }
                    } catch (SQLException e) {
                        System.err.println("Failed to close database connection: " + e.getMessage());
                    }
                }
            }
        }
    }

    // Optional: Add a shutdown hook to ensure connection is closed when JVM exits
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(Database::closeConnection));
    }
}