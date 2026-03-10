package org.example.financetracker.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import org.example.financetracker.db.*;
import org.example.financetracker.model.*;
import org.example.financetracker.service.ExchangeRateService;
import org.example.financetracker.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;

public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private StackPane  accountCarousel;
    @FXML private Label      accountNameLabel;
    @FXML private Label      accountBalanceLabel;
    @FXML private HBox       carouselDots;

    @FXML private Label totalCapitalLabel;
    @FXML private Label monthChangeLabel;
    @FXML private Label incomeLabel;
    @FXML private Label expenseLabel;
    @FXML private Label piggyLabel;
    @FXML private Label freeLabel;

    @FXML private VBox  recentTransactionsBox;
    @FXML private Label errorLabel;

    @FXML private TableView<Transaction>               transactionsTable;
    @FXML private TableColumn<Transaction, String>     titleColumn;
    @FXML private TableColumn<Transaction, String>     categoryColumn;
    @FXML private TableColumn<Transaction, String>     currencyColumn;
    @FXML private TableColumn<Transaction, String>     typeColumn;
    @FXML private TableColumn<Transaction, BigDecimal> amountColumn;
    @FXML private TableColumn<Transaction, LocalDate>  dateColumn;
    @FXML private TableColumn<Transaction, Void>       actionsColumn;

    private TransactionService  transactionService;
    private ExchangeRateService exchangeRateService;
    private SettingsDAO         settingsDAO;
    private CategoryDAO         categoryDAO;
    private AccountDAO          accountDAO;

    private ObservableList<Transaction> transactionsData;
    private List<Account> accounts = new ArrayList<>();
    private int currentAccountIndex = 0;

    private static final String[] ACCOUNT_COLORS = {
        "#27ae60","#e67e22","#e74c3c","#3498db","#9b59b6","#607d8b","#26a69a","#66bb6a"
    };

    private static final NumberFormat FMT = NumberFormat.getInstance(new Locale("ru","RU"));
    static { FMT.setMinimumFractionDigits(2); FMT.setMaximumFractionDigits(2); }

    @FXML
    private void initialize() {
        try {
            settingsDAO       = new SettingsDAO();
            transactionService  = new TransactionService();
            exchangeRateService = new ExchangeRateService(new ExchangeRateDAO());
            categoryDAO       = new CategoryDAO();
            accountDAO        = new AccountDAO();
            setupHiddenTable();
            loadData();
        } catch (Exception e) {
            showError("Ошибка инициализации: " + e.getMessage());
            log.error("Ошибка инициализации", e);
        }
    }

    private void loadData() {
        transactionsData = FXCollections.observableArrayList(transactionService.getAllTransactions());
        transactionsTable.setItems(transactionsData);
        accounts = accountDAO.getAll();
        refreshCarousel();
        updateSummary();
        updateRecentTransactions();
    }

    private void updateSummary() {
        Map<String, BigDecimal> totals = transactionService.calculateIncomeExpense();
        BigDecimal income  = totals.get("income");
        BigDecimal expense = totals.get("expense");
        BigDecimal total   = accountDAO.getTotalBalance();

        Platform.runLater(() -> {
            totalCapitalLabel.setText(FMT.format(total) + " ₽");
            incomeLabel.setText(FMT.format(income) + " ₽");
            expenseLabel.setText(FMT.format(expense) + " ₽");
            piggyLabel.setText("0,00 ₽");
            freeLabel.setText(FMT.format(total) + " ₽");

            if (income.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pct = expense.multiply(new BigDecimal("100"))
                        .divide(income, 1, RoundingMode.HALF_UP);
                monthChangeLabel.setText("↗ " + pct + "%  В ЭТОМ МЕСЯЦЕ");
                monthChangeLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #27ae60;");
            } else {
                monthChangeLabel.setText("Нет данных за месяц");
                monthChangeLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #9090a8;");
            }
        });
    }

    // ── Карусель счетов ───────────────────────────────────────────────────────

    private void refreshCarousel() {
        if (accounts.isEmpty()) {
            accountNameLabel.setText("Нет счетов");
            accountBalanceLabel.setText("0,00 ₽");
            accountCarousel.setStyle("-fx-background-color: #6c63ff; -fx-background-radius: 18;");
            carouselDots.getChildren().clear();
            return;
        }
        if (currentAccountIndex >= accounts.size()) currentAccountIndex = 0;
        Account acc = accounts.get(currentAccountIndex);
        accountNameLabel.setText(acc.getName());
        accountBalanceLabel.setText(FMT.format(acc.getBalance()) + " " + currencySymbol(acc.getCurrency()));
        accountCarousel.setStyle("-fx-background-color: " + acc.getColor() + "; -fx-background-radius: 18;");

        carouselDots.getChildren().clear();
        for (int i = 0; i < accounts.size(); i++) {
            Circle dot = new Circle(i == currentAccountIndex ? 5 : 3.5);
            dot.setFill(i == currentAccountIndex ? Color.WHITE : Color.color(1,1,1,0.45));
            HBox.setMargin(dot, new Insets(0,3,0,3));
            carouselDots.getChildren().add(dot);
        }
    }

    @FXML private void handlePrevAccount() {
        if (accounts.isEmpty()) return;
        currentAccountIndex = (currentAccountIndex - 1 + accounts.size()) % accounts.size();
        refreshCarousel();
    }

    @FXML private void handleNextAccount() {
        if (accounts.isEmpty()) return;
        currentAccountIndex = (currentAccountIndex + 1) % accounts.size();
        refreshCarousel();
    }

    // ── Диалог: Список счетов ─────────────────────────────────────────────────

    @FXML private void handleShowAccounts() {
        Dialog<ButtonType> dialog = createDialog("Ваши счета", "#4a56e2");
        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 16 20 8 20; -fx-background-color: #f4f4fb;");

        HBox sub = new HBox();
        Label subLbl = new Label("ВАШИ АКТИВЫ");
        subLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #9090a8;");
        Button addBtn = new Button("⊕ ДОБАВИТЬ");
        addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4a56e2; " +
                "-fx-border-color: #c0c8ff; -fx-border-radius: 16; -fx-background-radius: 16; " +
                "-fx-font-size: 12; -fx-padding: 5 14; -fx-cursor: hand;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        sub.getChildren().addAll(subLbl, sp, addBtn);

        VBox list = new VBox(8);
        for (Account acc : accounts) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-padding: 12 16;");
            Label icon = new Label("💳");
            icon.setStyle("-fx-background-color: " + acc.getColor() + "; -fx-background-radius: 10; -fx-padding: 8 10; -fx-font-size: 16;");
            VBox info = new VBox(2);
            Label nm = new Label(acc.getName());
            nm.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
            Label bl = new Label(FMT.format(acc.getBalance()) + " " + currencySymbol(acc.getCurrency()));
            bl.setStyle("-fx-font-size: 13; -fx-text-fill: #4a56e2; -fx-font-weight: bold;");
            info.getChildren().addAll(nm, bl);
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            Button del = new Button("🗑");
            del.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-size: 14; -fx-cursor: hand; -fx-padding: 4 6;");
            del.setOnAction(e -> { accountDAO.delete(acc.getId()); accounts = accountDAO.getAll(); loadData(); });
            row.getChildren().addAll(icon, info, spacer, del);
            list.getChildren().add(row);
        }

        addBtn.setOnAction(e -> { dialog.close(); handleAddAccount(); });
        content.getChildren().addAll(sub, list);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true); scroll.setPrefHeight(340);
        scroll.setStyle("-fx-background-color: #f4f4fb; -fx-background: #f4f4fb;");
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        styleOkBtn(dialog, "ГОТОВО", "#ffffff", "#1a1a2e");
        dialog.showAndWait(); loadData();
    }

    @FXML private void handleAddAccount() {
        Dialog<Account> dialog = createDialogTyped("Счёт", "#4a56e2");
        VBox content = new VBox(14);
        content.setStyle("-fx-padding: 20 24 8 24;");
        content.setPrefWidth(380);

        TextField nameField = styledField("Название счёта", 16);
        Label colorLbl = smallLabel("🎨 СТИЛЬ КАРТЫ");
        HBox colorPicker = new HBox(8);
        final String[] sel = {ACCOUNT_COLORS[0]};
        buildColorPicker(colorPicker, sel);

        Label currLbl = smallLabel("ВАЛЮТА СЧЕТА");
        ComboBox<String> currencyBox = new ComboBox<>();
        currencyBox.getItems().addAll("RUB","USD","EUR"); currencyBox.setValue("RUB");
        currencyBox.setMaxWidth(Double.MAX_VALUE);

        Label balLbl = smallLabel("НАЧАЛЬНЫЙ БАЛАНС");
        TextField balField = new TextField("0");
        balField.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 4 0;");

        content.getChildren().addAll(nameField, colorLbl, colorPicker, currLbl, currencyBox, balLbl, balField);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleOkBtn(dialog, "Сохранить", "#4a56e2", "white");

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            String name = nameField.getText().trim();
            if (name.isEmpty()) { showError("Введите название счёта"); return null; }
            BigDecimal bal;
            try { bal = new BigDecimal(balField.getText().trim().replace(",",".")); }
            catch (Exception e) { showError("Некорректный баланс"); return null; }
            return accountDAO.add(name, sel[0], currencyBox.getValue(), bal);
        });

        dialog.showAndWait().ifPresent(acc -> {
            if (acc != null) {
                accounts = accountDAO.getAll();
                currentAccountIndex = accounts.size() - 1;
                loadData(); showNotification("Счёт «" + acc.getName() + "» создан");
            }
        });
    }

    @FXML private void handleToggleAccounts() {}

    // ── Транзакции ────────────────────────────────────────────────────────────

    @FXML private void handleAddIncome()  { showTransactionDialog(true); }
    @FXML private void handleAddExpense() { showTransactionDialog(false); }

    private void showTransactionDialog(boolean isIncome) {
        String headerColor = isIncome ? "#27ae60" : "#e74c3c";
        String title = isIncome ? "Новый доход" : "Новый расход";
        Dialog<Transaction> dialog = createDialogTyped(title, headerColor);

        VBox content = new VBox(14);
        content.setStyle("-fx-padding: 16 24 8 24;");
        content.setPrefWidth(420);

        TextField titleField = styledField(isIncome ? "Источник дохода" : "Статья расхода", 14);

        HBox amountRow = new HBox(10);
        amountRow.setAlignment(Pos.CENTER_LEFT);
        TextField amountField = styledField("Сумма", 18);
        amountField.setStyle(amountField.getStyle() + "-fx-font-weight: bold;");
        HBox.setHgrow(amountField, Priority.ALWAYS);
        ComboBox<String> currencyBox = new ComboBox<>();
        currencyBox.getItems().addAll("RUB","USD","EUR");
        currencyBox.setValue(settingsDAO.getMainCurrency());
        amountRow.getChildren().addAll(amountField, currencyBox);

        HBox row2 = new HBox(10);
        ComboBox<String> accountBox = new ComboBox<>();
        accounts.forEach(a -> accountBox.getItems().add(a.getName()));
        if (!accounts.isEmpty()) accountBox.setValue(accounts.get(0).getName());
        accountBox.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(accountBox, Priority.ALWAYS);

        ComboBox<String> categoryBox = new ComboBox<>();
        String catType = isIncome ? "income" : "expense";
        categoryDAO.getAll().stream()
            .filter(c -> catType.equals(c.getType()))
            .forEach(c -> categoryBox.getItems().add(c.getName()));
        if (categoryBox.getItems().isEmpty()) categoryBox.getItems().add("Без категории");
        categoryBox.setValue(categoryBox.getItems().get(0));
        categoryBox.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(categoryBox, Priority.ALWAYS);
        row2.getChildren().addAll(accountBox, categoryBox);

        Label dateLbl = smallLabel("ДАТА ОПЕРАЦИИ");
        DatePicker dp = new DatePicker(LocalDate.now());
        dp.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(titleField, amountRow, row2, dateLbl, dp);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleOkBtn(dialog, "ДОБАВИТЬ", headerColor, "white");
        ((Button)dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Отмена");

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            String t = titleField.getText().trim();
            if (t.isEmpty()) { showError("Введите описание"); return null; }
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountField.getText().trim().replace(",","."));
                if (amount.compareTo(BigDecimal.ZERO) <= 0) { showError("Сумма > 0"); return null; }
            } catch (Exception e) { showError("Некорректная сумма"); return null; }
            String catName = categoryBox.getValue();
            Long catId = catName != null && !catName.equals("Без категории") ? categoryDAO.getIdByName(catName) : null;
            Transaction tx = new Transaction();
            tx.setTitle(t); tx.setAmount(amount); tx.setCurrency(currencyBox.getValue());
            tx.setTransaction_date(dp.getValue()); tx.setCategory_id(catId);
            if (catId != null) tx.setCategory(categoryDAO.getById(catId));
            return tx;
        });

        dialog.showAndWait().ifPresent(tx -> {
            transactionService.addTransaction(tx);
            transactionsData.add(0, tx);
            updateSummary(); updateRecentTransactions();
            showNotification("Транзакция добавлена!");
        });
    }

    // ── Обмен валют ───────────────────────────────────────────────────────────

    @FXML private void handleExchange() {
        Dialog<ButtonType> dialog = createDialog("Обмен валют", "#4a56e2");
        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 16 24 8 24;");
        content.setPrefWidth(420);

        ComboBox<String> fromAcc = new ComboBox<>();
        accounts.forEach(a -> fromAcc.getItems().add(a.getName()));
        if (!accounts.isEmpty()) fromAcc.setValue(accounts.get(0).getName());
        fromAcc.setMaxWidth(Double.MAX_VALUE);

        HBox fromRow = new HBox(10);
        TextField fromAmt = styledField("Сумма списания", 14);
        HBox.setHgrow(fromAmt, Priority.ALWAYS);
        ComboBox<String> fromCurr = new ComboBox<>();
        fromCurr.getItems().addAll("RUB","USD","EUR"); fromCurr.setValue("RUB");
        fromRow.getChildren().addAll(fromAmt, fromCurr);

        Label swap = new Label("⇌");
        swap.setMaxWidth(Double.MAX_VALUE); swap.setAlignment(Pos.CENTER);
        swap.setStyle("-fx-font-size: 22; -fx-text-fill: #4a56e2;");

        ComboBox<String> toAcc = new ComboBox<>();
        accounts.forEach(a -> toAcc.getItems().add(a.getName()));
        if (accounts.size() > 1) toAcc.setValue(accounts.get(1).getName());
        else if (!accounts.isEmpty()) toAcc.setValue(accounts.get(0).getName());
        toAcc.setMaxWidth(Double.MAX_VALUE);

        HBox toRow = new HBox(10);
        TextField toAmt = styledField("Сумма зачисления", 14);
        HBox.setHgrow(toAmt, Priority.ALWAYS);
        ComboBox<String> toCurr = new ComboBox<>();
        toCurr.getItems().addAll("RUB","USD","EUR"); toCurr.setValue("RUB");
        toRow.getChildren().addAll(toAmt, toCurr);

        Label dateLbl = smallLabel("ДАТА ОПЕРАЦИИ");
        DatePicker dp = new DatePicker(LocalDate.now());
        dp.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(fromAcc, fromRow, swap, toAcc, toRow, dateLbl, dp);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleOkBtn(dialog, "ДОБАВИТЬ", "#4a56e2", "white");
        ((Button)dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Отмена");
        dialog.showAndWait();
    }

    // ── Копилки ───────────────────────────────────────────────────────────────

    @FXML private void handleShowPiggy() {
        Dialog<ButtonType> dialog = new Dialog<>();
        VBox content = new VBox(12);
        content.setStyle("-fx-background-color: #4a56e2; -fx-padding: 24;");
        Label t = new Label("Ваши копилки");
        t.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: white;");
        Label s = new Label("Средства, отложенные на конкретные цели.");
        s.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.75);");
        VBox goalsBox = new VBox(8);
        goalsBox.setStyle("-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 12;");
        Label empty = new Label("Копилки пока не созданы");
        empty.setStyle("-fx-text-fill: #9090a8; -fx-font-size: 13;");
        goalsBox.getChildren().add(empty);
        content.getChildren().addAll(t, s, goalsBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ((Button)dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).setText("Закрыть");
        dialog.showAndWait();
    }

    // ── Прочие FXML-обработчики ───────────────────────────────────────────────

    @FXML private void handleRefreshRates() {
        Task<Void> task = new Task<>() { @Override protected Void call() {
            exchangeRateService.refreshAllRates(settingsDAO.getMainCurrency()); return null; }};
        task.setOnSucceeded(e -> { updateSummary(); showNotification("Курсы обновлены"); });
        task.setOnFailed(e -> showError("Ошибка обновления курсов"));
        new Thread(task).start();
    }

    @FXML private void handleOpenSettings() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(settingsDAO.getMainCurrency(),"RUB","USD","EUR");
        dialog.setTitle("Настройки"); dialog.setHeaderText("Основная валюта:");
        dialog.showAndWait().ifPresent(c -> {
            settingsDAO.setMainCurrency(c); updateSummary(); showNotification("Валюта: " + c);
        });
    }

    // ── Последние транзакции ──────────────────────────────────────────────────

    private void updateRecentTransactions() {
        Platform.runLater(() -> {
            recentTransactionsBox.getChildren().clear();
            List<Transaction> all = transactionService.getAllTransactions();
            List<Transaction> recent = all.size() > 5 ? all.subList(0,5) : all;
            if (recent.isEmpty()) {
                Label empty = new Label("Нет транзакций");
                empty.setStyle("-fx-text-fill: #c0c0d0; -fx-font-size: 13; -fx-padding: 10 0;");
                recentTransactionsBox.getChildren().add(empty);
                return;
            }
            for (Transaction tx : recent) {
                boolean isInc = tx.getCategory() != null && "income".equals(tx.getCategory().getType());
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 6 0;");
                Label icon = new Label(isInc ? "💚" : "🔴");
                icon.setStyle("-fx-font-size: 20;");
                VBox info = new VBox(2);
                Label nm = new Label(tx.getTitle());
                nm.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
                Label meta = new Label(tx.getTransaction_date() + "  " +
                        (tx.getCategory() != null ? tx.getCategory().getName() : "Без категории"));
                meta.setStyle("-fx-font-size: 11; -fx-text-fill: #9090a8;");
                info.getChildren().addAll(nm, meta);
                Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                String sign = isInc ? "+" : "-";
                String clr  = isInc ? "#27ae60" : "#e74c3c";
                Label amt = new Label(sign + FMT.format(tx.getAmount()) + " ₽");
                amt.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + clr + ";");
                row.getChildren().addAll(icon, info, spacer, amt);
                recentTransactionsBox.getChildren().add(row);
                if (recent.indexOf(tx) < recent.size()-1) {
                    Separator sep = new Separator(); sep.setStyle("-fx-opacity: 0.25;");
                    recentTransactionsBox.getChildren().add(sep);
                }
            }
        });
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private void setupHiddenTable() {
        titleColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        amountColumn.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getAmount()));
        currencyColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCurrency()));
        dateColumn.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTransaction_date()));
        categoryColumn.setCellValueFactory(c -> {
            Transaction t = c.getValue();
            return new SimpleStringProperty(t.getCategory() != null ? t.getCategory().getName() : "Без категории");
        });
        typeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategoryType()));
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty); setGraphic(null);
            }
        });
    }

    private Dialog<ButtonType> createDialog(String title, String color) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(title);
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: white;");
        HBox hdr = new HBox(lbl);
        hdr.setStyle("-fx-background-color: " + color + "; -fx-padding: 20 24 20 24;");
        d.getDialogPane().setHeader(hdr);
        return d;
    }

    @SuppressWarnings("unchecked")
    private <T> Dialog<T> createDialogTyped(String title, String color) {
        Dialog<T> d = new Dialog<>();
        d.setTitle(title);
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: white;");
        HBox hdr = new HBox(lbl);
        hdr.setStyle("-fx-background-color: " + color + "; -fx-padding: 20 24 20 24;");
        d.getDialogPane().setHeader(hdr);
        return d;
    }

    private void styleOkBtn(Dialog<?> d, String text, String bg, String fg) {
        Button btn = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        if (btn == null) return;
        btn.setText(text);
        btn.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                "-fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 16; -fx-padding: 10 40;");
    }

    private TextField styledField(String prompt, int fontSize) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-font-size: " + fontSize + "; -fx-padding: 10 14; -fx-background-radius: 10; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-background-color: #f8f8fc;");
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private Label smallLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10; -fx-text-fill: #9090a8; -fx-letter-spacing: 0.5;");
        return l;
    }

    private void buildColorPicker(HBox box, String[] sel) {
        for (String color : ACCOUNT_COLORS) {
            StackPane swatch = new StackPane();
            swatch.setPrefSize(36, 36);
            swatch.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10; -fx-cursor: hand;");
            swatch.setUserData(color);
            if (color.equals(sel[0])) {
                Label check = new Label("✓");
                check.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                swatch.getChildren().add(check);
            }
            swatch.setOnMouseClicked(e -> {
                sel[0] = color;
                box.getChildren().forEach(node -> {
                    StackPane s = (StackPane) node;
                    s.getChildren().clear();
                    if (color.equals(s.getUserData())) {
                        Label ch = new Label("✓");
                        ch.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                        s.getChildren().add(ch);
                    }
                });
            });
            box.getChildren().add(swatch);
        }
    }

    private void showNotification(String message) {
        Platform.runLater(() -> {
            errorLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; " +
                    "-fx-background-color: #e8f8ef; -fx-background-radius: 8; -fx-padding: 8 14;");
            errorLabel.setText("✅ " + message);
            errorLabel.setVisible(true);
        });
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; " +
                    "-fx-background-color: #fdecea; -fx-background-radius: 8; -fx-padding: 8 14;");
            errorLabel.setText("⚠️ " + message);
            errorLabel.setVisible(true);
        });
        new Thread(() -> {
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }

    private String currencySymbol(String code) {
        return switch (code) { case "USD" -> "$"; case "EUR" -> "€"; default -> "₽"; };
    }
}
