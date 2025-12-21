package org.example.financetracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.financetracker.db.DatabaseManager;
import org.example.financetracker.db.SettingsDAO;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException, SQLException {
        DatabaseManager.initializeDatabase();

        if (isFirstLaunch()) {
            // показ регистрации
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("registration-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            stage.setTitle("Finance Tracker - Настройка");
            stage.setScene(scene);
        } else {
            // переход сразу на главный экран
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 900, 600);
            stage.setTitle("Finance Tracker");
            stage.setScene(scene);
        }

        stage.show();
    }

    private boolean isFirstLaunch() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM app_settings WHERE id = 1");
             ResultSet rs = stmt.executeQuery()) {
            return !rs.next(); // если записи нет то первый запуск
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
