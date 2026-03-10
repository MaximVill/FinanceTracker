package org.example.financetracker.ui;

/**
 * Реализуют контроллеры страниц, которым нужна ссылка на AppLayoutController
 * (например, для навигации или инвалидации кэша).
 */
public interface AppLayoutAware {
    void setAppLayout(AppLayoutController appLayout);
}
