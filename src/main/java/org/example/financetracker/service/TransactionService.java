package org.example.financetracker.service;

import org.example.financetracker.db.DatabaseManager;
import org.example.financetracker.db.TransactionDAO;
import org.example.financetracker.db.ExchangeRateDAO;
import org.example.financetracker.db.SettingsDAO;

import org.example.financetracker.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

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

            total = total.add(amount);
        }

        return total;
    }
}