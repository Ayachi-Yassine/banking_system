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
import javafx.stage.Stage;
import model.User;

import java.io.IOException;
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
    private TableColumn<User, Integer> attemptsColumn; // Added failed attempts column

    @FXML
    private Button lockUnlockButton;

    @FXML
    private Button deleteUserButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button logoutButton;


    private User currentAdminUser;
    private UserDAO userDAO;
    private AccountDAO accountDAO; // Needed if admin needs to view/manage accounts directly


    private final ObservableList<User> userData = FXCollections.observableArrayList();

    public AdminController() {
        userDAO = new UserDAO();
        accountDAO = new AccountDAO();
    }

    /**
     * Initializes the controller class.
     */
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
                // Prevent admin from locking/deleting themselves (optional safety)
                if (currentAdminUser != null && newSelection.getId() == currentAdminUser.getId()) {
                    lockUnlockButton.setDisable(true);
                    deleteUserButton.setDisable(true);
                }

            } else {
                lockUnlockButton.setText("Lock/Unlock");
            }
        });
    }


    /**
     * Receives the logged-in admin user data.
     *
     * @param user The logged-in Admin User object.
     */
    public void initData(User user) {
        if (user == null || user.getRole() != User.Role.ADMIN) {
            // This should not happen if navigation logic is correct
            System.err.println("Error: AdminController initialized with non-admin or null user.");
            // Optionally force logout or show error
            handleLogoutButtonAction(null); // Need to pass an event or handle differently
            return;
        }
        this.currentAdminUser = user;
        welcomeLabel.setText("Admin Dashboard - Welcome, " + currentAdminUser.getUsername() + "!");
        loadUsersData();
    }

    private void loadUsersData() {
        List<User> users = userDAO.getAllUsers();
        userData.setAll(users);
        usersTable.getSelectionModel().clearSelection(); // Clear selection after refresh
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

        // Prevent admin from locking/unlocking themselves
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
                loadUsersData(); // Refresh the table
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

        // Prevent admin from deleting themselves
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
                loadUsersData(); // Refresh the table
            } else {
                showErrorAlert("Error", String.format("Failed to delete user '%s'. Check logs.", selectedUser.getUsername()));
            }
        }
    }

    @FXML
    private void handleLogoutButtonAction(ActionEvent event) {
        try {
            // Load the login view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml")); // Ensure path is correct
            Parent loginRoot = loader.load();
            Scene loginScene = new Scene(loginRoot);

            // Get the current stage (handle null event if called programmatically)
            Stage stage = null;
            if (event != null && event.getSource() instanceof Node) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else if (welcomeLabel != null && welcomeLabel.getScene() != null) {
                // Fallback if event is null (e.g. called from initData on error)
                stage = (Stage) welcomeLabel.getScene().getWindow();
            }

            if (stage != null) {
                // Apply CSS if available
                String css = getClass().getResource("/resources/style.css").toExternalForm();
                if (css != null) {
                    loginScene.getStylesheets().add(css);
                }
                // Set the new scene
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

    // --- Helper methods for Alerts ---
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