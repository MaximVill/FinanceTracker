package org.example.financetracker.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AppLayoutController {
    private static final Logger log = LoggerFactory.getLogger(AppLayoutController.class);

    @FXML private BorderPane root; // если нужен — но мы работаем через stage
    @FXML private StackPane contentArea;

    private SidebarController sidebarController;

    // Кэш загруженных страниц
    private final Map<String, Node> pageCache = new HashMap<>();

    @FXML
    private void initialize() {
        // Загружаем sidebar
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/financetracker/sidebar-view.fxml"));
            Node sidebar = loader.load();
            sidebarController = loader.getController();
            sidebarController.setAppLayout(this);

            BorderPane bp = (BorderPane) contentArea.getParent();
            bp.setLeft(sidebar);
        } catch (Exception e) {
            log.error("Не удалось загрузить сайдбар", e);
            e.printStackTrace();
        }

        // Показываем главную страницу по умолчанию
        showPage("main");
    }

    /** Переключает контент-область на нужную страницу */
    public void showPage(String page) {
        try {
            Node content = pageCache.computeIfAbsent(page, this::loadPage);
            if (content != null) {
                contentArea.getChildren().setAll(content);
            }
        } catch (Exception e) {
            log.error("Ошибка отображения страницы: {}", page, e);
        }
    }

    private Node loadPage(String page) {
        String fxml = switch (page) {
            case "main"     -> "/org/example/financetracker/main-view.fxml";
            case "summary"  -> "/org/example/financetracker/summary-view.fxml";
            case "history"  -> "/org/example/financetracker/history-view.fxml";
            case "goals"    -> "/org/example/financetracker/goals-view.fxml";
            case "planner"  -> "/org/example/financetracker/planner-view.fxml";
            case "debts"    -> "/org/example/financetracker/debts-view.fxml";
            case "settings" -> "/org/example/financetracker/settings-view.fxml";
            default -> null;
        };

        if (fxml == null) return null;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Node node = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof AppLayoutAware aware) {
                aware.setAppLayout(this);
            }

            return node;
        } catch (Exception e) {
            log.error("Не удалось загрузить FXML: {}", fxml, e);
            // Показываем ошибку вместо пустого экрана
            javafx.scene.control.Label errLabel = new javafx.scene.control.Label(
                "Ошибка загрузки страницы:\n" + e.getMessage() +
                (e.getCause() != null ? "\n\nПричина: " + e.getCause().getMessage() : "")
            );
            errLabel.setWrapText(true);
            errLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13; -fx-padding: 24;");
            e.printStackTrace(); // всегда в консоль
            return errLabel;
        }
    }

    /** Инвалидировать кэш страницы (например, после изменения данных) */
    public void invalidatePage(String page) {
        pageCache.remove(page);
    }

    public SidebarController getSidebarController() {
        return sidebarController;
    }
}
