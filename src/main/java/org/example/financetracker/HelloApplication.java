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
        DatabaseManager.initializeDatabase(); // Инициализация БД при запуске

        SettingsDAO settingsDAO = new SettingsDAO();
        String mainCurrency = settingsDAO.getMainCurrency();

        // Проверяем, был ли уже установлен профиль (если валюта не RUB по умолчанию или запись существует)
        if ("RUB".equals(mainCurrency)) {
            // Это может быть значение по умолчанию, нужно проверить, создана ли запись
            // Но в текущей логике SettingsDAO при чтении создаёт запись, если её нет
            // Поэтому проверим, есть ли запись с id=1
            try (var conn = DatabaseManager.getConnection()) {
                var stmt = conn.prepareStatement("SELECT COUNT(*) FROM app_settings WHERE id = 1");
                var rs = stmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    // Запись существует — значит, пользователь уже регистрировался
                    loadMainScreen(stage);
                } else {
                    // Записи нет — первый запуск
                    loadRegistrationScreen(stage);
                }
            }
        } else {
            // Основная валюта не RUB — значит, пользователь уже настроил профиль
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