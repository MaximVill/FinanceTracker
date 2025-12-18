package org.example.financetracker.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.financetracker.db.CategoryDAO;
import org.example.financetracker.db.DatabaseManager;
import org.example.financetracker.db.ExchangeRateDAO;
import org.example.financetracker.db.SettingsDAO;
import org.example.financetracker.model.Category;
import org.example.financetracker.model.Transaction;
import org.example.financetracker.service.ExchangeRateService;
import org.example.financetracker.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    // UI
    @FXML private Label balanceLabel, errorLabel;
    @FXML private TextField titleField, amountField;
    @FXML private ComboBox<String> currencyComboBox, categoryComboBox, typeComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TableView<Transaction> transactionsTable;
    @FXML private TableColumn<Transaction, String> titleColumn, categoryColumn, typeColumn;
    @FXML private TableColumn<Transaction, String> currencyColumn;
    @FXML private TableColumn<Transaction, BigDecimal> amountColumn;
    @FXML private TableColumn<Transaction, LocalDate> dateColumn;

    // Данные
    private ObservableList<Transaction> transactionsData;
    private TransactionService transactionService;
    private ExchangeRateService exchangeRateService;
    private SettingsDAO settingsDAO;
    private CategoryDAO categoryDAO;

    @FXML
    private void initialize() {
        try {
            initServices();
            setupUI();
            loadData();
        } catch (Exception e) {
            showError("Ошибка инициализации");
            log.error("Ошибка", e);
        }
    }

    private void initServices() throws SQLException {
        settingsDAO = new SettingsDAO();
        transactionService = new TransactionService();
        exchangeRateService = new ExchangeRateService(new ExchangeRateDAO(DatabaseManager.getConnection()));
        categoryDAO = new CategoryDAO();
    }

    private void setupUI() {
        // Валюта
        currencyComboBox.getItems().addAll("RUB", "USD", "EUR");
        currencyComboBox.setValue(settingsDAO.getMainCurrency());

        // Тип транзакции
        typeComboBox.getItems().addAll("Доход", "Расход");
        typeComboBox.setValue("Расход");

        // Категории
        loadCategoriesToComboBox();
        datePicker.setValue(LocalDate.now());

        // Таблица
        titleColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getTitle()));
        amountColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getAmount()));
        dateColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getTransaction_date()));
        categoryColumn.setCellValueFactory(cell -> {
            Transaction t = cell.getValue();
            String name = (t.getCategory() != null && t.getCategory().getName() != null)
                    ? t.getCategory().getName() : "Без категории";
            return new javafx.beans.property.SimpleStringProperty(name);
        });
        typeColumn.setCellValueFactory(cell -> {
            Transaction t = cell.getValue();
            String typeStr = (t.getType() == Transaction.Type.INCOME) ? "Доход" : "Расход";
            return new javafx.beans.property.SimpleStringProperty(typeStr);
        });

        currencyColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getCurrency()));

        transactionsData = FXCollections.observableArrayList();
        transactionsTable.setItems(transactionsData);

        // Валидация суммы
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) amountField.setText(oldVal);
        });
    }

    private void loadCategoriesToComboBox() {
        categoryComboBox.getItems().clear();
        List<Category> categories = categoryDAO.getAll();
        for (Category cat : categories) {
            categoryComboBox.getItems().add(cat.getName());
        }
        if (!categories.isEmpty()) {
            categoryComboBox.setValue(categories.get(0).getName());
        }
    }

    @FXML
    private void handleAddTransaction() {
        try {
            if (!validateInput()) return;

            Transaction t = new Transaction();
            t.setTitle(titleField.getText().trim());
            t.setAmount(new BigDecimal(amountField.getText()));
            t.setCurrency(currencyComboBox.getValue());
            t.setTransaction_date(datePicker.getValue());

            // Тип
            Transaction.Type type = ("Доход".equals(typeComboBox.getValue()))
                    ? Transaction.Type.INCOME : Transaction.Type.EXPENSE;
            t.setType(type);

            // Категория
            String categoryName = categoryComboBox.getValue();
            Long categoryId = categoryDAO.getIdByName(categoryName);
            t.setCategory_id(categoryId);

            transactionService.addTransaction(t);
            transactionsData.add(0, t);
            updateBalance();
            clearForm();
            log.info("Добавлено: {}", t.getTitle());
            showNotification("Добавлено!");
        } catch (Exception e) {
            showError("Ошибка: " + e.getMessage());
            log.error("Ошибка добавления", e);
        }
    }

    @FXML
    private void handleAddCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Новая категория");
        dialog.setHeaderText("Введите название категории");
        dialog.setContentText("Название:");
        dialog.showAndWait().ifPresent(categoryName -> {
            if (categoryName.trim().isEmpty()) {
                showError("Название не может быть пустым");
                return;
            }
            if (categoryDAO.exists(categoryName)) {
                showError("Категория уже существует");
                return;
            }
            ChoiceDialog<String> typeDialog = new ChoiceDialog<>("expense", "expense", "income");
            typeDialog.setTitle("Тип категории");
            typeDialog.setHeaderText("Выберите тип категории");
            typeDialog.setContentText("Тип:");
            typeDialog.showAndWait().ifPresent(type -> {
                Category cat = categoryDAO.add(categoryName, type);
                if (cat != null) {
                    loadCategoriesToComboBox();
                    categoryComboBox.setValue(categoryName);
                    showNotification("Категория добавлена");
                }
            });
        });
    }

    @FXML
    private void handleDeleteCategory() {
        String categoryName = categoryComboBox.getValue();
        if (categoryName == null) return;
        Long categoryId = categoryDAO.getIdByName(categoryName);
        if (categoryId == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление категории");
        alert.setHeaderText("Удалить категорию '" + categoryName + "'?");
        alert.setContentText("Категория не будет удалена, если используется в транзакциях.");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (categoryDAO.delete(categoryId)) {
                    loadCategoriesToComboBox();
                    showNotification("Категория удалена");
                } else {
                    showError("Нельзя удалить используемую категорию");
                }
            }
        });
    }

    @FXML
    private void handleRefreshRates() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                exchangeRateService.refreshAllRates(settingsDAO.getMainCurrency());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            updateBalance();
            showNotification("Курсы обновлены");
        });
        task.setOnFailed(e -> showError("Ошибка обновления"));
        new Thread(task).start();
    }

    private void loadData() {
        transactionsData.setAll(transactionService.getAllTransactions());
        updateBalance();
    }

    private void updateBalance() {
        Platform.runLater(() -> {
            try {
                BigDecimal balance = transactionService.calculateTotalBalance();
                String currency = settingsDAO.getMainCurrency();
                balanceLabel.setText(String.format("Баланс: %,.2f %s", balance, currency));
            } catch (Exception e) {
                balanceLabel.setText("Ошибка расчета");
            }
        });
    }

    private boolean validateInput() {
        if (titleField.getText().trim().isEmpty()) {
            showError("Введите название");
            return false;
        }
        if (amountField.getText().trim().isEmpty()) {
            showError("Введите сумму");
            return false;
        }
        return true;
    }

    private void clearForm() {
        titleField.clear();
        amountField.clear();
        titleField.requestFocus();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText("⚠️ " + message);
            errorLabel.setVisible(true);
        });
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> errorLabel.setVisible(false));
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private void showNotification(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Уведомление");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}