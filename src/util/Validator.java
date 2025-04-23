package util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Utility class for input validation.
 */
public class Validator {

    // Basic email pattern (adjust for stricter validation if needed)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Validates an email address format.
     *
     * @param email The email string to validate.
     * @return true if the email format is valid, false otherwise.
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Checks if a BigDecimal amount is positive (greater than zero).
     *
     * @param amount The BigDecimal amount to check.
     * @return true if the amount is not null and strictly positive, false otherwise.
     */
    public static boolean isPositiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if a username is valid (e.g., length, allowed characters).
     * Example: Minimum 3 characters, alphanumeric.
     *
     * @param username The username string to validate.
     * @return true if the username is valid, false otherwise.
     */
    public static boolean isValidUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9]{3,}$");
    }

    /**
     * Checks if a password meets basic complexity requirements (e.g., length).
     *
     * @param password The password string to validate.
     * @return true if the password meets criteria, false otherwise.
     */
    public static boolean isValidPassword(String password) {
        // Example: Minimum 8 characters
        return password != null && password.length() >= 8;
    }
}