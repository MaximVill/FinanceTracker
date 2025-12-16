package org.example.financetracker.db;

import org.example.financetracker.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private static final Logger log = LoggerFactory.getLogger(CategoryDAO.class);

    // 1. Получить все категории
    public List<Category> getAll() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name, type FROM categories ORDER BY type, name";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Category cat = new Category();
                cat.setId(rs.getLong("id"));
                cat.setName(rs.getString("name"));
                cat.setType(rs.getString("type"));
                categories.add(cat);
            }

            log.debug("Загружено {} категорий", categories.size());

        } catch (SQLException e) {
            log.error("Ошибка загрузки категорий", e);
        }

        return categories;
    }

    // 2. Добавить категорию
    public Category add(String name, String type) {
        String sql = "INSERT INTO categories (name, type) VALUES (?, ?)";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name.trim());
            stmt.setString(2, type);
            stmt.executeUpdate();

            // Получаем ID
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Category cat = new Category();
                    cat.setId(rs.getLong(1));
                    cat.setName(name.trim());
                    cat.setType(type);

                    log.info("Добавлена категория: {} ({})", name, type);
                    return cat;
                }
            }

        } catch (SQLException e) {
            log.error("Ошибка добавления категории: {}", name, e);
        }

        return null;
    }

    // 3. Удалить категорию (только если не используется)
    public boolean delete(long id) {
        // Проверяем, используется ли категория
        if (isUsed(id)) {
            log.warn("Категория id={} используется в транзакциях", id);
            return false;
        }

        String sql = "DELETE FROM categories WHERE id = ?";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                log.info("Удалена категория id={}", id);
                return true;
            }

        } catch (SQLException e) {
            log.error("Ошибка удаления категории id={}", id, e);
        }

        return false;
    }

    // 4. Проверить существование категории по имени
    public boolean exists(String name) {
        String sql = "SELECT id FROM categories WHERE name = ?";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            log.error("Ошибка проверки категории", e);
        }

        return false;
    }

    // 5. Получить ID категории по имени
    public Long getIdByName(String name) {
        String sql = "SELECT id FROM categories WHERE name = ?";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }

        } catch (SQLException e) {
            log.error("Ошибка получения ID категории", e);
        }

        return null;
    }

    // Вспомогательный: проверка использования категории
    private boolean isUsed(long categoryId) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE category_id = ?";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            log.error("Ошибка проверки использования категории", e);
        }

        return false;
    }
}