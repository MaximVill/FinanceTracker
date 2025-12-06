package org.example.financetracker.model;

public class AppSettings {
    private String mainCurrency = "RUB";

    // == ГЕТТЕРЫ И СЕТТЕРЫ ==
    public String getMainCurrency() {return mainCurrency;}
    public void setMainCurrency(String mainCurrency) {this.mainCurrency = mainCurrency;}
}
