package org.example.financetracker.db;

import org.example.financetracker.model.Transaction;
import org.example.financetracker.model.Category;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    private final Connection connection;

    public TransactionDAO() throws SQLException {
        this.connection = DatabaseManager.getConnection();
    }

    public void add(Transaction transaction) {
        String sql = "INSERT INTO transactions (title, amount, currency, transaction_date, category_id, type) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, transaction.getTitle());
            stmt.setBigDecimal(2, transaction.getAmount());
            stmt.setString(3, transaction.getCurrency());
            stmt.setDate(4, Date.valueOf(transaction.getTransaction_date()));
            stmt.setLong(5, transaction.getCategory_id());
            stmt.setString(6, transaction.getType().name());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    transaction.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка сохранения транзакции", e);
        }
    }

    public List<Transaction> findAll() {
        List<Transaction> transactions = new ArrayList<>();
        String sql = """
            SELECT t.id, t.title, t.amount, t.currency, t.transaction_date, t.category_id, t.type, 
                   c.name as category_name, c.type as category_type
            FROM transactions t
            LEFT JOIN categories c ON t.category_id = c.id
            ORDER BY t.transaction_date DESC, t.id DESC
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Transaction t = new Transaction();
                t.setId(rs.getLong("id"));
                t.setTitle(rs.getString("title"));
                t.setAmount(rs.getBigDecimal("amount"));
                t.setCurrency(rs.getString("currency"));
                t.setTransaction_date(rs.getDate("transaction_date").toLocalDate());
                t.setCategory_id(rs.getLong("category_id"));

                Category cat = null;
                if (rs.getString("category_name") != null) {
                    cat = new Category();
                    cat.setId(rs.getLong("category_id"));
                    cat.setName(rs.getString("category_name"));
                    cat.setType(rs.getString("category_type"));
                    t.setCategory(cat);
                }

                t.setType(Transaction.Type.valueOf(rs.getString("type")));
                transactions.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    public void delete(long id) {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}