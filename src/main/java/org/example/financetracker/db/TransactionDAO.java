package org.example.financetracker.db;

import org.example.financetracker.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    private static final Logger log = LoggerFactory.getLogger(TransactionDAO.class);

    public void add(Transaction t) {
        String sql = "INSERT INTO transactions (title, amount, currency, transaction_date, category_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, t.getTitle());
            stmt.setBigDecimal(2, t.getAmount());
            stmt.setString(3, t.getCurrency());
            stmt.setDate(4, Date.valueOf(t.getTransaction_date()));
            stmt.setLong(5, t.getCategory_id());

            stmt.executeUpdate();

            // Получаем сгенерированный id
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    t.setId(rs.getLong(1));
                }
            }

            log.info("Добавлена транзакция: {} {} {}", t.getAmount(), t.getCurrency(), t.getTitle());

        } catch (SQLException e) {
            log.error("Ошибка при добавлении транзакции", e);
            throw new RuntimeException("Failed to add transaction", e);
        }
    }

    // 2. Удаление по id
    public void delete(long id) {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
            log.info("Удалена транзакция с id={}", id);
        } catch (SQLException e) {
            log.error("Ошибка при удалении транзакции id={}", id, e);
            throw new RuntimeException("Failed to delete transaction", e);
        }
    }

    // 3. Получение всех транзакций
    public List<Transaction> findAll() {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT id, title, amount, currency, transaction_date, category_id FROM transactions ORDER BY transaction_date DESC";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Transaction t = new Transaction();
                t.setId(rs.getLong("id"));
                t.setTitle(rs.getString("title"));
                t.setAmount(rs.getBigDecimal("amount"));
                t.setCurrency(rs.getString("currency"));
                t.setTransaction_date(rs.getDate("transaction_date").toLocalDate());
                t.setCategory_id(rs.getLong("category_id"));
                transactions.add(t);
            }

        } catch (SQLException e) {
            log.error("Ошибка при загрузке транзакций", e);
            throw new RuntimeException("Failed to load transactions", e);
        }

        return transactions;
    }
}
