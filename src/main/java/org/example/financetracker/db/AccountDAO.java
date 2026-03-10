package org.example.financetracker.db;

import org.example.financetracker.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccountDAO {
    private static final Logger log = LoggerFactory.getLogger(AccountDAO.class);

    public List<Account> getAll() {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM accounts ORDER BY sort_order, id";
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            log.error("Ошибка загрузки счетов", e);
        }
        return list;
    }

    public Account add(String name, String color, String currency, BigDecimal balance) {
        String sql = "INSERT INTO accounts (name, color, currency, balance, sort_order) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int maxOrder = getMaxSortOrder();
            stmt.setString(1, name.trim());
            stmt.setString(2, color);
            stmt.setString(3, currency);
            stmt.setBigDecimal(4, balance);
            stmt.setInt(5, maxOrder + 1);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Account a = new Account(name.trim(), color, currency, balance);
                    a.setId(rs.getLong(1));
                    a.setSortOrder(maxOrder + 1);
                    log.info("Добавлен счёт: {}", name);
                    return a;
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка добавления счёта: {}", name, e);
        }
        return null;
    }

    public boolean update(Account account) {
        String sql = "UPDATE accounts SET name=?, color=?, currency=?, balance=? WHERE id=?";
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, account.getName());
            stmt.setString(2, account.getColor());
            stmt.setString(3, account.getCurrency());
            stmt.setBigDecimal(4, account.getBalance());
            stmt.setLong(5, account.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Ошибка обновления счёта id={}", account.getId(), e);
            return false;
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM accounts WHERE id=?";
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Ошибка удаления счёта id={}", id, e);
            return false;
        }
    }

    /** Суммарный баланс по всем счетам */
    public BigDecimal getTotalBalance() {
        String sql = "SELECT COALESCE(SUM(balance), 0) FROM accounts";
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (SQLException e) {
            log.error("Ошибка расчёта суммарного баланса", e);
        }
        return BigDecimal.ZERO;
    }

    private int getMaxSortOrder() {
        String sql = "SELECT COALESCE(MAX(sort_order), 0) FROM accounts";
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.warn("Не удалось получить max sort_order", e);
        }
        return 0;
    }

    private Account map(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getLong("id"));
        a.setName(rs.getString("name"));
        a.setColor(rs.getString("color"));
        a.setCurrency(rs.getString("currency"));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setSortOrder(rs.getInt("sort_order"));
        return a;
    }
}
