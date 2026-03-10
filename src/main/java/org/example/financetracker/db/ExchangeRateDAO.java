package org.example.financetracker.db;

import org.example.financetracker.model.ExchangeRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

public class ExchangeRateDAO {
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateDAO.class);

    // Конструктор без параметров — соединение берётся из DataSource на каждый запрос
    public ExchangeRateDAO() {}

    public ExchangeRate getRate(String from, String to) {
        String sql = "SELECT from_currency, to_currency, rate, last_updated " +
                     "FROM exchange_rates WHERE from_currency = ? AND to_currency = ?";
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, from);
            stmt.setString(2, to);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ExchangeRate rate = new ExchangeRate();
                    rate.setFrom_currency(rs.getString("from_currency"));
                    rate.setTo_currency(rs.getString("to_currency"));
                    rate.setRate(rs.getBigDecimal("rate"));
                    rate.setLast_updated(rs.getTimestamp("last_updated").toLocalDateTime());
                    return rate;
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка получения курса {} -> {}", from, to, e);
        }
        return null;
    }

    public void saveRate(String from, String to, BigDecimal rate) {
        String sql = """
            MERGE INTO exchange_rates (from_currency, to_currency, rate, last_updated)
            KEY (from_currency, to_currency)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, from);
            stmt.setString(2, to);
            stmt.setBigDecimal(3, rate);
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            log.debug("Сохранён курс {} -> {}: {}", from, to, rate);
        } catch (SQLException e) {
            log.error("Ошибка сохранения курса {} -> {}", from, to, e);
        }
    }
}