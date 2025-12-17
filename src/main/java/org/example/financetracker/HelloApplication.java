package org.example.financetracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.financetracker.db.DatabaseManager;
import java.io.IOException;
import java.sql.SQLException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException, SQLException {
        DatabaseManager.initializeDatabase(); // Инициализация БД при запуске

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("registration-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        stage.setTitle("Finance Tracker");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws SQLException {
        launch();
    }
}