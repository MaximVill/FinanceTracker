package org.example.financetracker.service;

import org.example.financetracker.db.ExchangeRateDAO;
import org.example.financetracker.model.ExchangeRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;




public class ExchangeRateService {
    private final ExchangeRateDAO exchangeRateDAO;
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    public ExchangeRateService(ExchangeRateDAO exchangeRateDAO) {
        this.exchangeRateDAO = exchangeRateDAO;
    }

    public ExchangeRateDAO getExchangeRateDAO() {
        return exchangeRateDAO;
    }

    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        // Сначала попробуем взять из БД (кэш)
        ExchangeRate rate = exchangeRateDAO.getRate(fromCurrency, toCurrency);
        if (rate != null && isRateFresh(rate)) {
            return rate.getRate();
        }

        // Если нет или устарел — запросим через API
        try {
            BigDecimal newRate = fetchRateFromAPI(fromCurrency, toCurrency);
            exchangeRateDAO.saveRate(fromCurrency, toCurrency, newRate);
            return newRate;
        } catch (Exception e) {
            // Fallback на последний известный курс
            if (rate != null) {
                return rate.getRate();
            }
            throw new RuntimeException("Cannot fetch exchange rate", e);
        }
    }

    private boolean isRateFresh(ExchangeRate rate) {
        long hoursSinceUpdate = (System.currentTimeMillis() - rate.getLast_updated().getHour()) / (1000 * 60 * 60);
        return hoursSinceUpdate < 24; // Обновляем каждые 24 часа
    }

    private BigDecimal fetchRateFromAPI(String fromCurrency, String toCurrency) throws Exception {
        URL url = new URL("https://api.exchangerate.host/latest?base=" + fromCurrency + "&symbols=" + toCurrency);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            Scanner scanner = new Scanner(connection.getInputStream()).useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            // Парсим JSON (упрощённо — можно использовать Jackson или Gson)
            // Пример ответа: {"rates":{"USD":1.0}}
            int start = response.indexOf("\"" + toCurrency + "\":") + toCurrency.length() + 3;
            int end = response.indexOf(",", start);
            if (end == -1) end = response.indexOf("}", start);

            String rateStr = response.substring(start, end);
            return new BigDecimal(rateStr).setScale(6, RoundingMode.HALF_UP);
        } else {
            throw new RuntimeException("API returned error code: " + responseCode);
        }
    }
    public void refreshAllRates(String mainCurrency) {
        List<String> currencies = Arrays.asList("RUB", "USD", "EUR");

        for (String fromCurrency : currencies) {
            if (fromCurrency.equals(mainCurrency)) continue;

            try {
                BigDecimal rate = fetchRateFromAPI(fromCurrency, mainCurrency);
                exchangeRateDAO.saveRate(fromCurrency, mainCurrency, rate);
                log.info("Обновлён курс: {} -> {} = {}", fromCurrency, mainCurrency, rate);

                // Также сохраняем обратный курс для оптимизации
                BigDecimal reverseRate = BigDecimal.ONE.divide(rate, 6, RoundingMode.HALF_UP);
                exchangeRateDAO.saveRate(mainCurrency, fromCurrency, reverseRate);

            } catch (Exception e) {
                log.warn("Не удалось обновить курс {} -> {}: {}",
                        fromCurrency, mainCurrency, e.getMessage());
            }
        }

        log.info("Все курсы обновлены относительно {}", mainCurrency);
    }
}