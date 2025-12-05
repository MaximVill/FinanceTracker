package org.example.financetracker.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainController {

    @FXML
    private Label balanceLabel;

    @FXML
    private void initialize() {
        // Здесь будет логика: загрузка баланса, курсов, отображение данных
        balanceLabel.setText("Баланс: 0 RUB");
    }
}