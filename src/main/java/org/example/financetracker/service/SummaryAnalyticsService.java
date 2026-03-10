package org.example.financetracker.service;

import org.example.financetracker.db.SettingsDAO;
import org.example.financetracker.db.TransactionDAO;
import org.example.financetracker.model.Category;
import org.example.financetracker.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class SummaryAnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(SummaryAnalyticsService.class);

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();

    // ── Данные для вкладки КАПИТАЛ ─────────────────────────────────────────

    public record CapitalData(
            BigDecimal netWorth,
            BigDecimal assets,
            BigDecimal liabilities,
            BigDecimal monthChange,
            List<MonthPoint> history
    ) {}

    public record MonthPoint(YearMonth month, BigDecimal netWorth, BigDecimal assets, BigDecimal liabilities) {}

    public CapitalData getCapitalData() {
        List<Transaction> all = transactionDAO.findAll();

        BigDecimal assets = BigDecimal.ZERO;
        BigDecimal liabilities = BigDecimal.ZERO;

        for (Transaction t : all) {
            if (t.getCategory() != null && "income".equals(t.getCategory().getType())) {
                assets = assets.add(t.getAmount());
            } else {
                liabilities = liabilities.add(t.getAmount());
            }
        }

        BigDecimal netWorth = assets.subtract(liabilities);

        // История по месяцам
        Map<YearMonth, BigDecimal[]> byMonth = new TreeMap<>();
        for (Transaction t : all) {
            YearMonth ym = YearMonth.from(t.getTransaction_date());
            byMonth.computeIfAbsent(ym, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            boolean isIncome = t.getCategory() != null && "income".equals(t.getCategory().getType());
            if (isIncome) byMonth.get(ym)[0] = byMonth.get(ym)[0].add(t.getAmount());
            else byMonth.get(ym)[1] = byMonth.get(ym)[1].add(t.getAmount());
        }

        List<MonthPoint> history = new ArrayList<>();
        BigDecimal cumAssets = BigDecimal.ZERO, cumLiab = BigDecimal.ZERO;
        for (Map.Entry<YearMonth, BigDecimal[]> e : byMonth.entrySet()) {
            cumAssets = cumAssets.add(e.getValue()[0]);
            cumLiab = cumLiab.add(e.getValue()[1]);
            history.add(new MonthPoint(e.getKey(), cumAssets.subtract(cumLiab), cumAssets, cumLiab));
        }

        // Изменение за текущий месяц
        YearMonth currentMonth = YearMonth.now();
        BigDecimal monthChange = all.stream()
                .filter(t -> YearMonth.from(t.getTransaction_date()).equals(currentMonth))
                .map(t -> {
                    boolean isInc = t.getCategory() != null && "income".equals(t.getCategory().getType());
                    return isInc ? t.getAmount() : t.getAmount().negate();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CapitalData(netWorth, assets, liabilities, monthChange, history);
    }

    // ── Данные для вкладки ЗДОРОВЬЕ ───────────────────────────────────────

    public record HealthData(
            BigDecimal emergencyFund,
            BigDecimal targetEmergencyFund,
            double emergencyFundPct,
            double savingsRate,
            double debtRatio,
            double autonomyMonths,
            BigDecimal forecastNextMonth
    ) {}

    public HealthData getHealthData() {
        List<Transaction> all = transactionDAO.findAll();
        YearMonth now = YearMonth.now();
        YearMonth prev = now.minusMonths(1);

        // Месячные расходы (среднее за последние 3 месяца)
        BigDecimal totalExpense3m = BigDecimal.ZERO;
        BigDecimal totalIncome3m = BigDecimal.ZERO;
        int monthsCount = 0;

        for (int i = 1; i <= 3; i++) {
            YearMonth ym = now.minusMonths(i);
            BigDecimal inc = BigDecimal.ZERO, exp = BigDecimal.ZERO;
            for (Transaction t : all) {
                if (!YearMonth.from(t.getTransaction_date()).equals(ym)) continue;
                if (t.getCategory() != null && "income".equals(t.getCategory().getType()))
                    inc = inc.add(t.getAmount());
                else exp = exp.add(t.getAmount());
            }
            if (inc.compareTo(BigDecimal.ZERO) > 0 || exp.compareTo(BigDecimal.ZERO) > 0) {
                totalIncome3m = totalIncome3m.add(inc);
                totalExpense3m = totalExpense3m.add(exp);
                monthsCount++;
            }
        }

        BigDecimal avgMonthlyExpense = monthsCount > 0
                ? totalExpense3m.divide(new BigDecimal(monthsCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(5000);

        BigDecimal avgMonthlyIncome = monthsCount > 0
                ? totalIncome3m.divide(new BigDecimal(monthsCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Общий накопленный баланс (активы - обязательства)
        BigDecimal totalAssets = BigDecimal.ZERO;
        for (Transaction t : all) {
            if (t.getCategory() != null && "income".equals(t.getCategory().getType()))
                totalAssets = totalAssets.add(t.getAmount());
            else
                totalAssets = totalAssets.subtract(t.getAmount());
        }
        BigDecimal emergencyFund = totalAssets.max(BigDecimal.ZERO);

        // Подушка = 3 месяца расходов
        BigDecimal target = avgMonthlyExpense.multiply(new BigDecimal("3"));
        double emergencyPct = target.compareTo(BigDecimal.ZERO) > 0
                ? emergencyFund.divide(target, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        // Норма сбережений текущего месяца
        BigDecimal currIncome = BigDecimal.ZERO, currExpense = BigDecimal.ZERO;
        for (Transaction t : all) {
            if (!YearMonth.from(t.getTransaction_date()).equals(now)) continue;
            if (t.getCategory() != null && "income".equals(t.getCategory().getType()))
                currIncome = currIncome.add(t.getAmount());
            else currExpense = currExpense.add(t.getAmount());
        }
        double savingsRate = currIncome.compareTo(BigDecimal.ZERO) > 0
                ? currIncome.subtract(currExpense).divide(currIncome, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        // Долговая нагрузка
        BigDecimal debtPayments = BigDecimal.ZERO;
        for (Transaction t : all) {
            if (!YearMonth.from(t.getTransaction_date()).equals(now)) continue;
            if (t.getCategory() != null && "Долги".equals(t.getCategory().getName()))
                debtPayments = debtPayments.add(t.getAmount());
        }
        double debtRatio = currIncome.compareTo(BigDecimal.ZERO) > 0
                ? debtPayments.divide(currIncome, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        // Автономность (месяцев без дохода)
        double autonomyMonths = avgMonthlyExpense.compareTo(BigDecimal.ZERO) > 0
                ? emergencyFund.divide(avgMonthlyExpense, 4, RoundingMode.HALF_UP).doubleValue()
                : 0;

        // Прогноз на следующий месяц
        BigDecimal forecast = emergencyFund.add(avgMonthlyIncome).subtract(avgMonthlyExpense);

        return new HealthData(emergencyFund, target, emergencyPct,
                Math.max(savingsRate, 0), Math.max(debtRatio, 0),
                Math.max(autonomyMonths, 0), forecast);
    }

    // ── Данные для вкладки СОВЕТЫ ─────────────────────────────────────────

    public record TipData(String title, String tip, List<InsightItem> insights) {}
    public record InsightItem(String icon, String title, String description, boolean isPositive) {}

    public TipData getTips() {
        HealthData h = getHealthData();
        List<InsightItem> insights = new ArrayList<>();

        // Автономность
        String autoDesc = String.format("Денег хватит на %.0f мес. жизни без новых доходов.", h.autonomyMonths());
        insights.add(new InsightItem("🛡", "Автономность", autoDesc, h.autonomyMonths() >= 3));

        // Сбережения
        if (h.savingsRate() > 10) {
            insights.add(new InsightItem("✅", "Норма сбережений",
                    String.format("Откладываешь %.0f%% дохода — отличный результат!", h.savingsRate()), true));
        } else if (h.savingsRate() > 0) {
            insights.add(new InsightItem("⚠", "Норма сбережений",
                    String.format("Откладываешь только %.0f%% — цель: минимум 10%%.", h.savingsRate()), false));
        }

        // Долги
        if (h.debtRatio() > 30) {
            insights.add(new InsightItem("🎯", "Долговая нагрузка",
                    String.format("%.0f%% дохода уходит на долги — это слишком много.", h.debtRatio()), false));
        }

        // Подушка безопасности
        if (h.emergencyFundPct() >= 100) {
            insights.add(new InsightItem("✅", "Подушка безопасности",
                    String.format("Подушка сформирована на %.0f%%! Продолжай в том же духе.", h.emergencyFundPct()), true));
        }

        // Достигнутые цели
        List<GoalProgress> goals = getGoalProgress();
        for (GoalProgress g : goals) {
            if (g.progressPct() >= 100) {
                insights.add(new InsightItem("🎯", "Цель: " + g.name(),
                        "Цель полностью профинансирована! Нажмите на галочку для завершения.", true));
            }
        }

        String mainTip;
        String mainTitle;
        if (h.savingsRate() < 10) {
            mainTitle = "СНАЧАЛА ЗАПЛАТИ СЕБЕ";
            mainTip = "\"Откладывай минимум 10% от любого дохода сразу после его получения, прежде чем начнешь тратить на остальное.\"";
        } else if (h.debtRatio() > 30) {
            mainTitle = "ПРАВИЛО ЛАВИНЫ ДОЛГОВ";
            mainTip = "\"Погашай сначала долг с самой высокой ставкой — это сэкономит максимум денег.\"";
        } else {
            mainTitle = "ПРАВИЛО 50/30/20";
            mainTip = "\"50% на нужды, 30% на желания, 20% на накопления — твой идеальный бюджет.\"";
        }

        return new TipData(mainTitle, mainTip, insights);
    }

    // ── Данные для вкладки СТРАТЕГИИ ─────────────────────────────────────

    public record StrategyData(
            BigDecimal currentMonthlyIncome,
            BigDecimal currentMonthlyExpense,
            List<CategoryExpense> topExpenses
    ) {}

    public record CategoryExpense(String name, String icon, BigDecimal avgMonthly, BigDecimal optimized) {}

    public StrategyData getStrategyData() {
        List<Transaction> all = transactionDAO.findAll();
        YearMonth now = YearMonth.now();

        // Доходы текущего месяца
        BigDecimal currIncome = BigDecimal.ZERO;
        for (Transaction t : all) {
            if (YearMonth.from(t.getTransaction_date()).equals(now)
                    && t.getCategory() != null && "income".equals(t.getCategory().getType()))
                currIncome = currIncome.add(t.getAmount());
        }

        // Расходы по категориям за последние 3 месяца
        Map<String, BigDecimal> expByCategory = new LinkedHashMap<>();
        Map<String, Integer> monthsWithData = new HashMap<>();

        for (Transaction t : all) {
            YearMonth ym = YearMonth.from(t.getTransaction_date());
            if (ym.isBefore(now.minusMonths(3))) continue;
            if (t.getCategory() == null || "income".equals(t.getCategory().getType())) continue;
            String cat = t.getCategory().getName();
            expByCategory.merge(cat, t.getAmount(), BigDecimal::add);
            monthsWithData.merge(cat, 1, Integer::sum);
        }

        // Средние по категориям
        List<CategoryExpense> top = new ArrayList<>();
        String[] icons = {"🚌", "🎮", "🎁", "🍕", "📱", "🏠", "👗", "💊"};
        int i = 0;
        for (Map.Entry<String, BigDecimal> e : expByCategory.entrySet()
                .stream().sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList())) {
            int months = monthsWithData.getOrDefault(e.getKey(), 1);
            BigDecimal avg = e.getValue().divide(new BigDecimal(Math.max(months, 1)), 2, RoundingMode.HALF_UP);
            String icon = i < icons.length ? icons[i++] : "💰";
            top.add(new CategoryExpense(e.getKey(), icon, avg, avg.multiply(new BigDecimal("0.8")).setScale(2, RoundingMode.HALF_UP)));
        }

        BigDecimal currExpense = expByCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StrategyData(currIncome, currExpense, top.stream().limit(5).collect(Collectors.toList()));
    }

    // ── Данные для вкладки ИСТОРИЯ ────────────────────────────────────────

    public record HistoryData(
            YearMonth month,
            BigDecimal forecastIncome,
            BigDecimal forecastExpense,
            BigDecimal actualIncome,
            BigDecimal actualExpense,
            String topIncomeSrc,
            BigDecimal topIncomeAmt,
            String topExpenseSrc,
            BigDecimal topExpenseAmt,
            List<DayPoint> dailyPoints,
            List<CategoryShare> expenseShares,
            List<CategoryShare> incomeShares
    ) {}

    public record DayPoint(int day, BigDecimal income, BigDecimal expense) {}
    public record CategoryShare(String name, BigDecimal amount, String color) {}

    public HistoryData getHistoryData(YearMonth month) {
        List<Transaction> all = transactionDAO.findAll();

        BigDecimal actualIncome = BigDecimal.ZERO, actualExpense = BigDecimal.ZERO;
        Map<Integer, BigDecimal[]> daily = new TreeMap<>();
        Map<String, BigDecimal> expCats = new LinkedHashMap<>();
        Map<String, BigDecimal> incCats = new LinkedHashMap<>();

        String topIncomeSrc = "—"; BigDecimal topIncomeAmt = BigDecimal.ZERO;
        String topExpenseSrc = "—"; BigDecimal topExpenseAmt = BigDecimal.ZERO;

        for (Transaction t : all) {
            if (!YearMonth.from(t.getTransaction_date()).equals(month)) continue;
            boolean isInc = t.getCategory() != null && "income".equals(t.getCategory().getType());
            int day = t.getTransaction_date().getDayOfMonth();
            daily.computeIfAbsent(day, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});

            if (isInc) {
                actualIncome = actualIncome.add(t.getAmount());
                daily.get(day)[0] = daily.get(day)[0].add(t.getAmount());
                String cat = t.getCategory() != null ? t.getCategory().getName() : t.getTitle();
                incCats.merge(cat, t.getAmount(), BigDecimal::add);
                if (t.getAmount().compareTo(topIncomeAmt) > 0) {
                    topIncomeAmt = t.getAmount(); topIncomeSrc = t.getTitle();
                }
            } else {
                actualExpense = actualExpense.add(t.getAmount());
                daily.get(day)[1] = daily.get(day)[1].add(t.getAmount());
                String cat = t.getCategory() != null ? t.getCategory().getName() : t.getTitle();
                expCats.merge(cat, t.getAmount(), BigDecimal::add);
                if (t.getAmount().compareTo(topExpenseAmt) > 0) {
                    topExpenseAmt = t.getAmount(); topExpenseSrc = t.getCategory() != null ? t.getCategory().getName() : t.getTitle();
                }
            }
        }

        // Прогноз (из предыдущих месяцев)
        BigDecimal forecastIncome = BigDecimal.ZERO, forecastExpense = BigDecimal.ZERO;
        int fc = 0;
        for (int i = 1; i <= 3; i++) {
            YearMonth prev = month.minusMonths(i);
            BigDecimal pi = BigDecimal.ZERO, pe = BigDecimal.ZERO;
            for (Transaction t : all) {
                if (!YearMonth.from(t.getTransaction_date()).equals(prev)) continue;
                if (t.getCategory() != null && "income".equals(t.getCategory().getType()))
                    pi = pi.add(t.getAmount());
                else pe = pe.add(t.getAmount());
            }
            if (pi.compareTo(BigDecimal.ZERO) > 0 || pe.compareTo(BigDecimal.ZERO) > 0) {
                forecastIncome = forecastIncome.add(pi);
                forecastExpense = forecastExpense.add(pe);
                fc++;
            }
        }
        if (fc > 0) {
            forecastIncome = forecastIncome.divide(new BigDecimal(fc), 2, RoundingMode.HALF_UP);
            forecastExpense = forecastExpense.divide(new BigDecimal(fc), 2, RoundingMode.HALF_UP);
        }

        // Daily points
        List<DayPoint> days = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal[]> e : daily.entrySet())
            days.add(new DayPoint(e.getKey(), e.getValue()[0], e.getValue()[1]));

        // Category shares
        String[] colors = {"#4a56e2","#27ae60","#e67e22","#e74c3c","#9b59b6","#1abc9c","#f39c12","#2ecc71"};
        List<CategoryShare> expShares = new ArrayList<>();
        int ci = 0;
        for (Map.Entry<String, BigDecimal> e : expCats.entrySet()
                .stream().sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList()))
            expShares.add(new CategoryShare(e.getKey(), e.getValue(), colors[ci++ % colors.length]));

        List<CategoryShare> incShares = new ArrayList<>();
        ci = 0;
        for (Map.Entry<String, BigDecimal> e : incCats.entrySet()
                .stream().sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList()))
            incShares.add(new CategoryShare(e.getKey(), e.getValue(), colors[ci++ % colors.length]));

        return new HistoryData(month, forecastIncome, forecastExpense,
                actualIncome, actualExpense, topIncomeSrc, topIncomeAmt,
                topExpenseSrc, topExpenseAmt, days, expShares, incShares);
    }

    // ── Вспомогательные ──────────────────────────────────────────────────

    public record GoalProgress(String name, BigDecimal target, BigDecimal current, double progressPct) {}

    public List<GoalProgress> getGoalProgress() {
        // Заглушка — расширяется когда появится таблица целей
        return new ArrayList<>();
    }
}
