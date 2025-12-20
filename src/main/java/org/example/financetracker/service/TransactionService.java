package org.example.financetracker.service;

import org.example.financetracker.db.DatabaseManager;
import org.example.financetracker.db.TransactionDAO;
import org.example.financetracker.db.ExchangeRateDAO;
import org.example.financetracker.db.SettingsDAO;
import org.example.financetracker.db.DataSource;

import org.example.financetracker.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionDAO transactionDAO;
    private final ExchangeRateService exchangeRateService;
    private final SettingsDAO settingsDAO;

    public TransactionService() throws SQLException {
        this.transactionDAO = new TransactionDAO();
        this.exchangeRateService = new ExchangeRateService(new ExchangeRateDAO(DatabaseManager.getConnection()));
        this.settingsDAO = new SettingsDAO();
    }

    // Добавление транзакции
    public void addTransaction(Transaction transaction) {
        transactionDAO.add(transaction);
        log.info("Транзакция добавлена через сервис: {}", transaction.getTitle());
    }

    // Удаление транзакции
    public void deleteTransaction(long id) {
        transactionDAO.delete(id);
        log.info("Транзакция удалена через сервис: id={}", id);
    }

    // Получение всех транзакций
    public List<Transaction> getAllTransactions() {
        return transactionDAO.findAll();
    }

    // Расчет общего баланса в основной валюте
    public BigDecimal calculateTotalBalance() {
        String mainCurrency = settingsDAO.getMainCurrency();
        List<Transaction> transactions = getAllTransactions();

        BigDecimal total = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            BigDecimal amount = t.getAmount();

            // Если валюта транзакции отличается от основной - конвертируем
            if (!t.getCurrency().equals(mainCurrency)) {
                BigDecimal rate = exchangeRateService.getRate(t.getCurrency(), mainCurrency);
                amount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            }

            // Определяем тип транзакции
            if (t.getCategory() != null && t.getCategory().getType() != null) {
                if ("income".equals(t.getCategory().getType())) {
                    // Доходы добавляются
                    total = total.add(amount);
                } else {
                    // Расходы вычитаются
                    total = total.subtract(amount);
                }
            } else {
                // Если категория не определена, считаем расходом
                total = total.subtract(amount);
            }
        }
        return total;
    }

    // Расчет отдельно доходов и расходов
    public Map<String, BigDecimal> calculateIncomeExpense() {
        String mainCurrency = settingsDAO.getMainCurrency();
        List<Transaction> transactions = getAllTransactions();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            BigDecimal amount = t.getAmount();

            // Конвертация в основную валюту
            if (!t.getCurrency().equals(mainCurrency)) {
                BigDecimal rate = exchangeRateService.getRate(t.getCurrency(), mainCurrency);
                amount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            }

            // Определяем тип по категории
            if (t.getCategory() != null && t.getCategory().getType() != null) {
                if ("income".equals(t.getCategory().getType())) {
                    income = income.add(amount);
                } else if ("expense".equals(t.getCategory().getType())) {
                    expense = expense.add(amount);
                }
            } else {
                // Если категория не определена, считаем расходом
                expense = expense.add(amount);
            }
        }

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("income", income);
        result.put("expense", expense);
        return result;
    }

    // Очистка всех транзакций (обнуление баланса)
    public void clearAllTransactions() {
        try (Connection conn = DataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM transactions");
            log.info("Все транзакции удалены");
        } catch (SQLException e) {
            log.error("Ошибка при очистке транзакций", e);
            throw new RuntimeException("Failed to clear transactions", e);
        }
    }
}