package org.example.financetracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.financetracker.db.DatabaseManager;
import org.example.financetracker.db.SettingsDAO;
import java.io.IOException;
import java.sql.SQLException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException, SQLException {
        // Инициализация БД
        DatabaseManager.initializeDatabase();

        // Создаем настройки по умолчанию
        SettingsDAO settingsDAO = new SettingsDAO();
        settingsDAO.createDefaultSettings();

        // Проверяем, первый ли запуск
        if (settingsDAO.isFirstLaunch()) {
            // Первый запуск - показываем экран регистрации
            loadRegistrationScreen(stage);
        } else {
            // Уже регистрировался - показываем главный экран
            loadMainScreen(stage);
        }
    }

    private void loadRegistrationScreen(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("registration-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("Finance Tracker - Регистрация");
        stage.setScene(scene);
        stage.show();
    }

    private void loadMainScreen(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        stage.setTitle("Finance Tracker - Главный экран");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws SQLException {
        launch();
    }
}