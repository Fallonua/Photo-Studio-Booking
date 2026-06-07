package com.studio.booking.dao.impl;

import com.studio.booking.dao.AdminDao;
import com.studio.booking.db.DatabaseManager;
import com.studio.booking.model.Admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * JDBC-реализация {@link AdminDao}.
 * <p>
 * В приложении предусмотрен один администратор — запись с {@code id = 1} в таблице {@code admin}.
 * Пароль хранится в открытом виде (без хеширования).
 */
public class AdminDaoImpl implements AdminDao {

    /**
     * Возвращает единственную учётную запись администратора.
     *
     * @return запись с id = 1 или пустой Optional, если таблица не инициализирована
     */
    @Override
    public Optional<Admin> find() {
        String sql = "SELECT * FROM admin WHERE id = 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(new Admin(rs.getInt("id"), rs.getString("password")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения учётной записи администратора", e);
        }
        return Optional.empty();
    }

    /**
     * Сравнивает введённый пароль с паролем из БД.
     *
     * @return {@code true}, если пароль совпал; {@code false} при неверном пароле или отсутствии записи admin
     */
    @Override
    public boolean checkPassword(String password) {
        return find()
                .map(admin -> admin.getPassword().equals(password))
                .orElse(false);
    }
}
