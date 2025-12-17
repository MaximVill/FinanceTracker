module org.example.financetracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.h2database;
    requires org.slf4j;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    opens org.example.financetracker.ui to javafx.fxml;
    opens org.example.financetracker.model to javafx.base, com.fasterxml.jackson.databind;
    exports org.example.financetracker;
    exports org.example.financetracker.ui;
    exports org.example.financetracker.model;
    exports org.example.financetracker.service;
    exports org.example.financetracker.db;
}