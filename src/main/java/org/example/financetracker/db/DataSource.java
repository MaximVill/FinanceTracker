package org.example.financetracker.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {
    private static final String URL = "jdbc:h2:./finance_tracker;DB_CLOSE_ON_EXIT=FALSE;USER=sa;PASSWORD=";
    private static final String USER = "";
    private static final String PASSWORD = "";

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:h2:./finance_tracker;DB_CLOSE_ON_EXIT=FALSE",
                "sa",
                ""
        );
    }
}
