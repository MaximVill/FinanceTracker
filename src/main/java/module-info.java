module org.example.financetracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.h2database;
    requires org.slf4j;

    opens org.example.financetracker.ui to javafx.fxml;
    exports org.example.financetracker;
}