package org.example.financetracker.db;

import org.example.financetracker.model.ExchangeRate;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

public class ExchangeRateDAO {
    private final Connection connection;

    public ExchangeRateDAO(Connection connection) {
        this.connection = connection;
    }

    public ExchangeRate getRate(String from, String to) {
        String sql = "SELECT from_currency, to_currency, rate, last_updated FROM exchange_rates WHERE from_currency = ? AND to_currency = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, from);
            stmt.setString(2, to);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                ExchangeRate rate = new ExchangeRate();
                rate.setFrom_currency(rs.getString("from_currency"));
                rate.setTo_currency(rs.getString("to_currency"));
                rate.setRate(rs.getBigDecimal("rate"));
                rate.setLast_updated(rs.getTimestamp("last_updated").toLocalDateTime());
                return rate;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveRate(String from, String to, BigDecimal rate) {
        String sql = """
            MERGE INTO exchange_rates (from_currency, to_currency, rate, last_updated)
            KEY (from_currency, to_currency)
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, from);
            stmt.setString(2, to);
            stmt.setBigDecimal(3, rate);
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}