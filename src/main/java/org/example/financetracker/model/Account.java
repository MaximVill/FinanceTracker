package org.example.financetracker.model;

import java.math.BigDecimal;

public class Account {
    private Long id;
    private String name;
    private String color;   // hex, напр. "#27ae60"
    private String currency;
    private BigDecimal balance;
    private int sortOrder;

    public Account() {}

    public Account(String name, String color, String currency, BigDecimal balance) {
        this.name = name;
        this.color = color;
        this.currency = currency;
        this.balance = balance;
    }

    @Override
    public String toString() { return name; }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
