package org.example.financetracker.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.financetracker.db.SettingsDAO;
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
        currencyComboBox.getItems().addAll("RUB", "USD", "EUR");
        currencyComboBox.setValue("RUB");
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 50) {
                nameField.setText(oldValue);
            }
        });
    }

    @FXML
    private void handleSubmit() {
        String mainCurrency = currencyComboBox.getValue();
        if (mainCurrency == null || mainCurrency.isEmpty()) {
            showError("Пожалуйста, выберите валюту");
            return;
        }

        try {
            SettingsDAO settingsDAO = new SettingsDAO();
            // Сначала создаём запись настроек (если нет), затем обновляем поля
            settingsDAO.createDefaultSettings();
            settingsDAO.setMainCurrency(mainCurrency);
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                settingsDAO.setUserName(name);
            }
            settingsDAO.setFirstLaunchComplete();
            log.info("Профиль настроен: имя={}, валюта={}", name, mainCurrency);

            // Переход на главный экран
            switchToMainScreen();
        } catch (Exception e) {
            log.error("Ошибка сохранения настроек", e);
            showError("Не удалось сохранить настройки: " + e.getMessage());
        }
    }

    private void switchToMainScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/financetracker/app-layout.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) submitButton.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 680));
            stage.setTitle("FinTrackGo");
            stage.setMinWidth(700);
            stage.setMinHeight(500);
            stage.centerOnScreen();
        } catch (IOException e) {
            log.error("Ошибка загрузки главного экрана", e);
            showError("Не удалось открыть главное окно");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
