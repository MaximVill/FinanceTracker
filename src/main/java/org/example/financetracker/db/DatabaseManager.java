package org.example.financetracker.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL = "jdbc:h2:./finance_tracker;DB_CLOSE_ON_EXIT=FALSE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("H2 Driver not found", e);
            throw new RuntimeException("Failed to load H2 Driver", e);
        }
    }

    public static void initializeDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            createTables(connection);
            initDefaultCategories(connection);
            logger.info("Database initialized successfully!");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static void createTables(Connection connection) throws SQLException {
        // == TRANSACTIONS ==
        String createTransactions = """
            CREATE TABLE IF NOT EXISTS TRANSACTIONS (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(100) NOT NULL,
                amount DECIMAL(15, 2) NOT NULL,
                currency VARCHAR(3) NOT NULL,
                transaction_date DATE NOT NULL,
                category_id BIGINT
            )
            """;

        // == CATEGORIES ==
        String createCategories = """
            CREATE TABLE IF NOT EXISTS CATEGORIES (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(50) NOT NULL,
                type VARCHAR(10) NOT NULL CHECK (type IN ('income', 'expense'))
            )
            """;

        // == EXCHANGE_RATES ==
        String createExchangeRates = """
            CREATE TABLE IF NOT EXISTS EXCHANGE_RATES (
                from_currency VARCHAR(3) NOT NULL,
                to_currency VARCHAR(3) NOT NULL,
                rate DECIMAL(18, 6) NOT NULL,
                last_updated TIMESTAMP NOT NULL,
                PRIMARY KEY (from_currency, to_currency)
            )
            """;

        try (PreparedStatement ps1 = connection.prepareStatement(createTransactions);
             PreparedStatement ps2 = connection.prepareStatement(createCategories);
             PreparedStatement ps3 = connection.prepareStatement(createExchangeRates)) {
            ps1.execute();
            ps2.execute();
            ps3.execute();
        }
    }

    private static void initDefaultCategories(Connection connection) throws SQLException {
        // Проверим, есть ли уже категории — чтобы не дублировать
        String countCategories = "SELECT COUNT(*) FROM CATEGORIES";
        int count;
        try (var stmt = connection.prepareStatement(countCategories);
             var rs = stmt.executeQuery()) {
            rs.next();
            count = rs.getInt(1);
        }

        if (count == 0) {
            String[] income = {"Зарплата", "Подработка", "Подарок"};
            String[] expense = {"Продукты", "Транспорт", "ЖКХ", "Развлечения", "Одежда"};

            String insert = "INSERT INTO CATEGORIES (name, type) VALUES (?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insert)) {
                for (String name : income) {
                    ps.setString(1, name);
                    ps.setString(2, "income");
                    ps.addBatch();
                }
                for (String name : expense) {
                    ps.setString(1, name);
                    ps.setString(2, "expense");
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    // Утилита для получения соединения в DAO
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
