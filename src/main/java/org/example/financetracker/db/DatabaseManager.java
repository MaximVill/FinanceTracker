package org.example.financetracker.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL = "jdbc:h2:./data/finance_tracker;MODE=MYSQL;DATABASE_TO_UPPER=false";
    private static Connection connection;

    public static void initializeDatabase() {
        try {
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection(DB_URL, "sa", "");
            try (Statement stmt = connection.createStatement()) {
                // Таблица настроек
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS app_settings (
                        id INT PRIMARY KEY,
                        main_currency VARCHAR(3) NOT NULL DEFAULT 'RUB'
                    )
                    """);
                stmt.execute("MERGE INTO app_settings (id, main_currency) KEY(id) VALUES (1, 'RUB')");

                // Категории
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(50) NOT NULL UNIQUE,
                        type VARCHAR(10) NOT NULL CHECK (type IN ('income', 'expense'))
                    )
                    """);

                // Транзакции
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        title VARCHAR(100) NOT NULL,
                        amount DECIMAL(15, 2) NOT NULL,
                        currency VARCHAR(3) NOT NULL,
                        transaction_date DATE NOT NULL,
                        category_id BIGINT,
                        type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
                        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
                    )
                    """);

                // Курсы валют
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS exchange_rates (
                        from_currency VARCHAR(3) NOT NULL,
                        to_currency VARCHAR(3) NOT NULL,
                        rate DECIMAL(18, 6) NOT NULL,
                        last_updated TIMESTAMP NOT NULL,
                        PRIMARY KEY (from_currency, to_currency)
                    )
                    """);

                // Базовые категории
                stmt.execute("MERGE INTO categories (name, type) KEY(name) VALUES ('Зарплата', 'income')");
                stmt.execute("MERGE INTO categories (name, type) KEY(name) VALUES ('Подработка', 'income')");
                stmt.execute("MERGE INTO categories (name, type) KEY(name) VALUES ('Продукты', 'expense')");
                stmt.execute("MERGE INTO categories (name, type) KEY(name) VALUES ('Транспорт', 'expense')");

            }
            log.info("База данных инициализирована успешно!");
        } catch (Exception e) {
            log.error("Ошибка инициализации базы данных", e);
            throw new RuntimeException("Не удалось инициализировать БД", e);
        }
    }

    public static Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("База данных не инициализирована");
        }
        return connection;
    }
}