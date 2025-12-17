package org.example.financetracker.service;

import org.example.financetracker.db.ExchangeRateDAO;
import org.example.financetracker.model.ExchangeRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExchangeRateService {
    private final ExchangeRateDAO exchangeRateDAO;
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);
    private static final String CB_API_URL = "https://www.cbr-xml-daily.ru/daily_json.js";

    public ExchangeRateService(ExchangeRateDAO exchangeRateDAO) {
        this.exchangeRateDAO = exchangeRateDAO;
    }

    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        // если основная валюта и валюта в транзакции рубль, то курс 1
        if ("RUB".equals(fromCurrency) && "RUB".equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        // конвертация в рубли
        if ("RUB".equals(toCurrency)) {
            BigDecimal directRate = getRateFromCB(fromCurrency);
            if (directRate != null) {
                ExchangeRate rate = new ExchangeRate();
                rate.setFrom_currency(fromCurrency);
                rate.setTo_currency("RUB");
                rate.setRate(directRate);
                rate.setLast_updated(LocalDateTime.now());
                exchangeRateDAO.saveRate(fromCurrency, "RUB", directRate);
                return directRate;
            }
        }

        // конвертация из рубля
        if ("RUB".equals(fromCurrency)) {
            BigDecimal toCurrencyRate = getRateFromCB(toCurrency); // 1 USD = X RUB
            if (toCurrencyRate != null && toCurrencyRate.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal reverseRate = BigDecimal.ONE.divide(toCurrencyRate, 6, RoundingMode.HALF_UP);
                exchangeRateDAO.saveRate("RUB", toCurrency, reverseRate);
                return reverseRate;
            }
        }

        // конвертация в других множествах пар
        BigDecimal fromToRub = getRateFromCB(fromCurrency);
        BigDecimal toToRub = getRateFromCB(toCurrency);
        if (fromToRub != null && toToRub != null && toToRub.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = fromToRub.divide(toToRub, 6, RoundingMode.HALF_UP);
            exchangeRateDAO.saveRate(fromCurrency, toCurrency, rate);
            return rate;
        }

        // fallback на БД
        ExchangeRate cached = exchangeRateDAO.getRate(fromCurrency, toCurrency);
        if (cached != null) {
            return cached.getRate();
        }

        throw new RuntimeException("Не удалось получить курс " + fromCurrency + " → " + toCurrency);
    }

    private BigDecimal getRateFromCB(String currency) {
        if ("RUB".equals(currency)) {
            return BigDecimal.ONE;
        }

        try {
            // берем из кэша за последние 24 часа
            ExchangeRate cached = exchangeRateDAO.getRate(currency, "RUB");
            if (cached != null && isRateFresh(cached)) {
                return cached.getRate();
            }

            // основной API запрос к центробанку
            URL url = new URL(CB_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // если код 200 (успешно)
            if (conn.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.toString());
                JsonNode valute = root.get("Valute");
                if (valute.has(currency)) {
                    BigDecimal rate = new BigDecimal(valute.get(currency).get("Value").asText())
                            .setScale(6, RoundingMode.HALF_UP);
                    return rate;
                } else {
                    log.warn("Валюта {} не найдена в ответе ЦБ", currency);
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при запросе к ЦБ для {}", currency, e);
        }
        return null;
    }

    // проверка на актуальность по времени
    private boolean isRateFresh(ExchangeRate rate) {
        if (rate.getLast_updated() == null) return false;
        long hours = java.time.Duration.between(rate.getLast_updated(), LocalDateTime.now()).toHours();
        return hours < 24;
    }

    public void refreshAllRates(String mainCurrency) {
        if (!"RUB".equals(mainCurrency)) {
            log.warn("Основная валюта не RUB — автообновление отключено");
            return;
        }

        List<String> currencies = Arrays.asList("USD", "EUR");
        for (String curr : currencies) {
            try {
                BigDecimal rubRate = getRateFromCB(curr); // 1 USD = X RUB
                if (rubRate != null) {
                    exchangeRateDAO.saveRate(curr, "RUB", rubRate);
                    BigDecimal reverse = BigDecimal.ONE.divide(rubRate, 6, RoundingMode.HALF_UP);
                    exchangeRateDAO.saveRate("RUB", curr, reverse);
                    log.info("Обновлён курс: {} → RUB = {}", curr, rubRate);
                }
            } catch (Exception e) {
                log.warn("Не удалось обновить курс для {}", curr, e);
            }
        }
    }
}