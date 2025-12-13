package org.example.financetracker.model;

public class UserSettings {
    private String userName;
    private String mainCurrency;
    private boolean firstLaunch = true;

    // Конструкторы
    public UserSettings() {}

    public UserSettings(String userName, String mainCurrency) {
        this.userName = userName;
        this.mainCurrency = mainCurrency;
        this.firstLaunch = false;
    }

    // Геттеры и сеттеры
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getMainCurrency() { return mainCurrency; }
    public void setMainCurrency(String mainCurrency) { this.mainCurrency = mainCurrency; }

    public boolean isFirstLaunch() { return firstLaunch; }
    public void setFirstLaunch(boolean firstLaunch) { this.firstLaunch = firstLaunch; }
}
