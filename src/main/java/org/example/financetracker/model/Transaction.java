package org.example.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction {
    private Long id;
    private String title;
    private BigDecimal amount;
    private String currency; // RUB, USD, EUR
    private LocalDate transaction_date;
    private Long category_id;

    @Override
    public String toString() {
        return String.format("Transaction{id=%s, title='%s', type='%s', amount=%s %s, date=%s}",
                id, title, category_id, amount, currency, transaction_date);
    }

    // == ГЕТТЕРЫ ==
    public Long getId() {return id;}
    public String getTitle() {return title;}
    public BigDecimal getAmount() {return amount;}
    public String getCurrency() {return currency;}
    public LocalDate getTransaction_date() {return transaction_date;}
    public Long getCategory_id() {return category_id;}

    // == СЕТТЕРЫ ==
    public void setId(Long id) {this.id = id;}
    public void setTitle(String title) {this.title = title;}
    public void setAmount(BigDecimal amount) {this.amount = amount;}
    public void setCurrency(String currency) {this.currency = currency;}
    public void setTransaction_date(LocalDate transaction_date) {this.transaction_date = transaction_date;}
    public void setCategory_id(Long category_id) {this.category_id = category_id;}
}
