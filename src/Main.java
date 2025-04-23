// Adjust package name if needed (e.g., com.yourcompany.bankingapp)

import database.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the login view as the starting point
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml")); // Path from compiled classes root
            if (loader.getLocation() == null) {
                throw new IOException("Cannot find FXML file: /view/login.fxml");
            }
            Parent root = loader.load();

            Scene scene = new Scene(root);

            // Attempt to load and apply CSS
            URL cssUrl = getClass().getResource("/resources/style.css"); // Path from compiled classes root
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("CSS applied successfully.");
            } else {
                System.err.println("Warning: CSS file not found at /resources/style.css");
            }


            primaryStage.setTitle("Banking Application - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // Optional: disable resizing
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            // Show a basic error dialog if FXML loading fails
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Application Error");
            alert.setHeaderText("Failed to load the application interface.");
            alert.setContentText("Could not load login.fxml. Please ensure the file exists and the path is correct.\nError: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @Override
    public void stop() throws Exception {
        // Ensure database connection is closed when the application exits
        System.out.println("Application shutting down...");
        Database.closeConnection();
        super.stop();
    }


    /**
     * The main entry point for all JavaFX applications.
     * The start method is called after the init method has returned,
     * and after the system is ready for the application to begin running.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // You can perform non-GUI initialization here if needed
        // e.g., loading configuration, checking prerequisites

        // Test DB connection early (optional)
        /*
        try {
            Database.getConnection();
            System.out.println("Initial DB connection test successful.");
            Database.closeConnection(); // Close test connection
        } catch (SQLException e) {
             System.err.println("FATAL: Could not connect to database on startup. Please check configuration.");
             // Optionally prevent launch if DB is essential
             // System.exit(1);
        }
        */

        // Launch the JavaFX application
        launch(args);
    }
}