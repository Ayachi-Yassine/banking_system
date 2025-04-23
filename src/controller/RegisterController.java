package controller;

import dao.AccountDAO;
import dao.UserDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Account;
import model.User;
import util.PasswordUtil; // Assuming PasswordUtil exists
import util.Validator; // Assuming Validator exists

import java.io.IOException;
import java.math.BigDecimal;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Button registerButton;

    @FXML
    private Button backToLoginButton; // Button to go back

    @FXML
    private Label messageLabel;

    // Optional: Password generator button/field
    @FXML
    private TextField generatedPasswordField;
    @FXML
    private Button generatePasswordButton;


    private UserDAO userDAO;
    private AccountDAO accountDAO;

    public RegisterController() {
        userDAO = new UserDAO();
        accountDAO = new AccountDAO();
    }

    @FXML
    private void initialize() {
        // Optional: Add listener or action for generate password button
        if (generatePasswordButton != null && generatedPasswordField != null) {
            generatePasswordButton.setOnAction(event -> {
                String strongPassword = PasswordUtil.generateStrongPassword(12); // Example length
                generatedPasswordField.setText(strongPassword);
                // Optionally copy to password fields or require user to type it
                passwordField.setText(strongPassword);
                confirmPasswordField.setText(strongPassword);
            });
        }
    }


    @FXML
    private void handleRegisterButtonAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // --- Input Validation ---
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            setMessage("All fields are required.", true);
            return;
        }

        if (!Validator.isValidUsername(username)) {
            setMessage("Invalid username format (min 3 alphanumeric chars).", true);
            return;
        }

        if (!Validator.isValidPassword(password)) {
            setMessage("Password must be at least 8 characters long.", true);
            return;
        }


        if (!password.equals(confirmPassword)) {
            setMessage("Passwords do not match.", true);
            return;
        }

        // Check if username already exists
        if (userDAO.getUserByUsername(username) != null) {
            setMessage("Username already taken.", true);
            return;
        }

        // --- Registration Process ---
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password); // DAO will hash it
        newUser.setRole(User.Role.USER); // Default role

        User createdUser = userDAO.register(newUser);

        if (createdUser != null) {
            // User registered successfully, now create their initial account
            Account newAccount = new Account(createdUser.getId(), BigDecimal.ZERO); // Initial balance 0
            Account createdAccount = accountDAO.createAccount(newAccount);

            if (createdAccount != null) {
                setMessage("Registration successful! Account created.", false);
                // Optionally clear fields or navigate back to login after a delay
                // For now, just show success message
                clearFields();
            } else {
                setMessage("Registration successful, but failed to create account. Contact admin.", true);
                // Ideally, handle this more robustly (e.g., rollback user creation or retry account creation)
            }

        } else {
            setMessage("Registration failed. Please try again.", true);
        }
    }

    @FXML
    private void handleBackToLoginButtonAction(ActionEvent event) {
        try {
            // Load the login view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml")); // Ensure path is correct
            Parent loginRoot = loader.load();
            Scene loginScene = new Scene(loginRoot);

            // Get the current stage
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Apply CSS if available
            String css = getClass().getResource("/resources/style.css").toExternalForm();
            if (css != null) {
                loginScene.getStylesheets().add(css);
            }


            // Set the new scene
            stage.setScene(loginScene);
            stage.setTitle("Login");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            setMessage("Error loading login page.", true);
        }
    }


    private void setMessage(String message, boolean isError) {
        messageLabel.setText(message);
        if (isError) {
            messageLabel.setStyle("-fx-text-fill: red;");
        } else {
            messageLabel.setStyle("-fx-text-fill: green;");
        }
    }

    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        if (generatedPasswordField != null) {
            generatedPasswordField.clear();
        }
    }
}