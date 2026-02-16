package net.kcww.ec.nqueens;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class NQueensApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            URL fxmlUrl = getClass().getResource("/nqueens.fxml");
            if (fxmlUrl == null) {
                throw new IOException("File 'nqueens.fxml' not found in resources.");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setTitle("N-Queens Visualizer");
            primaryStage.setScene(scene);

            primaryStage.setOnCloseRequest(e -> {
                Platform.exit();
                System.exit(0);
            });

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Startup Error");
            alert.setHeaderText("Application Failed to Start");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

}