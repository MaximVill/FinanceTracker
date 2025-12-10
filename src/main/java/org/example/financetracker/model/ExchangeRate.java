package org.example.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExchangeRate {
    private String from_currency;
    private String to_currency;
    private BigDecimal rate;
    private LocalDateTime last_updated;

    // == ГЕТТЕРЫ ==
    public String getFrom_currency() {return from_currency;}
    public String getTo_currency() {return to_currency;}
    public BigDecimal getRate() {return rate; }
    public LocalDateTime getLast_updated() {return last_updated;}

    // == СЕТТЕРЫ ==
    public void setFrom_currency(String from_currency) {this.from_currency = from_currency;}
    public void setTo_currency(String to_currency) {this.to_currency = to_currency;}
    public void setRate(BigDecimal rate) {this.rate = rate;}
    public void setLast_updated(LocalDateTime last_updated) {this.last_updated = last_updated;}
}
