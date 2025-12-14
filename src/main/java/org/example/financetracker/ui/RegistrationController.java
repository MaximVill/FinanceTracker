package org.example.financetracker.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.financetracker.db.SettingsDAO;
import org.example.financetracker.model.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RegistrationController {
    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> currencyComboBox;

    @FXML
    private Button submitButton;

    @FXML
    private Label errorLabel;

    @FXML
    private void initialize() {
        // Инициализация выпадающего списка валют
        currencyComboBox.getItems().addAll("RUB", "USD", "EUR");
        currencyComboBox.setValue("RUB");

        // Настройка валидации
        setupValidation();
    }

    private void setupValidation() {
        // Имя может быть пустым, но если введено - проверить длину
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 50) {
                nameField.setText(oldValue);
            }
        });
    }

    @FXML
    private void handleSubmit() {
        String userName = nameField.getText().trim();
        String mainCurrency = currencyComboBox.getValue();

        // Валидация
        if (mainCurrency == null || mainCurrency.isEmpty()) {
            showError("Пожалуйста, выберите валюту");
            return;
        }

        try {
            // Сохраняем настройки в БД
            SettingsDAO settingsDAO = new SettingsDAO();
            settingsDAO.setMainCurrency(mainCurrency);

            log.info("Пользователь зарегистрирован: имя='{}', валюта='{}'",
                    userName.isEmpty() ? "Гость" : userName, mainCurrency);

            // Переход на основной экран
            switchToMainScreen();

        } catch (Exception e) {
            log.error("Ошибка при сохранении настроек", e);
            showError("Ошибка сохранения настроек: " + e.getMessage());
        }
    }

    private void switchToMainScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/financetracker/main-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) submitButton.getScene().getWindow();
            Scene scene = new Scene(root, 900, 600);
            stage.setScene(scene);
            stage.setTitle("Finance Tracker - Главный экран");
            stage.centerOnScreen();

            log.info("Переход на главный экран");

        } catch (IOException e) {
            log.error("Ошибка загрузки главного экрана", e);
            showError("Не удалось загрузить главный экран");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}