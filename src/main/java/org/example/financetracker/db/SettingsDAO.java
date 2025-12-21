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
                        "INSERT INTO app_settings (id, main_currency, first_launch) VALUES (1, 'RUB', TRUE)")) {
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

    // Отметить, что первый запуск завершен
    public void setFirstLaunchComplete() {
        String sql = "UPDATE app_settings SET first_launch = FALSE WHERE id = 1";
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            log.info("Первый запуск отмечен как завершенный");
        } catch (SQLException e) {
            log.error("Ошибка обновления first_launch", e);
        }
    }

    // Проверить, первый ли это запуск
    public boolean isFirstLaunch() {
        String sql = "SELECT first_launch FROM app_settings WHERE id = 1";
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getBoolean("first_launch");
            }
            // Если записи нет, значит первый запуск
            return true;
        } catch (SQLException e) {
            log.error("Ошибка чтения first_launch", e);
            return true;
        }
    }

    // Создать настройки по умолчанию (если их нет)
    public void createDefaultSettings() {
        String sql = "INSERT INTO app_settings (id, main_currency, first_launch) " +
                "SELECT 1, 'RUB', TRUE " +
                "WHERE NOT EXISTS (SELECT 1 FROM app_settings WHERE id = 1)";
        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Ошибка создания настроек по умолчанию", e);
        }
    }
}