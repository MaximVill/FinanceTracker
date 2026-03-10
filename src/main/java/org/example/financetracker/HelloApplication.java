package org.example.financetracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.financetracker.db.DatabaseManager;
import org.example.financetracker.db.SettingsDAO;
import java.io.IOException;
import org.example.financetracker.db.DataSource;
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
            Scene scene = new Scene(fxmlLoader.load(), 860, 620);
            stage.setTitle("FinTrackGo - Настройка");
            stage.setScene(scene);
        } else {
            // главный layout с sidebar
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("app-layout.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1100, 680);
            stage.setTitle("FinTrackGo");
            stage.setMinWidth(700);
            stage.setMinHeight(500);
            stage.setScene(scene);
        }

        stage.show();
    }

    private boolean isFirstLaunch() throws SQLException {
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM app_settings WHERE id = 1");
             ResultSet rs = stmt.executeQuery()) {
            return !rs.next(); // если записи нет то первый запуск
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
