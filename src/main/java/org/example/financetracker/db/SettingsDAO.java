package org.example.financetracker.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsDAO {
    private static final Logger log = LoggerFactory.getLogger(SettingsDAO.class);

    // Получить основную валюту
    public String getMainCurrency() {
        String sql = "SELECT main_currency FROM app_settings WHERE id = 1";
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String currency = rs.getString("main_currency");
                return currency != null ? currency : "RUB";
            } else {
                // Создаем запись по умолчанию
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO app_settings (id, main_currency) VALUES (1, 'RUB')")) {
                    insert.executeUpdate();
                }
                return "RUB";
            }

        } catch (SQLException e) {
            log.error("Ошибка при чтении настроек", e);
            return "RUB"; // Значение по умолчанию
        }
    }

    // Сохранить основную валюту
    public void setMainCurrency(String currency) {
        String sql = "MERGE INTO app_settings (id, main_currency) KEY (id) VALUES (1, ?)";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currency);
            stmt.executeUpdate();
            log.info("Основная валюта установлена: {}", currency);
        } catch (SQLException e) {
            log.error("Ошибка при сохранении валюты", e);
            throw new RuntimeException("Failed to save main currency", e);
        }
    }
}