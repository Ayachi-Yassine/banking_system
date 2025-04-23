package controller;

import dao.AccountDAO;
import dao.TransactionDAO;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Account;
import model.Transaction;
import model.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private TableView<Transaction> transactionsTable;

    @FXML
    private TableColumn<Transaction, LocalDateTime> dateColumn;

    @FXML
    private TableColumn<Transaction, Transaction.TransactionType> typeColumn;

    @FXML
    private TableColumn<Transaction, BigDecimal> amountColumn;

    @FXML
    private TableColumn<Transaction, Integer> relatedAccountColumn; // Optional for transfers


    @FXML
    private Button depositButton;

    @FXML
    private Button withdrawButton;

    @FXML
    private Button transferButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button logoutButton;


    private User currentUser;
    private Account currentAccount;
    private AccountDAO accountDAO;
    private TransactionDAO transactionDAO;

    private final ObservableList<Transaction> transactionData = FXCollections.observableArrayList();

    public DashboardController() {
        accountDAO = new AccountDAO();
        transactionDAO = new TransactionDAO();
    }

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
        // Initialize the table columns
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        // Format amount column if needed (e.g., currency symbol)
        amountColumn.setCellFactory(tc -> new TableCell<Transaction, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Format as currency (adjust locale/currency code as needed)
                    setText(String.format("%,.2f DT", item)); // Example: Tunisian Dinar
                }
            }
        });

        if (relatedAccountColumn != null) {
            relatedAccountColumn.setCellValueFactory(new PropertyValueFactory<>("relatedAccountId"));
            relatedAccountColumn.setCellFactory(tc -> new TableCell<Transaction, Integer>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item == 0) { // Handle null or 0
                        setText(null);
                    } else {
                        setText(item.toString());
                    }
                }
            });
        }


        transactionsTable.setItems(transactionData);
    }

    /**
     * Receives the logged-in user data from the LoginController.
     *
     * @param user The logged-in User object.
     */
    public void initData(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Welcome, " + currentUser.getUsername() + "!");
        loadAccountData();
        loadTransactionHistory();
    }

    private void loadAccountData() {
        if (currentUser != null) {
            this.currentAccount = accountDAO.getAccountByUserId(currentUser.getId());
            if (this.currentAccount != null) {
                updateBalanceDisplay();
            } else {
                // Handle case where account doesn't exist (should not happen after registration)
                balanceLabel.setText("Balance: Error loading account");
                showErrorAlert("Account Error", "Could not load account details for user.");
            }
        }
    }

    private void updateBalanceDisplay() {
        if (this.currentAccount != null) {
            balanceLabel.setText(String.format("Balance: %,.2f DT", this.currentAccount.getBalance())); // Example format
        }
    }


    private void loadTransactionHistory() {
        if (currentAccount != null) {
            List<Transaction> history = transactionDAO.getHistoryByAccountId(currentAccount.getId(), 50); // Get last 50 transactions
            transactionData.setAll(history); // Update table data
        } else {
            transactionData.clear(); // Clear table if no account
        }
    }

    @FXML
    private void handleRefreshButtonAction(ActionEvent event) {
        loadAccountData();
        loadTransactionHistory();
        showInfoAlert("Refreshed", "Account balance and transaction history updated.");
    }


    @FXML
    private void handleDepositButtonAction(ActionEvent event) {
        if (currentAccount == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Deposit Funds");
        dialog.setHeaderText("Enter amount to deposit into account " + currentAccount.getId());
        dialog.setContentText("Amount (DT):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amountStr -> {
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    showErrorAlert("Invalid Amount", "Deposit amount must be positive.");
                    return;
                }

                // Perform deposit via DAO
                boolean success = accountDAO.deposit(currentAccount.getId(), amount);

                if (success) {
                    // Log transaction
                    Transaction depositTx = new Transaction(currentAccount.getId(), Transaction.TransactionType.DEPOSIT, amount, null);
                    transactionDAO.saveTransaction(depositTx);

                    // Refresh data
                    loadAccountData();
                    loadTransactionHistory();
                    showInfoAlert("Deposit Successful", String.format("Successfully deposited %,.2f DT.", amount));
                } else {
                    showErrorAlert("Deposit Failed", "Could not process the deposit.");
                }

            } catch (NumberFormatException e) {
                showErrorAlert("Invalid Input", "Please enter a valid number for the amount.");
            } catch (Exception e) {
                showErrorAlert("Error", "An unexpected error occurred: " + e.getMessage());
                e.printStackTrace(); // Log the full error
            }
        });
    }

    @FXML
    private void handleWithdrawButtonAction(ActionEvent event) {
        if (currentAccount == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Withdraw Funds");
        dialog.setHeaderText("Enter amount to withdraw from account " + currentAccount.getId());
        dialog.setContentText("Amount (DT):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amountStr -> {
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    showErrorAlert("Invalid Amount", "Withdrawal amount must be positive.");
                    return;
                }

                // Perform withdrawal via DAO
                boolean success = accountDAO.withdraw(currentAccount.getId(), amount);

                if (success) {
                    // Log transaction
                    Transaction withdrawTx = new Transaction(currentAccount.getId(), Transaction.TransactionType.WITHDRAW, amount, null);
                    transactionDAO.saveTransaction(withdrawTx);

                    // Refresh data
                    loadAccountData();
                    loadTransactionHistory();
                    showInfoAlert("Withdrawal Successful", String.format("Successfully withdrew %,.2f DT.", amount));
                } else {
                    // DAO handles insufficient funds message, show generic error here or check balance first
                    showErrorAlert("Withdrawal Failed", "Could not process the withdrawal (check funds?).");
                }

            } catch (NumberFormatException e) {
                showErrorAlert("Invalid Input", "Please enter a valid number for the amount.");
            } catch (Exception e) {
                showErrorAlert("Error", "An unexpected error occurred: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleTransferButtonAction(ActionEvent event) {
        if (currentAccount == null) return;

        // --- Create a custom dialog for transfer ---
        // This is more complex than TextInputDialog. You'd typically create a separate FXML
        // or build the dialog layout programmatically.
        // For simplicity, let's use two TextInputDialogs (not ideal UX).

        TextInputDialog targetAccountDialog = new TextInputDialog();
        targetAccountDialog.setTitle("Transfer Funds");
        targetAccountDialog.setHeaderText("Transfer from account " + currentAccount.getId());
        targetAccountDialog.setContentText("Enter target account ID:");

        Optional<String> targetResult = targetAccountDialog.showAndWait();
        if (targetResult.isPresent() && !targetResult.get().isEmpty()) {
            try {
                int targetAccountId = Integer.parseInt(targetResult.get());

                if (targetAccountId == currentAccount.getId()) {
                    showErrorAlert("Invalid Target", "Cannot transfer to the same account.");
                    return;
                }

                // Check if target account exists (optional but good practice)
                Account targetAccount = accountDAO.getAccountById(targetAccountId);
                if (targetAccount == null) {
                    showErrorAlert("Invalid Target", "Target account ID does not exist.");
                    return;
                }


                TextInputDialog amountDialog = new TextInputDialog();
                amountDialog.setTitle("Transfer Funds");
                amountDialog.setHeaderText("Transfer to account " + targetAccountId);
                amountDialog.setContentText("Amount (DT):");

                Optional<String> amountResult = amountDialog.showAndWait();
                if (amountResult.isPresent() && !amountResult.get().isEmpty()) {
                    try {
                        BigDecimal amount = new BigDecimal(amountResult.get());
                        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                            showErrorAlert("Invalid Amount", "Transfer amount must be positive.");
                            return;
                        }

                        // Perform transfer via DAO
                        boolean success = accountDAO.transfer(currentAccount.getId(), targetAccountId, amount);

                        if (success) {
                            // Log transactions for both accounts
                            Transaction transferOutTx = new Transaction(currentAccount.getId(), Transaction.TransactionType.TRANSFER_OUT, amount, targetAccountId);
                            Transaction transferInTx = new Transaction(targetAccountId, Transaction.TransactionType.TRANSFER_IN, amount, currentAccount.getId());
                            transactionDAO.saveTransaction(transferOutTx);
                            transactionDAO.saveTransaction(transferInTx);


                            // Refresh data
                            loadAccountData();
                            loadTransactionHistory();
                            showInfoAlert("Transfer Successful", String.format("Successfully transferred %,.2f DT to account %d.", amount, targetAccountId));
                        } else {
                            showErrorAlert("Transfer Failed", "Could not process the transfer (check funds?).");
                        }

                    } catch (NumberFormatException e) {
                        showErrorAlert("Invalid Input", "Please enter a valid number for the amount.");
                    }
                }

            } catch (NumberFormatException e) {
                showErrorAlert("Invalid Input", "Please enter a valid number for the target account ID.");
            } catch (Exception e) {
                showErrorAlert("Error", "An unexpected error occurred: " + e.getMessage());
                e.printStackTrace();
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
            showErrorAlert("Logout Error", "Failed to return to the login screen.");
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
}