package org.example.financetracker.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
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
import java.util.Map;
import java.util.Optional;

public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private Label balanceLabel, errorLabel, incomeLabel, expenseLabel;
    @FXML private TableView<Transaction> transactionsTable;
    @FXML private TableColumn<Transaction, String> titleColumn, categoryColumn, currencyColumn, typeColumn;
    @FXML private TableColumn<Transaction, BigDecimal> amountColumn;
    @FXML private TableColumn<Transaction, LocalDate> dateColumn;
    @FXML private TableColumn<Transaction, Void> actionsColumn;

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
        // Настройка столбцов таблицы
        titleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        amountColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getAmount()));
        currencyColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCurrency()));
        dateColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getTransaction_date()));
        categoryColumn.setCellValueFactory(cell -> {
            Transaction t = cell.getValue();
            String name = (t.getCategory() != null) ? t.getCategory().getName() : "Без категории";
            return new SimpleStringProperty(name);
        });
        typeColumn.setCellValueFactory(cell -> {
            Transaction t = cell.getValue();
            return new SimpleStringProperty(t.getCategoryType());
        });

        // Столбец с кнопками действий
        actionsColumn.setCellFactory(new Callback<TableColumn<Transaction, Void>, TableCell<Transaction, Void>>() {
            @Override
            public TableCell<Transaction, Void> call(TableColumn<Transaction, Void> param) {
                return new TableCell<Transaction, Void>() {
                    private final Button editButton = new Button("Редактировать");
                    private final Button deleteButton = new Button("Удалить");

                    {
                        editButton.setOnAction(event -> {
                            Transaction transaction = getTableRow().getItem();
                            if (transaction != null) {
                                openEditDialog(transaction);
                            }
                        });
                        deleteButton.setOnAction(event -> {
                            Transaction transaction = getTableRow().getItem();
                            if (transaction != null) {
                                handleDeleteTransaction(transaction);
                            }
                        });
                        editButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
                        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox box = new HBox(editButton, deleteButton);
                            box.setSpacing(5);
                            setGraphic(box);
                        }
                    }
                };
            }
        });

        transactionsData = FXCollections.observableArrayList();
        transactionsTable.setItems(transactionsData);
    }

    @FXML
    private void handleAddIncome() {
        showTransactionDialog(true);
    }

    @FXML
    private void handleAddExpense() {
        showTransactionDialog(false);
    }

    private void showTransactionDialog(boolean isIncome) {
        Dialog<Transaction> dialog = new Dialog<>();
        dialog.setTitle(isIncome ? "Добавить доход" : "Добавить расход");
        dialog.setHeaderText(isIncome ? "Введите данные о доходе" : "Введите данные о расходе");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField();
        titleField.setPromptText("Например: Зарплата");

        TextField amountField = new TextField();
        amountField.setPromptText("0.00");

        ComboBox<String> currencyComboBox = new ComboBox<>();
        currencyComboBox.getItems().addAll("RUB", "USD", "EUR");
        currencyComboBox.setValue(settingsDAO.getMainCurrency());

        DatePicker datePicker = new DatePicker(LocalDate.now());

        ComboBox<String> categoryComboBox = new ComboBox<>();
        loadCategoriesToComboBox(categoryComboBox, isIncome ? "income" : "expense");

        HBox categoryBox = new HBox(5);
        TextField newCategoryField = new TextField();
        newCategoryField.setPromptText("Новая категория...");
        Button addCategoryBtn = new Button("+");
        addCategoryBtn.setOnAction(e -> {
            String newCatName = newCategoryField.getText().trim();
            if (!newCatName.isEmpty()) {
                Category newCat = categoryDAO.add(newCatName, isIncome ? "income" : "expense");
                if (newCat != null) {
                    categoryComboBox.getItems().add(newCatName);
                    categoryComboBox.setValue(newCatName);
                    newCategoryField.clear();
                }
            }
        });
        categoryBox.getChildren().addAll(categoryComboBox, newCategoryField, addCategoryBtn);

        grid.add(new Label("Название:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Сумма:"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("Валюта:"), 0, 2);
        grid.add(currencyComboBox, 1, 2);
        grid.add(new Label("Дата:"), 0, 3);
        grid.add(datePicker, 1, 3);
        grid.add(new Label("Категория:"), 0, 4);
        grid.add(categoryBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(titleField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (titleField.getText().trim().isEmpty()) {
                    showError("Введите название");
                    return null;
                }

                String amountText = amountField.getText().trim();
                if (amountText.isEmpty()) {
                    showError("Введите сумму");
                    return null;
                }

                try {
                    BigDecimal amount = new BigDecimal(amountText);
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        showError("Сумма должна быть больше нуля");
                        return null;
                    }
                } catch (NumberFormatException e) {
                    showError("Некорректная сумма");
                    return null;
                }

                String categoryName = categoryComboBox.getValue();
                if (categoryName == null || categoryName.isEmpty()) {
                    showError("Выберите категорию");
                    return null;
                }

                try {
                    Transaction t = new Transaction();
                    t.setTitle(titleField.getText().trim());
                    t.setAmount(new BigDecimal(amountField.getText()));
                    t.setCurrency(currencyComboBox.getValue());
                    t.setTransaction_date(datePicker.getValue());

                    Long categoryId = categoryDAO.getIdByName(categoryName);
                    t.setCategory_id(categoryId);

                    if (categoryId != null) {
                        Category category = categoryDAO.getById(categoryId);
                        t.setCategory(category);
                    }

                    return t;
                } catch (Exception e) {
                    showError("Ошибка: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<Transaction> result = dialog.showAndWait();
        result.ifPresent(transaction -> {
            try {
                transactionService.addTransaction(transaction);
                transactionsData.add(0, transaction);
                updateBalance();
                showNotification("Транзакция добавлена!");
            } catch (Exception e) {
                showError("Ошибка сохранения: " + e.getMessage());
            }
        });
    }

    private void loadCategoriesToComboBox(ComboBox<String> comboBox, String type) {
        comboBox.getItems().clear();
        List<Category> categories = categoryDAO.getAll();
        for (Category cat : categories) {
            if (type == null || type.equals(cat.getType())) {
                comboBox.getItems().add(cat.getName());
            }
        }
        if (!comboBox.getItems().isEmpty()) {
            comboBox.setValue(comboBox.getItems().get(0));
        }
    }

    private void handleDeleteTransaction(Transaction transaction) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление транзакции");
        alert.setHeaderText("Удалить транзакцию '" + transaction.getTitle() + "'?");
        alert.setContentText("Это действие нельзя отменить.");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    transactionService.deleteTransaction(transaction.getId());
                    transactionsData.remove(transaction);
                    updateBalance();
                    showNotification("Транзакция удалена");
                } catch (Exception e) {
                    showError("Ошибка удаления: " + e.getMessage());
                    log.error("Ошибка удаления транзакции", e);
                }
            }
        });
    }

    private void openEditDialog(Transaction transaction) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Редактирование транзакции");
        dialog.setHeaderText("Измените данные транзакции");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField editTitleField = new TextField(transaction.getTitle());
        TextField editAmountField = new TextField(transaction.getAmount().toString());
        ComboBox<String> editCurrencyComboBox = new ComboBox<>();
        editCurrencyComboBox.getItems().addAll("RUB", "USD", "EUR");
        editCurrencyComboBox.setValue(transaction.getCurrency());

        DatePicker editDatePicker = new DatePicker(transaction.getTransaction_date());

        ComboBox<String> editCategoryComboBox = new ComboBox<>();
        if (transaction.getCategory() != null) {
            String type = transaction.getCategory().getType();
            loadCategoriesToComboBox(editCategoryComboBox, type);
            editCategoryComboBox.setValue(transaction.getCategory().getName());
        } else {
            loadCategoriesToComboBox(editCategoryComboBox, null);
        }

        grid.add(new Label("Название:"), 0, 0);
        grid.add(editTitleField, 1, 0);
        grid.add(new Label("Сумма:"), 0, 1);
        grid.add(editAmountField, 1, 1);
        grid.add(new Label("Валюта:"), 0, 2);
        grid.add(editCurrencyComboBox, 1, 2);
        grid.add(new Label("Дата:"), 0, 3);
        grid.add(editDatePicker, 1, 3);
        grid.add(new Label("Категория:"), 0, 4);
        grid.add(editCategoryComboBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(() -> editTitleField.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (editTitleField.getText().trim().isEmpty()) {
                    showError("Введите название");
                    return null;
                }
                if (editAmountField.getText().trim().isEmpty()) {
                    showError("Введите сумму");
                    return null;
                }

                try {
                    BigDecimal amount = new BigDecimal(editAmountField.getText());
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        showError("Сумма должна быть больше нуля");
                        return null;
                    }
                } catch (NumberFormatException e) {
                    showError("Некорректная сумма");
                    return null;
                }

                try {
                    transaction.setTitle(editTitleField.getText().trim());
                    transaction.setAmount(new BigDecimal(editAmountField.getText()));
                    transaction.setCurrency(editCurrencyComboBox.getValue());
                    transaction.setTransaction_date(editDatePicker.getValue());

                    String categoryName = editCategoryComboBox.getValue();
                    Long categoryId = categoryDAO.getIdByName(categoryName);
                    transaction.setCategory_id(categoryId);

                    if (categoryId != null) {
                        Category category = categoryDAO.getById(categoryId);
                        if (category != null) {
                            transaction.setCategory(category);
                        }
                    }

                    saveTransaction(transaction);

                    int index = transactionsData.indexOf(transaction);
                    if (index >= 0) {
                        transactionsData.set(index, transaction);
                    }
                    updateBalance();
                    showNotification("Транзакция обновлена");
                    return dialogButton;
                } catch (Exception e) {
                    showError("Ошибка: " + e.getMessage());
                    log.error("Ошибка редактирования", e);
                    return null;
                }
            }
            return dialogButton;
        });

        dialog.showAndWait();
    }

    private void saveTransaction(Transaction transaction) {
        try {
            transactionService.deleteTransaction(transaction.getId());
            transactionService.addTransaction(transaction);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось обновить транзакцию", e);
        }
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

    @FXML
    private void handleChangeCurrency() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                settingsDAO.getMainCurrency(), "RUB", "USD", "EUR");
        dialog.setTitle("Смена основной валюты");
        dialog.setHeaderText("Выберите основную валюту:");
        dialog.setContentText("Валюта:");
        dialog.showAndWait().ifPresent(currency -> {
            if (!currency.equals(settingsDAO.getMainCurrency())) {
                settingsDAO.setMainCurrency(currency);
                loadData();
                updateBalance();
                showNotification("Основная валюта изменена на " + currency);
            }
        });
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
                showNotification("Все транзакции удалены");
            }
        });
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
                if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    balanceLabel.setText(String.format("Баланс: +%,.2f %s", balance, currency));
                    balanceLabel.setStyle("-fx-text-fill: green; -fx-font-size: 24; -fx-font-weight: bold;");
                } else {
                    balanceLabel.setText(String.format("Баланс: -%,.2f %s", balance.abs(), currency));
                    balanceLabel.setStyle("-fx-text-fill: red; -fx-font-size: 24; -fx-font-weight: bold;");
                }
                updateIncomeExpense();
            } catch (Exception e) {
                balanceLabel.setText("Ошибка расчета");
                balanceLabel.setStyle("-fx-text-fill: orange;");
            }
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