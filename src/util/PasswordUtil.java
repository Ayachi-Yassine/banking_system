package util;

// IMPORTANT: Add a BCrypt library dependency (e.g., org.mindrot:jbcrypt:0.4)
import org.mindrot.jbcrypt.BCrypt; // Assuming use of jbcrypt

import java.security.SecureRandom;

/**
 * Utility class for password hashing and verification using BCrypt.
 */
public class PasswordUtil {

    // Configure the workload factor (higher is slower but more secure)
    private static final int BCRYPT_WORKLOAD = 12; // Standard value

    /**
     * Hashes a plain text password using BCrypt.
     *
     * @param plainPassword The password to hash.
     * @return The BCrypt hashed password string (includes salt).
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        // BCrypt.gensalt() generates a salt automatically
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_WORKLOAD));
    }

    /**
     * Verifies a plain text password against a stored BCrypt hash.
     *
     * @param plainPassword  The password attempt from the user.
     * @param hashedPassword The stored hash from the database.
     * @return true if the password matches the hash, false otherwise.
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || plainPassword.isEmpty() || hashedPassword.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Handle cases where the hash is not in the expected format
            System.err.println("Error verifying password hash format: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generates a strong random password (example implementation).
     * Not strictly required by the Cahier des Charges for PasswordUtil itself,
     * but mentioned for the register form.
     *
     * @param length The desired length of the password.
     * @return A randomly generated password string.
     */
    public static String generateStrongPassword(int length) {
        if (length < 8) throw new IllegalArgumentException("Password length should be at least 8");

        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+=-";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}