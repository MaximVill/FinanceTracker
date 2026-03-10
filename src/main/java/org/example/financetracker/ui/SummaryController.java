package org.example.financetracker.ui;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.example.financetracker.service.SummaryAnalyticsService;
import org.example.financetracker.service.SummaryAnalyticsService.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class SummaryController implements AppLayoutAware {
    private static final Logger log = LoggerFactory.getLogger(SummaryController.class);

    @FXML private HBox tabBar;
    @FXML private StackPane contentPane;

    private AppLayoutController appLayout;
    private final SummaryAnalyticsService analytics = new SummaryAnalyticsService();

    private static final NumberFormat FMT = NumberFormat.getInstance(new Locale("ru", "RU"));
    static { FMT.setMinimumFractionDigits(2); FMT.setMaximumFractionDigits(2); }

    private static final String[] TAB_NAMES = {"Капитал", "Здоровье", "Советы", "Стратегии", "История"};
    private static final String[] TAB_ICONS = {"⚖", "✦", "💡", "⚗", "📅"};
    private int currentTab = 0;
    private YearMonth historyMonth = YearMonth.now();

    @FXML
    private void initialize() {
        buildTabBar();
        showTab(0);
    }

    @Override
    public void setAppLayout(AppLayoutController appLayout) {
        this.appLayout = appLayout;
    }

    // ── Таб-бар ────────────────────────────────────────────────────────────

    private void buildTabBar() {
        tabBar.getChildren().clear();
        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int idx = i;
            Button btn = new Button(TAB_ICONS[i] + "  " + TAB_NAMES[i]);
            btn.setOnAction(e -> showTab(idx));
            styleTabBtn(btn, i == currentTab);
            tabBar.getChildren().add(btn);
        }
    }

    private void styleTabBtn(Button btn, boolean active) {
        if (active) {
            btn.setStyle("-fx-background-color: #4a56e2; -fx-text-fill: white; " +
                    "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 20; " +
                    "-fx-padding: 7 18; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c6c8a; " +
                    "-fx-font-size: 13; -fx-background-radius: 20; " +
                    "-fx-padding: 7 18; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle(
                    "-fx-background-color: #ededff; -fx-text-fill: #4a56e2; " +
                    "-fx-font-size: 13; -fx-background-radius: 20; -fx-padding: 7 18; -fx-cursor: hand;"));
            btn.setOnMouseExited(e -> styleTabBtn(btn, false));
        }
    }

    private void showTab(int idx) {
        currentTab = idx;
        buildTabBar();
        try {
            switch (idx) {
                case 0 -> contentPane.getChildren().setAll(buildCapitalTab());
                case 1 -> contentPane.getChildren().setAll(buildHealthTab());
                case 2 -> contentPane.getChildren().setAll(buildTipsTab());
                case 3 -> contentPane.getChildren().setAll(buildStrategyTab());
                case 4 -> contentPane.getChildren().setAll(buildHistoryTab());
            }
        } catch (Exception e) {
            log.error("Ошибка загрузки вкладки {}", idx, e);
            Label err = new Label("Ошибка загрузки: " + e.getMessage());
            err.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13; -fx-padding: 20;");
            contentPane.getChildren().setAll(err);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── ВКЛАДКА 1: КАПИТАЛ ───────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════

    private ScrollPane buildCapitalTab() {
        CapitalData data = analytics.getCapitalData();

        HBox root = new HBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #f4f4fb;");

        // Левая колонка — карточки
        VBox left = new VBox(14);
        left.setMinWidth(280);
        left.setMaxWidth(300);

        // Карточка: Чистый капитал
        VBox netCard = card();
        Label netLbl = smallCap("ЧИСТЫЙ КАПИТАЛ");
        Label netVal = bigNum(fmt(data.netWorth()) + " ₽", "#2a2aaa");
        String chgSign = data.monthChange().compareTo(BigDecimal.ZERO) >= 0 ? "↗ +" : "↘ ";
        String chgColor = data.monthChange().compareTo(BigDecimal.ZERO) >= 0 ? "#27ae60" : "#e74c3c";
        Label netChg = new Label(chgSign + fmt(data.monthChange()) + " ₽ /мес");
        netChg.setStyle("-fx-font-size: 13; -fx-text-fill: " + chgColor + "; " +
                "-fx-background-color: " + (data.monthChange().compareTo(BigDecimal.ZERO) >= 0 ? "#e8f8ef" : "#fdecea") + "; " +
                "-fx-background-radius: 8; -fx-padding: 4 10;");
        netCard.getChildren().addAll(netLbl, netVal, netChg);

        // Карточка: Активы
        VBox assetsCard = card();
        HBox aRow = dotRow("#4a56e2", "АКТИВЫ");
        Label assetsVal = midNum(fmt(data.assets()) + " ₽");
        assetsCard.getChildren().addAll(aRow, assetsVal);

        // Карточка: Обязательства
        VBox liabCard = card();
        HBox lRow = dotRow("#e74c3c", "ОБЯЗАТЕЛЬСТВА");
        Label liabVal = midNum(fmt(data.liabilities()) + " ₽");
        liabVal.setStyle(liabVal.getStyle() + "-fx-text-fill: #e74c3c;");
        liabCard.getChildren().addAll(lRow, liabVal);

        left.getChildren().addAll(netCard, assetsCard, liabCard);

        // Правая колонка — график
        VBox right = new VBox(12);
        HBox.setHgrow(right, Priority.ALWAYS);

        // Легенда
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.setPadding(new Insets(0, 16, 0, 0));
        legend.getChildren().addAll(
                legendItem("#4a56e2", "Твои настоящие деньги"),
                legendItem("#27ae60", "Всё твоё"),
                legendItem("#e74c3c", "Твои долги")
        );

        // Canvas-график
        Canvas chart = new Canvas(750, 400);
        drawCapitalChart(chart, data.history());

        right.getChildren().addAll(legend, chart);
        root.getChildren().addAll(left, right);

        ScrollPane sp = scrollPane();
        sp.setContent(root);
        return sp;
    }

    private void drawCapitalChart(Canvas canvas, List<MonthPoint> history) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        double padL = 80, padR = 20, padT = 20, padB = 50;
        double plotW = w - padL - padR, plotH = h - padT - padB;

        gc.setFill(Color.WHITE);
        gc.fillRoundRect(0, 0, w, h, 16, 16);

        if (history.isEmpty()) {
            gc.setFill(Color.web("#9090a8"));
            gc.setFont(Font.font(14));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Нет данных для отображения", w / 2, h / 2);
            return;
        }

        // Диапазон
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        for (MonthPoint p : history) {
            minY = Math.min(minY, Math.min(p.netWorth().doubleValue(),
                    Math.min(p.assets().doubleValue(), -p.liabilities().doubleValue())));
            maxY = Math.max(maxY, Math.max(p.netWorth().doubleValue(),
                    Math.max(p.assets().doubleValue(), p.liabilities().doubleValue())));
        }
        double range = maxY - minY;
        if (range == 0) range = 1000;
        minY -= range * 0.1;
        maxY += range * 0.1;

        // Сетка
        gc.setStroke(Color.web("#e8e8f0"));
        gc.setLineWidth(1);
        int gridLines = 5;
        gc.setFill(Color.web("#9090a8"));
        gc.setFont(Font.font(11));
        gc.setTextAlign(TextAlignment.RIGHT);
        for (int i = 0; i <= gridLines; i++) {
            double yVal = minY + (maxY - minY) * i / gridLines;
            double y = padT + plotH - (yVal - minY) / (maxY - minY) * plotH;
            gc.strokeLine(padL, y, padL + plotW, y);
            gc.fillText(formatShort(yVal) + " ₽", padL - 6, y + 4);
        }

        int n = history.size();
        double step = plotW / Math.max(n - 1, 1);

        // Подписи оси X
        gc.setFill(Color.web("#9090a8"));
        gc.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < n; i++) {
            double x = padL + i * step;
            String lbl = history.get(i).month().getMonth()
                    .getDisplayName(TextStyle.SHORT, new Locale("ru")) + " "
                    + String.valueOf(history.get(i).month().getYear()).substring(2);
            gc.fillText(lbl, x, h - 10);
        }

        // Линии
        drawLine(gc, history, padL, padT, plotH, step, minY, maxY,
                p -> p.assets().doubleValue(), "#27ae60");
        drawLine(gc, history, padL, padT, plotH, step, minY, maxY,
                p -> p.liabilities().doubleValue(), "#e74c3c");
        drawLine(gc, history, padL, padT, plotH, step, minY, maxY,
                p -> p.netWorth().doubleValue(), "#4a56e2");

        // Точки последней записи
        if (!history.isEmpty()) {
            MonthPoint last = history.get(history.size() - 1);
            double lx = padL + (n - 1) * step;
            drawDot(gc, lx, toY(last.netWorth().doubleValue(), padT, plotH, minY, maxY), "#4a56e2");
            drawDot(gc, lx, toY(last.assets().doubleValue(), padT, plotH, minY, maxY), "#27ae60");
            drawDot(gc, lx, toY(last.liabilities().doubleValue(), padT, plotH, minY, maxY), "#e74c3c");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── ВКЛАДКА 2: ЗДОРОВЬЕ ──────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════

    private ScrollPane buildHealthTab() {
        HealthData data = analytics.getHealthData();

        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #f4f4fb;");

        // Верхняя строка — 4 метрики
        HBox metrics = new HBox(14);

        // 1. Запас (подушка)
        VBox cushionCard = card();
        cushionCard.setMinWidth(220);
        cushionCard.setMaxWidth(280);
        HBox.setHgrow(cushionCard, Priority.ALWAYS);
        Label cTitle = smallCapWithIcon("🛡", "ЗАПАС (ПОДУШКА)");
        ProgressBar pb = new ProgressBar(Math.min(data.emergencyFundPct() / 100.0, 1.0));
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setStyle("-fx-accent: #4a56e2;");
        Label cPct = new Label(String.format("%.0f%% ОТ НУЖНОЙ СУММЫ", data.emergencyFundPct()));
        cPct.setStyle("-fx-font-size: 10; -fx-text-fill: #9090a8;");
        Label cVal = midNum(fmt(data.emergencyFund()) + " ₽");
        Label cSub = new Label("ТВОИ СВОБОДНЫЕ ДЕНЬГИ");
        cSub.setStyle("-fx-font-size: 10; -fx-text-fill: #9090a8;");
        cushionCard.getChildren().addAll(cTitle, pb, cPct, cVal, cSub);

        // 2. Норма сбережений
        VBox savCard = card();
        HBox.setHgrow(savCard, Priority.ALWAYS);
        Label sTitle = smallCapWithIcon("💰", "СКОЛЬКО КОПИШЬ");
        Label sPct = bigNum(String.format("%.0f%%", data.savingsRate()),
                data.savingsRate() >= 20 ? "#27ae60" : data.savingsRate() >= 10 ? "#e67e22" : "#e74c3c");
        Label sSub = new Label("ОТКЛАДЫВАЕШЬ В МЕСЯЦ");
        sSub.setStyle("-fx-font-size: 10; -fx-text-fill: #9090a8;");
        String savMood = data.savingsRate() >= 20 ? "✅ МОЛОДЕЦ!" : data.savingsRate() >= 10 ? "⚠ МОЖНО ЛУЧШЕ" : "❌ МАЛО";
        Label sMood = badge(savMood, data.savingsRate() >= 10 ? "#27ae60" : "#e74c3c");
        savCard.getChildren().addAll(sTitle, sPct, sSub, sMood);

        // 3. Тяжесть долгов
        VBox debtCard = card();
        HBox.setHgrow(debtCard, Priority.ALWAYS);
        Label dTitle = smallCapWithIcon("⚖", "ТЯЖЕСТЬ ДОЛГОВ");
        Label dPct = bigNum(String.format("%.0f%%", data.debtRatio()),
                data.debtRatio() <= 15 ? "#27ae60" : data.debtRatio() <= 30 ? "#e67e22" : "#e74c3c");
        Label dSub = new Label("УХОДИТ НА ДОЛГИ");
        dSub.setStyle("-fx-font-size: 10; -fx-text-fill: #9090a8;");
        String debtMood = data.debtRatio() == 0 ? "✅ ЧИСТ" : data.debtRatio() <= 15 ? "✅ НОРМА" : "⚠ СЛИШКОМ МНОГО";
        Label dMood = badge(debtMood, data.debtRatio() <= 15 ? "#27ae60" : "#e74c3c");
        debtCard.getChildren().addAll(dTitle, dPct, dSub, dMood);

        // 4. Можно не работать
        VBox autoCard = card();
        HBox.setHgrow(autoCard, Priority.ALWAYS);
        Label aTitle = smallCapWithIcon("🗂", "МОЖНО НЕ РАБОТАТЬ");
        Label aVal = bigNum(String.format("%.0f мес.", data.autonomyMonths()),
                data.autonomyMonths() >= 6 ? "#4a56e2" : data.autonomyMonths() >= 3 ? "#e67e22" : "#e74c3c");
        Label aSub = new Label("ТВОЙ ЗАПАС ВРЕМЕНИ");
        aSub.setStyle("-fx-font-size: 10; -fx-text-fill: #9090a8;");
        Label aSub2 = new Label("ПРИ ТВОИХ ТРАТАХ");
        aSub2.setStyle("-fx-font-size: 10; -fx-text-fill: #c0c0d0;");
        autoCard.getChildren().addAll(aTitle, aVal, aSub, aSub2);

        metrics.getChildren().addAll(cushionCard, savCard, debtCard, autoCard);

        // Прогноз на следующий месяц
        VBox forecastCard = card();
        Label fTitle = new Label("→  Что будет через месяц?");
        fTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #4a56e2;");
        Label fSub = new Label("Примерный прогноз твоих денег на конец месяца на основе темпа трат.");
        fSub.setStyle("-fx-font-size: 13; -fx-text-fill: #6c6c8a; -fx-wrap-text: true;");
        fSub.setWrapText(true);

        BigDecimal forecast = data.forecastNextMonth();
        BigDecimal diff = forecast.subtract(data.emergencyFund());
        String diffStr = (diff.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + fmt(diff) + " ₽";
        String diffColor = diff.compareTo(BigDecimal.ZERO) >= 0 ? "#27ae60" : "#e74c3c";
        HBox fRow = new HBox(12);
        fRow.setAlignment(Pos.BASELINE_LEFT);
        Label fVal = new Label(fmt(forecast) + " ₽");
        fVal.setStyle("-fx-font-size: 36; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label fDiff = new Label(diffStr);
        fDiff.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: " + diffColor + ";");
        fRow.getChildren().addAll(fVal, fDiff);
        forecastCard.getChildren().addAll(fTitle, fSub, fRow);

        root.getChildren().addAll(metrics, forecastCard);

        ScrollPane sp = scrollPane();
        sp.setContent(root);
        return sp;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── ВКЛАДКА 3: СОВЕТЫ ────────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════

    private ScrollPane buildTipsTab() {
        TipData data = analytics.getTips();

        VBox root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #f4f4fb;");

        // Главный совет
        HBox mainTip = new HBox(16);
        mainTip.setAlignment(Pos.CENTER_LEFT);
        mainTip.setStyle("-fx-background-color: #edeeff; -fx-background-radius: 16; -fx-padding: 20 24;");
        VBox tipText = new VBox(6);
        HBox.setHgrow(tipText, Priority.ALWAYS);
        Label tipTitle = new Label("💡 " + data.title());
        tipTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #4a56e2;");
        Label tipBody = new Label(data.tip());
        tipBody.setStyle("-fx-font-size: 14; -fx-text-fill: #2a2a5a; -fx-font-style: italic;");
        tipBody.setWrapText(true);
        tipText.getChildren().addAll(tipTitle, tipBody);
        Label starIcon = new Label("✦");
        starIcon.setStyle("-fx-font-size: 48; -fx-text-fill: #c8ccff; -fx-opacity: 0.6;");
        mainTip.getChildren().addAll(tipText, starIcon);

        root.getChildren().add(mainTip);

        // Инсайты сгруппированные
        if (!data.insights().isEmpty()) {
            // Группа "Ваш кошелёк"
            Label walletLbl = sectionLabel("ВАШ КОШЕЛЁК");
            root.getChildren().add(walletLbl);

            for (InsightItem item : data.insights()) {
                HBox row = new HBox(14);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16 20;");
                Label icon = new Label(item.icon());
                icon.setStyle("-fx-font-size: 20; -fx-background-color: " +
                        (item.isPositive() ? "#e8f8ef" : "#fdecea") +
                        "; -fx-background-radius: 10; -fx-padding: 8 10;");
                VBox info = new VBox(3);
                Label iTitle = new Label(item.title());
                iTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
                Label iDesc = new Label(item.description());
                iDesc.setStyle("-fx-font-size: 12; -fx-text-fill: #6c6c8a;");
                iDesc.setWrapText(true);
                info.getChildren().addAll(iTitle, iDesc);
                row.getChildren().addAll(icon, info);

                // Если это аналитика по целям — отдельный раздел
                if (item.title().startsWith("Цель:")) {
                    if (root.getChildren().stream().noneMatch(n ->
                            n instanceof Label l && "АНАЛИТИКА".equals(l.getText()))) {
                        root.getChildren().add(sectionLabel("АНАЛИТИКА"));
                    }
                }
                root.getChildren().add(row);
            }
        } else {
            Label noData = new Label("Добавьте транзакции, чтобы увидеть персональные советы.");
            noData.setStyle("-fx-font-size: 14; -fx-text-fill: #9090a8; -fx-padding: 20 0;");
            root.getChildren().add(noData);
        }

        ScrollPane sp = scrollPane();
        sp.setContent(root);
        return sp;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── ВКЛАДКА 4: СТРАТЕГИИ ─────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════

    private ScrollPane buildStrategyTab() {
        StrategyData data = analytics.getStrategyData();

        HBox root = new HBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #f4f4fb;");

        // Левая — настройка модели
        VBox left = new VBox(16);
        left.setMinWidth(400);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label leftTitle = new Label("Настройка модели");
        leftTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        left.getChildren().add(leftTitle);

        // Текущий доход
        VBox incCard = new VBox(4);
        incCard.setStyle("-fx-background-color: #f0fff4; -fx-background-radius: 14; -fx-padding: 16 20;");
        Label incSub = new Label("ТЕКУЩИЙ ДОХОД");
        incSub.setStyle("-fx-font-size: 10; -fx-text-fill: #27ae60;");
        Label incVal = new Label(fmt(data.currentMonthlyIncome()) + " ₽/мес.");
        incVal.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        incCard.getChildren().addAll(incSub, incVal);
        left.getChildren().add(incCard);

        // Оптимизация расходов
        Label optTitle = smallCap("ОПТИМИЗАЦИЯ РАСХОДОВ");
        left.getChildren().add(optTitle);

        if (data.topExpenses().isEmpty()) {
            Label noExp = new Label("Нет данных по расходам за последние 3 месяца.");
            noExp.setStyle("-fx-font-size: 13; -fx-text-fill: #9090a8;");
            left.getChildren().add(noExp);
        } else {
            // Слайдеры расходов
            final BigDecimal[] totalSaved = {BigDecimal.ZERO};
            for (CategoryExpense ce : data.topExpenses()) {
                VBox expRow = new VBox(6);
                expRow.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 14 16;");
                HBox topRow = new HBox(10);
                topRow.setAlignment(Pos.CENTER_LEFT);
                Label icon = new Label(ce.icon());
                icon.setStyle("-fx-font-size: 22;");
                VBox info = new VBox(2);
                Label name = new Label(ce.name());
                name.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
                Label avg = new Label("~" + fmt(ce.avgMonthly()) + " ₽");
                avg.setStyle("-fx-font-size: 11; -fx-text-fill: #9090a8;");
                info.getChildren().addAll(name, avg);
                Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
                Label valLbl = new Label(String.valueOf((int) ce.avgMonthly().doubleValue()));
                valLbl.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1a1a2e; -fx-min-width: 60; -fx-alignment: CENTER_RIGHT;");
                topRow.getChildren().addAll(icon, info, sp2, valLbl);

                Slider slider = new Slider(0, ce.avgMonthly().doubleValue() * 1.5, ce.avgMonthly().doubleValue());
                slider.setMaxWidth(Double.MAX_VALUE);
                slider.setStyle("-fx-accent: #4a56e2;");
                slider.valueProperty().addListener((obs, old, nv) -> {
                    valLbl.setText(String.valueOf((int) nv.doubleValue()));
                });
                expRow.getChildren().addAll(topRow, slider);
                left.getChildren().add(expRow);
            }
        }

        // Правая — результат
        VBox right = new VBox(14);
        right.setMinWidth(340);
        right.setMaxWidth(400);

        Label rightTitle = new Label("Результат изменений");
        rightTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        right.getChildren().add(rightTitle);

        // Старый/новый расход
        HBox compareRow = new HBox(12);
        VBox oldBox = new VBox(4);
        oldBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 14 16;");
        HBox.setHgrow(oldBox, Priority.ALWAYS);
        Label oldLbl = smallCap("СТАРЫЙ РАСХОД");
        Label oldVal = new Label(fmt(data.currentMonthlyExpense()) + " ₽");
        oldVal.setStyle("-fx-font-size: 16; -fx-text-fill: #9090a8; -fx-strikethrough: true;");
        oldBox.getChildren().addAll(oldLbl, oldVal);

        VBox newBox = new VBox(4);
        newBox.setStyle("-fx-background-color: #fff0f0; -fx-background-radius: 12; -fx-padding: 14 16;");
        HBox.setHgrow(newBox, Priority.ALWAYS);
        Label newLbl = smallCap("НОВЫЙ РАСХОД");
        newLbl.setStyle("-fx-font-size: 10; -fx-text-fill: #e74c3c;");
        Label newVal = new Label(fmt(data.currentMonthlyExpense()) + " ₽");
        newVal.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        newBox.getChildren().addAll(newLbl, newVal);
        compareRow.getChildren().addAll(oldBox, newBox);

        // Чистая прибыль
        BigDecimal netProfit = data.currentMonthlyIncome().subtract(data.currentMonthlyExpense());
        VBox profitCard = new VBox(8);
        profitCard.setStyle("-fx-background-color: #edeeff; -fx-background-radius: 14; -fx-padding: 20 20;");
        profitCard.setAlignment(Pos.CENTER);
        Label profitLbl = smallCap("НОВАЯ ЧИСТАЯ ПРИБЫЛЬ");
        Label profitVal = new Label((netProfit.compareTo(BigDecimal.ZERO) < 0 ? "" : "+") + fmt(netProfit) + " ₽");
        profitVal.setStyle("-fx-font-size: 30; -fx-font-weight: bold; -fx-text-fill: " +
                (netProfit.compareTo(BigDecimal.ZERO) >= 0 ? "#4a56e2" : "#e74c3c") + ";");
        Label profitSub = new Label("БУДЕТ УХОДИТЬ В КАПИТАЛ ЕЖЕМЕСЯЧНО");
        profitSub.setStyle("-fx-font-size: 10; -fx-text-fill: #9090a8;");
        profitCard.getChildren().addAll(profitLbl, profitVal, profitSub);

        // Кнопка прогноза
        Button forecastBtn = new Button("⚗  ПОСТРОИТЬ ПРОГНОЗ РОСТА");
        forecastBtn.setMaxWidth(Double.MAX_VALUE);
        forecastBtn.setStyle("-fx-background-color: #4a56e2; -fx-text-fill: white; -fx-font-size: 14; " +
                "-fx-font-weight: bold; -fx-background-radius: 14; -fx-padding: 16 20; -fx-cursor: hand;");
        forecastBtn.setOnAction(e -> showGrowthForecast(netProfit));

        right.getChildren().addAll(rightTitle, compareRow, profitCard, forecastBtn);
        root.getChildren().addAll(left, right);

        ScrollPane sp = scrollPane();
        sp.setContent(root);
        return sp;
    }

    private void showGrowthForecast(BigDecimal monthlyNet) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Прогноз роста капитала");
        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);

        Label title = new Label("📈 Прогноз роста при текущем темпе");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        content.getChildren().add(title);

        // Таблица 12 месяцев
        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(8);
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16;");
        String[] headers = {"Месяц", "Добавляется", "Накоплено"};
        for (int i = 0; i < headers.length; i++) {
            Label h = new Label(headers[i]);
            h.setStyle("-fx-font-size: 11; -fx-text-fill: #9090a8; -fx-font-weight: bold;");
            grid.add(h, i, 0);
        }
        BigDecimal cumulative = BigDecimal.ZERO;
        for (int m = 1; m <= 12; m++) {
            cumulative = cumulative.add(monthlyNet);
            Label mLbl = new Label(m + " мес.");
            mLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #4a4a6a;");
            Label addLbl = new Label((monthlyNet.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") +
                    fmt(monthlyNet) + " ₽");
            addLbl.setStyle("-fx-font-size: 13; -fx-text-fill: " +
                    (monthlyNet.compareTo(BigDecimal.ZERO) >= 0 ? "#27ae60" : "#e74c3c") + ";");
            Label cumLbl = new Label(fmt(cumulative) + " ₽");
            cumLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #4a56e2;");
            grid.add(mLbl, 0, m); grid.add(addLbl, 1, m); grid.add(cumLbl, 2, m);
        }
        content.getChildren().add(grid);
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ((Button) dlg.getDialogPane().lookupButton(ButtonType.CLOSE)).setText("Закрыть");
        dlg.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── ВКЛАДКА 5: ИСТОРИЯ ───────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════

    private ScrollPane buildHistoryTab() {
        HistoryData data = analytics.getHistoryData(historyMonth);

        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #f4f4fb;");

        // Навигация месяца
        HBox nav = new HBox(12);
        nav.setAlignment(Pos.CENTER_LEFT);
        Button prev = new Button("‹");
        prev.setStyle("-fx-background-color: white; -fx-text-fill: #4a56e2; -fx-font-size: 18; " +
                "-fx-background-radius: 8; -fx-padding: 4 12; -fx-cursor: hand;");
        prev.setOnAction(e -> { historyMonth = historyMonth.minusMonths(1); showTab(4); });
        String monthStr = historyMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("ru")).toUpperCase()
                + ". " + historyMonth.getYear();
        Label monthLbl = new Label(monthStr);
        monthLbl.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Button next = new Button("›");
        next.setStyle("-fx-background-color: white; -fx-text-fill: #4a56e2; -fx-font-size: 18; " +
                "-fx-background-radius: 8; -fx-padding: 4 12; -fx-cursor: hand;");
        next.setOnAction(e -> { historyMonth = historyMonth.plusMonths(1); showTab(4); });
        nav.getChildren().addAll(prev, monthLbl, next);

        HBox body = new HBox(20);
        HBox.setHgrow(body, Priority.ALWAYS);

        // Левая — прогноз vs факт
        VBox left = new VBox(14);
        left.setMinWidth(260);
        left.setMaxWidth(300);

        // Карточка прогноз vs факт
        VBox compCard = card();
        GridPane compGrid = new GridPane();
        compGrid.setHgap(30); compGrid.setVgap(8);
        Label progLbl = new Label("ПРОГНОЗ"); progLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #9090a8;");
        Label factLbl = new Label("ФАКТ МЕСЯЦА"); factLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #4a56e2; -fx-font-weight: bold;");
        compGrid.add(progLbl, 0, 0); compGrid.add(factLbl, 1, 0);

        compGrid.add(makeCompRow("Доход", "#27ae60"), 0, 1);
        compGrid.add(makeCompVal(fmt(data.forecastIncome()) + " ₽", "#27ae60"), 0, 2);
        compGrid.add(makeCompVal(fmt(data.actualIncome()) + " ₽", "#27ae60"), 1, 2);

        compGrid.add(makeCompRow("Расход", "#e74c3c"), 0, 3);
        compGrid.add(makeCompVal(fmt(data.forecastExpense()) + " ₽", "#e74c3c"), 0, 4);
        compGrid.add(makeCompVal(fmt(data.actualExpense()) + " ₽", "#e74c3c"), 1, 4);

        BigDecimal factTotal = data.actualIncome().subtract(data.actualExpense());
        compGrid.add(makeCompRow("Итог", "#4a56e2"), 0, 5);
        BigDecimal forecastTotal = data.forecastIncome().subtract(data.forecastExpense());
        compGrid.add(makeCompVal(fmt(forecastTotal) + " ₽",
                forecastTotal.compareTo(BigDecimal.ZERO) >= 0 ? "#4a56e2" : "#e74c3c"), 0, 6);
        compGrid.add(makeCompVal(fmt(factTotal) + " ₽",
                factTotal.compareTo(BigDecimal.ZERO) >= 0 ? "#4a56e2" : "#e74c3c"), 1, 6);

        compCard.getChildren().add(compGrid);

        // Топ доход / расход
        VBox topCard = card();
        if (!data.topIncomeSrc().equals("—")) {
            HBox topInc = new HBox(10); topInc.setAlignment(Pos.CENTER_LEFT);
            Label incIcon = new Label("💚"); incIcon.setStyle("-fx-font-size: 22; -fx-background-color: #e8f8ef; -fx-background-radius: 8; -fx-padding: 6 8;");
            VBox incInfo = new VBox(2);
            Label incSub2 = new Label("Основной доход"); incSub2.setStyle("-fx-font-size: 11; -fx-text-fill: #9090a8;");
            Label incMain = new Label("Перевели  " + fmt(data.topIncomeAmt()) + " ₽");
            incMain.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            incInfo.getChildren().addAll(incSub2, incMain);
            topInc.getChildren().addAll(incIcon, incInfo);
            topCard.getChildren().add(topInc);
        }
        if (!data.topExpenseSrc().equals("—")) {
            HBox topExp = new HBox(10); topExp.setAlignment(Pos.CENTER_LEFT);
            Label expIcon = new Label("🚌"); expIcon.setStyle("-fx-font-size: 22; -fx-background-color: #fdecea; -fx-background-radius: 8; -fx-padding: 6 8;");
            VBox expInfo = new VBox(2);
            Label expSub2 = new Label("Основной расход"); expSub2.setStyle("-fx-font-size: 11; -fx-text-fill: #9090a8;");
            Label expMain = new Label(data.topExpenseSrc() + "  " + fmt(data.topExpenseAmt()) + " ₽");
            expMain.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            expInfo.getChildren().addAll(expSub2, expMain);
            topExp.getChildren().addAll(expIcon, expInfo);
            topCard.getChildren().add(topExp);
        }
        if (topCard.getChildren().isEmpty()) {
            topCard.getChildren().add(new Label("Нет транзакций за этот месяц."));
        }

        left.getChildren().addAll(compCard, topCard);

        // Правая — график + категории
        VBox right = new VBox(14);
        HBox.setHgrow(right, Priority.ALWAYS);

        // Переключатель Расходы/Доходы/Сравнение
        HBox chartTabs = new HBox(0);
        chartTabs.setStyle("-fx-background-color: #ededf7; -fx-background-radius: 10;");
        final int[] chartMode = {2}; // 0=расходы, 1=доходы, 2=сравнение
        Button btnExp = chartTabBtn("Расходы", false);
        Button btnInc = chartTabBtn("Доходы", false);
        Button btnCmp = chartTabBtn("Сравнение", true);
        Canvas barChart = new Canvas(600, 220);
        drawBarChart(barChart, data.dailyPoints(), 2);
        btnExp.setOnAction(e -> { chartMode[0] = 0; updateChartTabs(btnExp, btnInc, btnCmp, 0); drawBarChart(barChart, data.dailyPoints(), 0); });
        btnInc.setOnAction(e -> { chartMode[0] = 1; updateChartTabs(btnExp, btnInc, btnCmp, 1); drawBarChart(barChart, data.dailyPoints(), 1); });
        btnCmp.setOnAction(e -> { chartMode[0] = 2; updateChartTabs(btnExp, btnInc, btnCmp, 2); drawBarChart(barChart, data.dailyPoints(), 2); });
        chartTabs.getChildren().addAll(btnExp, btnInc, btnCmp);

        // Чистый результат
        Label netResult = new Label("Чистый результат");
        netResult.setStyle("-fx-font-size: 13; -fx-text-fill: #9090a8;");
        Label netVal = new Label((factTotal.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + fmt(factTotal) + " ₽");
        netVal.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " +
                (factTotal.compareTo(BigDecimal.ZERO) >= 0 ? "#27ae60" : "#e74c3c") + ";");

        VBox chartCard = card();
        chartCard.getChildren().addAll(chartTabs, netResult, netVal, barChart);

        // Легенда категорий
        HBox catTabRow = new HBox(0);
        catTabRow.setStyle("-fx-background-color: #ededf7; -fx-background-radius: 10;");
        Button catExpBtn = chartTabBtn("Расходы", true);
        Button catIncBtn = chartTabBtn("Доходы", false);
        VBox catLegend = new VBox(6);
        buildCategoryLegend(catLegend, data.expenseShares());
        catExpBtn.setOnAction(e -> {
            updateChartTabs(catExpBtn, catIncBtn, null, 0);
            catLegend.getChildren().clear();
            buildCategoryLegend(catLegend, data.expenseShares());
        });
        catIncBtn.setOnAction(e -> {
            updateChartTabs(catExpBtn, catIncBtn, null, 1);
            catLegend.getChildren().clear();
            buildCategoryLegend(catLegend, data.incomeShares());
        });
        catTabRow.getChildren().addAll(catExpBtn, catIncBtn);

        VBox catCard = card();
        catCard.getChildren().addAll(catTabRow, catLegend);

        right.getChildren().addAll(chartCard, catCard);
        body.getChildren().addAll(left, right);
        root.getChildren().addAll(nav, body);

        ScrollPane sp = scrollPane();
        sp.setContent(root);
        return sp;
    }

    // ── Рисование столбчатого графика ──────────────────────────────────

    private void drawBarChart(Canvas canvas, List<DayPoint> days, int mode) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);

        if (days.isEmpty()) {
            gc.setFill(Color.web("#9090a8")); gc.setFont(Font.font(13));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Нет данных за выбранный месяц", w / 2, h / 2);
            return;
        }

        double padL = 20, padR = 20, padT = 10, padB = 30;
        double plotW = w - padL - padR, plotH = h - padT - padB;

        double maxVal = 0;
        for (DayPoint p : days) {
            if (mode == 0) maxVal = Math.max(maxVal, p.expense().doubleValue());
            else if (mode == 1) maxVal = Math.max(maxVal, p.income().doubleValue());
            else maxVal = Math.max(maxVal, Math.max(p.income().doubleValue(), p.expense().doubleValue()));
        }
        if (maxVal == 0) maxVal = 1;

        int n = days.size();
        double barW = Math.min(plotW / (n * (mode == 2 ? 2.5 : 1.5)), 30);
        double groupW = mode == 2 ? barW * 2.2 : barW * 1.5;
        double totalW = n * groupW;
        double startX = padL + (plotW - totalW) / 2;

        // Ось X — дни
        gc.setFill(Color.web("#9090a8")); gc.setFont(Font.font(11));
        gc.setTextAlign(TextAlignment.CENTER);

        for (int i = 0; i < n; i++) {
            double cx = startX + i * groupW + groupW / 2;
            DayPoint p = days.get(i);

            if (mode == 0 || mode == 2) {
                double bh = (p.expense().doubleValue() / maxVal) * plotH;
                gc.setFill(Color.web("#e8f0ff"));
                double bx = mode == 2 ? cx - barW - 1 : cx - barW / 2;
                gc.fillRoundRect(bx, padT + plotH - bh, barW, bh, 6, 6);
                gc.setFill(Color.web("#4a56e2"));
                double inner = bh * 0.65;
                gc.fillRoundRect(bx, padT + plotH - inner, barW, inner, 6, 6);
            }
            if (mode == 1 || mode == 2) {
                double bh = (p.income().doubleValue() / maxVal) * plotH;
                gc.setFill(Color.web("#e8f8ef"));
                double bx = mode == 2 ? cx + 1 : cx - barW / 2;
                gc.fillRoundRect(bx, padT + plotH - bh, barW, bh, 6, 6);
                gc.setFill(Color.web("#27ae60"));
                double inner = bh * 0.65;
                gc.fillRoundRect(bx, padT + plotH - inner, barW, inner, 6, 6);
            }

            gc.setFill(Color.web("#9090a8"));
            gc.fillText(String.valueOf(p.day()), cx, h - 8);
        }

        // Прогноз линия (пунктир)
        gc.setStroke(Color.web("#4a56e2", 0.4));
        gc.setLineWidth(1.5);
        gc.setLineDashes(6, 4);
        double avgH = (days.stream().mapToDouble(p -> p.income().doubleValue()).average().orElse(0) / maxVal) * plotH;
        gc.strokeLine(padL, padT + plotH - avgH, padL + plotW, padT + plotH - avgH);
        gc.setLineDashes();
    }

    private void buildCategoryLegend(VBox container, List<CategoryShare> shares) {
        if (shares.isEmpty()) {
            Label empty = new Label("Нет данных");
            empty.setStyle("-fx-font-size: 13; -fx-text-fill: #9090a8;");
            container.getChildren().add(empty);
            return;
        }
        FlowPane flow = new FlowPane(12, 8);
        for (CategoryShare cs : shares) {
            HBox item = new HBox(6);
            item.setAlignment(Pos.CENTER_LEFT);
            Label dot = new Label("●");
            dot.setStyle("-fx-font-size: 14; -fx-text-fill: " + cs.color() + ";");
            Label name = new Label(cs.name());
            name.setStyle("-fx-font-size: 12; -fx-text-fill: #4a4a6a;");
            item.getChildren().addAll(dot, name);
            flow.getChildren().add(item);
        }
        container.getChildren().add(flow);
    }

    private void updateChartTabs(Button b1, Button b2, Button b3, int active) {
        Button[] btns = {b1, b2, b3};
        for (int i = 0; i < btns.length; i++) {
            if (btns[i] == null) continue;
            if (i == active) {
                btns[i].setStyle("-fx-background-color: white; -fx-text-fill: #1a1a2e; " +
                        "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 8; " +
                        "-fx-padding: 7 16; -fx-cursor: hand;");
            } else {
                btns[i].setStyle("-fx-background-color: transparent; -fx-text-fill: #9090a8; " +
                        "-fx-font-size: 13; -fx-background-radius: 8; -fx-padding: 7 16; -fx-cursor: hand;");
            }
        }
    }

    // ── Вспомогательные UI-методы ─────────────────────────────────────────

    private VBox card() {
        VBox v = new VBox(8);
        v.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 18 20;");
        return v;
    }

    private ScrollPane scrollPane() {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #f4f4fb; -fx-background: #f4f4fb; -fx-border-color: transparent;");
        return sp;
    }

    private Label smallCap(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10; -fx-text-fill: #9090a8; -fx-font-weight: bold;");
        return l;
    }

    private Label smallCapWithIcon(String icon, String text) {
        Label l = new Label(icon + "  " + text);
        l.setStyle("-fx-font-size: 11; -fx-text-fill: #9090a8; -fx-font-weight: bold;");
        return l;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11; -fx-text-fill: #9090a8; -fx-font-weight: bold; -fx-padding: 8 0 2 0;");
        return l;
    }

    private Label bigNum(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 34; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        return l;
    }

    private Label midNum(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        return l;
    }

    private HBox dotRow(String color, String text) {
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("●");
        dot.setStyle("-fx-font-size: 10; -fx-text-fill: " + color + ";");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 10; -fx-text-fill: #9090a8; -fx-font-weight: bold;");
        h.getChildren().addAll(dot, lbl);
        return h;
    }

    private HBox legendItem(String color, String text) {
        HBox h = new HBox(6);
        h.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12; -fx-text-fill: #4a4a6a;");
        h.getChildren().addAll(dot, lbl);
        return h;
    }

    private Label badge(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: " + color + "; " +
                "-fx-background-color: " + color + "22; -fx-background-radius: 8; -fx-padding: 4 10;");
        return l;
    }

    private Button chartTabBtn(String text, boolean active) {
        Button btn = new Button(text);
        if (active) btn.setStyle("-fx-background-color: white; -fx-text-fill: #1a1a2e; " +
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 7 16; -fx-cursor: hand;");
        else btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9090a8; " +
                "-fx-font-size: 13; -fx-background-radius: 8; -fx-padding: 7 16; -fx-cursor: hand;");
        return btn;
    }

    private Label makeCompRow(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
        return l;
    }

    private Label makeCompVal(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        return l;
    }

    private String fmt(BigDecimal v) {
        if (v == null) return "0,00";
        return FMT.format(v);
    }

    private String formatShort(double v) {
        if (Math.abs(v) >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (Math.abs(v) >= 1_000) return String.format("%.0fK", v / 1_000);
        return String.format("%.0f", v);
    }

    // ── Рисование линии на Canvas ─────────────────────────────────────────

    private interface PointGetter { double get(MonthPoint p); }

    private void drawLine(GraphicsContext gc, List<MonthPoint> history,
                          double padL, double padT, double plotH, double step,
                          double minY, double maxY, PointGetter getter, String colorHex) {
        gc.setStroke(Color.web(colorHex));
        gc.setLineWidth(2.5);
        gc.setLineDashes();
        gc.beginPath();
        for (int i = 0; i < history.size(); i++) {
            double x = padL + i * step;
            double y = toY(getter.get(history.get(i)), padT, plotH, minY, maxY);
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();
    }

    private double toY(double val, double padT, double plotH, double minY, double maxY) {
        return padT + plotH - (val - minY) / (maxY - minY) * plotH;
    }

    private void drawDot(GraphicsContext gc, double x, double y, String color) {
        gc.setFill(Color.web(color));
        gc.fillOval(x - 5, y - 5, 10, 10);
        gc.setFill(Color.WHITE);
        gc.fillOval(x - 3, y - 3, 6, 6);
    }
}
