package controller;

import dao.AccountDAO;
import dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox; // Added import
import javafx.stage.Stage;
import model.Account;
import model.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class AdminController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, Integer> userIdColumn;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, User.Role> roleColumn;

    @FXML
    private TableColumn<User, Boolean> lockedColumn;

    @FXML
    private TableColumn<User, Integer> attemptsColumn;

    @FXML
    private Button lockUnlockButton;

    @FXML
    private Button deleteUserButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Button showCreateFormButton;

    @FXML
    private VBox createUserForm;

    @FXML
    private TextField newUsernameField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private ChoiceBox<String> newRoleChoiceBox;

    @FXML
    private Label messageLabel;

    private User currentAdminUser;
    private UserDAO userDAO;
    private AccountDAO accountDAO;

    private final ObservableList<User> userData = FXCollections.observableArrayList();

    public AdminController() {
        userDAO = new UserDAO();
        accountDAO = new AccountDAO();
    }

    @FXML
    private void initialize() {
        // Initialize the table columns
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        lockedColumn.setCellValueFactory(new PropertyValueFactory<>("locked"));
        attemptsColumn.setCellValueFactory(new PropertyValueFactory<>("failedAttempts"));

        // Custom cell factory for boolean 'locked' column to show text like "Yes"/"No" or icons
        lockedColumn.setCellFactory(column -> new TableCell<User, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item ? "Yes" : "No");
                    // Optional: style locked accounts differently
                    if (item) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        usersTable.setItems(userData);

        // Disable buttons initially until a user is selected
        lockUnlockButton.setDisable(true);
        deleteUserButton.setDisable(true);

        // Add listener to enable/disable buttons based on selection
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean userSelected = newSelection != null;
            lockUnlockButton.setDisable(!userSelected);
            deleteUserButton.setDisable(!userSelected);

            // Update Lock/Unlock button text based on selected user's status
            if (userSelected) {
                lockUnlockButton.setText(newSelection.isLocked() ? "Unlock User" : "Lock User");
                // Prevent admin from locking/deleting themselves
                if (currentAdminUser != null && newSelection.getId() == currentAdminUser.getId()) {
                    lockUnlockButton.setDisable(true);
                    deleteUserButton.setDisable(true);
                }
            } else {
                lockUnlockButton.setText("Lock/Unlock");
            }
        });

        // Initialize the ChoiceBox for roles
        newRoleChoiceBox.setItems(FXCollections.observableArrayList("USER", "ADMIN"));
        newRoleChoiceBox.setValue("USER");
    }

    public void initData(User user) {
        if (user == null || user.getRole() != User.Role.ADMIN) {
            System.err.println("Error: AdminController initialized with non-admin or null user.");
            handleLogoutButtonAction(null);
            return;
        }
        this.currentAdminUser = user;
        welcomeLabel.setText("Admin Dashboard - Welcome, " + currentAdminUser.getUsername() + "!");
        loadUsersData();
    }

    private void loadUsersData() {
        List<User> users = userDAO.getAllUsers();
        userData.setAll(users);
        usersTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRefreshButtonAction(ActionEvent event) {
        loadUsersData();
        showInfoAlert("Refreshed", "User list updated.");
    }

    @FXML
    private void handleLockUnlockButtonAction(ActionEvent event) {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showErrorAlert("No Selection", "Please select a user from the table.");
            return;
        }

        if (currentAdminUser != null && selectedUser.getId() == currentAdminUser.getId()) {
            showErrorAlert("Action Denied", "Administrators cannot lock/unlock their own account.");
            return;
        }

        boolean currentLockStatus = selectedUser.isLocked();
        String action = currentLockStatus ? "unlock" : "lock";

        Optional<ButtonType> result = showConfirmationAlert(
                "Confirm Action",
                String.format("Are you sure you want to %s the user '%s'?", action, selectedUser.getUsername())
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = userDAO.updateUserLockStatus(selectedUser.getUsername(), !currentLockStatus);
            if (success) {
                showInfoAlert("Success", String.format("User '%s' has been %sed.", selectedUser.getUsername(), action));
                loadUsersData();
            } else {
                showErrorAlert("Error", String.format("Failed to %s user '%s'.", action, selectedUser.getUsername()));
            }
        }
    }

    @FXML
    private void handleDeleteUserButtonAction(ActionEvent event) {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showErrorAlert("No Selection", "Please select a user from the table.");
            return;
        }

        if (currentAdminUser != null && selectedUser.getId() == currentAdminUser.getId()) {
            showErrorAlert("Action Denied", "Administrators cannot delete their own account.");
            return;
        }

        Optional<ButtonType> result = showConfirmationAlert(
                "Confirm Deletion",
                String.format("WARNING: Are you sure you want to permanently delete the user '%s'?\nThis will also delete associated accounts and transactions (if cascade is enabled).", selectedUser.getUsername())
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = userDAO.deleteUser(selectedUser.getId());
            if (success) {
                showInfoAlert("Success", String.format("User '%s' has been deleted.", selectedUser.getUsername()));
                loadUsersData();
            } else {
                showErrorAlert("Error", String.format("Failed to delete user '%s'. Check logs.", selectedUser.getUsername()));
            }
        }
    }

    @FXML
    private void handleLogoutButtonAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent loginRoot = loader.load();
            Scene loginScene = new Scene(loginRoot);

            Stage stage = null;
            if (event != null && event.getSource() instanceof Node) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else if (welcomeLabel != null && welcomeLabel.getScene() != null) {
                stage = (Stage) welcomeLabel.getScene().getWindow();
            }

            if (stage != null) {
                String css = getClass().getResource("/resources/style.css").toExternalForm();
                if (css != null) {
                    loginScene.getStylesheets().add(css);
                }
                stage.setScene(loginScene);
                stage.setTitle("Login");
                stage.show();
            } else {
                System.err.println("Logout failed: Could not determine the current stage.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Logout Error", "Failed to return to the login screen.");
        } catch (NullPointerException e) {
            System.err.println("Logout failed: Could not get scene or window reference.");
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleCreateForm() {
        createUserForm.setVisible(!createUserForm.isVisible());
        showCreateFormButton.setText(createUserForm.isVisible() ? "Hide Create User Form" : "Show Create User Form");
    }

    @FXML
    private void handleCreateUser() {
        String username = newUsernameField.getText();
        String password = newPasswordField.getText();
        String role = newRoleChoiceBox.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            messageLabel.setText("All fields are required.");
            return;
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password); // Hashed by UserDAO
        newUser.setRole(User.Role.valueOf(role));

        User createdUser = userDAO.register(newUser);
        if (createdUser != null) {
            accountDAO.createAccount(new Account(createdUser.getId(), BigDecimal.ZERO));
            messageLabel.setText("User created successfully.");
            loadUsersData();
        } else {
            messageLabel.setText("Failed to create user.");
        }
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private Optional<ButtonType> showConfirmationAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait();
    }
}