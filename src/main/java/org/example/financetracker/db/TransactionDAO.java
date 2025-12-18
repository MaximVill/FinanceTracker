package org.example.financetracker.db;

import org.example.financetracker.model.Category;
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
        String sql = """
            SELECT t.*, c.name as category_name, c.type as category_type
            FROM transactions t
            LEFT JOIN categories c ON t.category_id = c.id
            ORDER BY t.transaction_date DESC
            """;

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

                // Если есть категория, создаем объект
                if (rs.getLong("category_id") > 0) {
                    Category cat = new Category();
                    cat.setId(rs.getLong("category_id"));
                    cat.setName(rs.getString("category_name"));
                    cat.setType(rs.getString("category_type"));
                    t.setCategory(cat); // Используем новый сеттер
                }

                transactions.add(t);
            }

        } catch (SQLException e) {
            log.error("Ошибка при загрузке транзакций", e);
            throw new RuntimeException("Failed to load transactions", e);
        }

        return transactions;
    }
}
