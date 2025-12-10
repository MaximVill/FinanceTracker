package org.example.financetracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.financetracker.db.DatabaseManager;
import org.example.financetracker.db.TransactionDAO;
import org.example.financetracker.model.Transaction;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException, SQLException {
        DatabaseManager.initializeDatabase(); // Инициализация БД при запуске

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("registration-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        stage.setTitle("Finance Tracker");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws SQLException {
        /*
        // Инициализируем БД
        DatabaseManager.initializeDatabase();

        // Создаём DAO
        TransactionDAO dao = new TransactionDAO();

        // Создаём тестовую транзакцию
        Transaction t = new Transaction();
        t.setTitle("Тестовая покупка");
        t.setAmount(new BigDecimal("1250.75"));
        t.setCurrency("RUB");
        t.setTransaction_date(LocalDate.now());
        t.setCategory_id(1L); // временно, пока нет категорий

        // Сохраняем
        dao.add(t);
        System.out.println("Сохранена транзакция: " + t);

        // Читаем все транзакции
        List<Transaction> all = dao.findAll();
        System.out.println("Все транзакции:");
        all.forEach(System.out::println);
        */
        launch();
    }
}