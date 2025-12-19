package org.example.financetracker.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    // UI
    @FXML private Label balanceLabel, errorLabel;
    @FXML private TextField titleField, amountField;
    @FXML private ComboBox<String> currencyComboBox, categoryComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TableView<Transaction> transactionsTable;
    @FXML private TableColumn<Transaction, String> titleColumn, categoryColumn;
    @FXML private TableColumn<Transaction, BigDecimal> amountColumn;
    @FXML private TableColumn<Transaction, LocalDate> dateColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;
    @FXML private TableColumn<Transaction, BigDecimal> convertedColumn;
    @FXML private Label incomeLabel, expenseLabel;


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
        // валюта
        currencyComboBox.getItems().addAll("RUB", "USD", "EUR");
        currencyComboBox.setValue(settingsDAO.getMainCurrency());

        // категории из БД
        loadCategoriesToComboBox();

        datePicker.setValue(LocalDate.now());

        // Таблица
        titleColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle()));

        amountColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getAmount()));

        dateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getTransaction_date()));

        categoryColumn.setCellValueFactory(cell -> {
            Transaction t = cell.getValue();
            if (t.getCategory() != null && t.getCategory().getName() != null) {
                return new SimpleStringProperty(t.getCategory().getName());
            } else {
                return new SimpleStringProperty("Без категории");
            }
        });

        transactionsData = FXCollections.observableArrayList();
        transactionsTable.setItems(transactionsData);

        // валидация
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) amountField.setText(oldVal);
        });

        // Настройка новых колонок
        typeColumn.setCellValueFactory(cell -> {
            Transaction t = cell.getValue();
            if (t.getCategory() != null && t.getCategory().getType() != null) {
                if ("income".equals(t.getCategory().getType())) {
                    return new SimpleStringProperty("+");
                } else {
                    return new SimpleStringProperty("-");
                }
            } else {
                return new SimpleStringProperty("-"); // по умолчанию расход
            }
        });

        typeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("+".equals(item)) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                }
            }
        });

        convertedColumn.setCellValueFactory(cell -> {
            Transaction t = cell.getValue();
            String mainCurrency = settingsDAO.getMainCurrency();

            if (t.getCurrency().equals(mainCurrency)) {
                return new SimpleObjectProperty<>(t.getAmount());
            } else {
                try {
                    BigDecimal rate = exchangeRateService.getRate(t.getCurrency(), mainCurrency);
                    BigDecimal converted = t.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
                    return new SimpleObjectProperty<>(converted);
                } catch (Exception e) {
                    return new SimpleObjectProperty<>(BigDecimal.ZERO);
                }
            }
        });

        convertedColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%,.2f %s", item, settingsDAO.getMainCurrency()));
                    setStyle("-fx-font-weight: bold;");
                }
            }
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
    private void handleClearAll() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Очистка данных");
        alert.setHeaderText("Очистить все транзакции?");
        alert.setContentText("Все данные будут удалены без возможности восстановления.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                transactionService.clearAllTransactions();
                transactionsData.clear();
                updateBalance();
                updateIncomeExpense();
                showNotification("Все транзакции удалены");
            }
        });
    }

    @FXML
    private void handleChangeCurrency() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                settingsDAO.getMainCurrency(),
                "RUB", "USD", "EUR"
        );
        dialog.setTitle("Смена основной валюты");
        dialog.setHeaderText("Выберите основную валюту:");
        dialog.setContentText("Валюта:");

        dialog.showAndWait().ifPresent(currency -> {
            if (!currency.equals(settingsDAO.getMainCurrency())) {
                settingsDAO.setMainCurrency(currency);
                currencyComboBox.setValue(currency);

                // Обновляем данные
                loadData();
                updateIncomeExpense();
                showNotification("Основная валюта изменена на " + currency);
            }
        });
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

            // категория
            String categoryName = categoryComboBox.getValue();
            Long categoryId = categoryDAO.getIdByName(categoryName);
            t.setCategory_id(categoryId);

            // Получаем объект категории для отображения
            if (categoryId != null) {
                Category category = new Category();
                category.setId(categoryId);
                category.setName(categoryName);

                // Определяем тип категории
                List<Category> allCategories = categoryDAO.getAll();
                for (Category cat : allCategories) {
                    if (cat.getId().equals(categoryId)) {
                        category.setType(cat.getType());
                        break;
                    }
                }
                t.setCategory(category);
            }

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

            // Спросим тип категории
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

                // Форматирование с цветом
                if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    balanceLabel.setText(String.format("Баланс: +%,.2f %s", balance, currency));
                    balanceLabel.setStyle("-fx-text-fill: green; -fx-font-size: 24; -fx-font-weight: bold;");
                } else {
                    balanceLabel.setText(String.format("Баланс: -%,.2f %s", balance.abs(), currency));
                    balanceLabel.setStyle("-fx-text-fill: red; -fx-font-size: 24; -fx-font-weight: bold;");
                }

                updateIncomeExpense(); // Обновляем доходы/расходы

            } catch (Exception e) {
                balanceLabel.setText("Ошибка расчета");
                balanceLabel.setStyle("-fx-text-fill: orange;");
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

    private void updateIncomeExpense() {
        try {
            Map<String, BigDecimal> totals = transactionService.calculateIncomeExpense();
            BigDecimal income = totals.get("income");
            BigDecimal expense = totals.get("expense");

            Platform.runLater(() -> {
                incomeLabel.setText(String.format("Доходы: +%,.2f", income));
                expenseLabel.setText(String.format("Расходы: -%,.2f", expense));
            });
        } catch (Exception e) {
            log.error("Ошибка расчета доходов/расходов", e);
        }
    }
}