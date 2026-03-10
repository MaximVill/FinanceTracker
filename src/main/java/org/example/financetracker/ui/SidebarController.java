package org.example.financetracker.ui;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.financetracker.db.SettingsDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SidebarController {
    private static final Logger log = LoggerFactory.getLogger(SidebarController.class);

    private static final double EXPANDED_WIDTH  = 240;
    private static final double COLLAPSED_WIDTH = 60;
    private static final String STYLE_ACTIVE =
            "-fx-background-color: #ededff; -fx-text-fill: #6c63ff; -fx-font-weight: bold; " +
            "-fx-font-size: 13; -fx-padding: 10 14 10 14; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String STYLE_INACTIVE =
            "-fx-background-color: transparent; -fx-text-fill: #4a4a6a; " +
            "-fx-font-size: 13; -fx-padding: 10 14 10 14; -fx-background-radius: 8; -fx-cursor: hand;";

    @FXML private VBox  sidebarRoot;
    @FXML private HBox  logoBox;
    @FXML private VBox  logoTextBox;
    @FXML private VBox  navBox;
    @FXML private VBox  bottomBox;
    @FXML private HBox  userBox;
    @FXML private VBox  userTextBox;
    @FXML private Label copyrightLabel;
    @FXML private Button collapseBtn;
    @FXML private Button btnHome;
    @FXML private Button btnSummary;
    @FXML private Button btnHistory;
    @FXML private Button btnGoals;
    @FXML private Button btnPlanner;
    @FXML private Button btnDebts;
    @FXML private Button btnSettings;
    @FXML private Label  userNameLabel;
    @FXML private Label  userEmailLabel;
    @FXML private Label  chevronLabel;

    private boolean collapsed = false;
    private AppLayoutController appLayout;
    private List<Button> navButtons;

    // Иконки для свёрнутого состояния (только emoji)
    private static final String[] ICONS_COLLAPSED  = {"🏠", "📊", "🕐", "🎯", "📅", "🤝"};
    private static final String[] LABELS_EXPANDED  = {
            "🏠  Главная", "📊  Сводка", "🕐  История",
            "🎯  Цели", "📅  Планировщик", "🤝  Долги"
    };

    @FXML
    private void initialize() {
        navButtons = List.of(btnHome, btnSummary, btnHistory, btnGoals, btnPlanner, btnDebts);

        // Загружаем имя пользователя
        try {
            SettingsDAO dao = new SettingsDAO();
            String name = dao.getUserName();
            userNameLabel.setText(name.isBlank() ? "Пользователь" : name);
            userEmailLabel.setText(""); // email пока не хранится
        } catch (Exception e) {
            log.warn("Не удалось загрузить имя пользователя", e);
        }

        // Hover-эффекты на неактивных кнопках
        for (Button btn : navButtons) {
            btn.setOnMouseEntered(e -> {
                if (!STYLE_ACTIVE.equals(btn.getStyle())) {
                    btn.setStyle(STYLE_INACTIVE.replace(
                            "-fx-background-color: transparent;",
                            "-fx-background-color: #f0f0ff;"));
                }
            });
            btn.setOnMouseExited(e -> {
                if (!STYLE_ACTIVE.equals(btn.getStyle())) {
                    btn.setStyle(STYLE_INACTIVE);
                }
            });
        }
    }

    public void setAppLayout(AppLayoutController appLayout) {
        this.appLayout = appLayout;
    }

    // ── Навигация ──────────────────────────────────────────────────────────────

    @FXML private void handleHome()    { navigateTo(btnHome,    "main");     }
    @FXML private void handleSummary() { navigateTo(btnSummary, "summary");  }
    @FXML private void handleHistory() { navigateTo(btnHistory, "history");  }
    @FXML private void handleGoals()   { navigateTo(btnGoals,   "goals");    }
    @FXML private void handlePlanner() { navigateTo(btnPlanner, "planner");  }
    @FXML private void handleDebts()   { navigateTo(btnDebts,   "debts");    }
    @FXML private void handleSettings(){ navigateTo(btnSettings,"settings"); }

    private void navigateTo(Button active, String page) {
        // Сбросить стиль всех кнопок nav
        for (Button b : navButtons) b.setStyle(STYLE_INACTIVE);
        active.setStyle(STYLE_ACTIVE);
        if (appLayout != null) appLayout.showPage(page);
    }

    // ── Collapse / Expand ─────────────────────────────────────────────────────

    @FXML
    private void handleCollapse() {
        collapsed = !collapsed;

        if (collapsed) {
            // Свернуть: убрать текст, оставить иконки
            sidebarRoot.setPrefWidth(COLLAPSED_WIDTH);
            sidebarRoot.setMinWidth(COLLAPSED_WIDTH);
            logoTextBox.setVisible(false);
            logoTextBox.setManaged(false);
            collapseBtn.setText("⊞");
            userTextBox.setVisible(false);
            userTextBox.setManaged(false);
            chevronLabel.setVisible(false);
            chevronLabel.setManaged(false);
            copyrightLabel.setVisible(false);
            copyrightLabel.setManaged(false);

            String[] icons = ICONS_COLLAPSED;
            for (int i = 0; i < navButtons.size(); i++) {
                navButtons.get(i).setText(icons[i]);
                navButtons.get(i).setAlignment(javafx.geometry.Pos.CENTER);
            }
            btnSettings.setText("⚙");
            btnSettings.setAlignment(javafx.geometry.Pos.CENTER);

        } else {
            // Развернуть
            sidebarRoot.setPrefWidth(EXPANDED_WIDTH);
            sidebarRoot.setMinWidth(EXPANDED_WIDTH);
            logoTextBox.setVisible(true);
            logoTextBox.setManaged(true);
            collapseBtn.setText("⊟");
            userTextBox.setVisible(true);
            userTextBox.setManaged(true);
            chevronLabel.setVisible(true);
            chevronLabel.setManaged(true);
            copyrightLabel.setVisible(true);
            copyrightLabel.setManaged(true);

            for (int i = 0; i < navButtons.size(); i++) {
                navButtons.get(i).setText(LABELS_EXPANDED[i]);
                navButtons.get(i).setAlignment(javafx.geometry.Pos.BASELINE_LEFT);
            }
            btnSettings.setText("⚙  Настройки");
            btnSettings.setAlignment(javafx.geometry.Pos.BASELINE_LEFT);
        }
    }

    @FXML void onCollapseHover() {
        collapseBtn.setStyle(
            "-fx-background-color: #ededff; -fx-text-fill: #6c63ff; -fx-font-size: 16; " +
            "-fx-cursor: hand; -fx-padding: 4 6 4 6; -fx-background-radius: 6;");
    }
    @FXML void onCollapseExit() {
        collapseBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #9090a8; -fx-font-size: 16; " +
            "-fx-cursor: hand; -fx-padding: 4 6 4 6; -fx-background-radius: 6;");
    }

    /** Обновить имя пользователя в сайдбаре (вызывается после смены настроек) */
    public void refreshUserName() {
        try {
            String name = new SettingsDAO().getUserName();
            userNameLabel.setText(name.isBlank() ? "Пользователь" : name);
        } catch (Exception e) {
            log.warn("Ошибка обновления имени", e);
        }
    }
}
