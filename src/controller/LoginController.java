package controller;

import dao.AccountDAO;
import dao.UserDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.User;
import javafx.scene.Node; // Import Node for getting the stage

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton; // Added button to navigate to registration

    @FXML
    private Label messageLabel;

    private UserDAO userDAO;
    // private AccountDAO accountDAO; // Might not be needed directly here

    public LoginController() {
        userDAO = new UserDAO();
        // accountDAO = new AccountDAO();
    }

    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Username and password cannot be empty.");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        User user = userDAO.login(username, password);

        if (user != null) {
            // Login successful
            messageLabel.setText("Login Successful!");
            messageLabel.setStyle("-fx-text-fill: green;");
            System.out.println("Login successful for user: " + user.getUsername() + ", Role: " + user.getRole());

            // Navigate to the appropriate dashboard
            navigateToDashboard(user, event);

        } else {
            // Login failed - Check if user exists but is locked or bad credentials
            User existingUser = userDAO.getUserByUsername(username);
            if (existingUser != null && existingUser.isLocked()) {
                messageLabel.setText("Account is locked. Please contact admin.");
            } else {
                messageLabel.setText("Invalid username or password.");
            }
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleRegisterButtonAction(ActionEvent event) {
        try {
            // Load the register view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/register.fxml")); // Ensure path is correct
            Parent registerRoot = loader.load();
            Scene registerScene = new Scene(registerRoot);

            // Get the current stage
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Apply CSS if available
            String css = getClass().getResource("/resources/style.css").toExternalForm();
            if (css != null) {
                registerScene.getStylesheets().add(css);
            }


            // Set the new scene
            stage.setScene(registerScene);
            stage.setTitle("Register");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Error loading registration page.");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }


    private void navigateToDashboard(User user, ActionEvent event) {
        try {
            String fxmlFile;
            FXMLLoader loader;

            // Choose FXML based on user role
            if (user.getRole() == User.Role.ADMIN) {
                fxmlFile = "/view/admin.fxml"; // Path to admin dashboard FXML
                loader = new FXMLLoader(getClass().getResource(fxmlFile));
            } else {
                fxmlFile = "/view/dashboard.fxml"; // Path to user dashboard FXML
                loader = new FXMLLoader(getClass().getResource(fxmlFile));
            }


            Parent dashboardRoot = loader.load();

            // Pass user data to the next controller
            if (user.getRole() == User.Role.ADMIN) {
                AdminController adminController = loader.getController();
                adminController.initData(user); // Method to pass user data
            } else {
                DashboardController dashboardController = loader.getController();
                dashboardController.initData(user); // Method to pass user data
            }


            Scene dashboardScene = new Scene(dashboardRoot);

            // Get the current stage
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();


            // Apply CSS if available
            String css = getClass().getResource("/resources/style.css").toExternalForm();
            if (css != null) {
                dashboardScene.getStylesheets().add(css);
            }

            // Set the new scene
            stage.setScene(dashboardScene);
            stage.setTitle(user.getRole() == User.Role.ADMIN ? "Admin Dashboard" : "User Dashboard");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Error loading dashboard.");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }
}