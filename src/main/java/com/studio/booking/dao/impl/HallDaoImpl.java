package com.studio.booking.dao.impl;

import com.studio.booking.dao.HallDao;
import com.studio.booking.db.DatabaseManager;
import com.studio.booking.model.Hall;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-реализация {@link HallDao}.
 * <p>
 * CRUD для съёмочных залов: название, вместимость, базовая цена за час и описание интерьера.
 * Базовая цена зала используется при расчёте итоговой стоимости брони в {@code BookingService}.
 */
public class HallDaoImpl implements HallDao {

    /** Поиск зала по id (нужен при редактировании брони и расчёте цены). */
    @Override
    public Optional<Hall> findById(int id) {
        String sql = "SELECT * FROM halls WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска зала по id", e);
        }
        return Optional.empty();
    }

    /** Все залы студии — для главного экрана и справочника «Управление залами». */
    @Override
    public List<Hall> findAll() {
        String sql = "SELECT * FROM halls ORDER BY id";
        List<Hall> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения списка залов", e);
        }
        return list;
    }

    /**
     * Создаёт новый зал.
     * После INSERT подставляет autoincrement-id в переданный объект {@link Hall}.
     */
    @Override
    public Hall save(Hall hall) {
        String sql = "INSERT INTO halls (name, capacity, base_price_per_hour, interior_description) " +
                "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, hall.getName());
            ps.setInt(2, hall.getCapacity());
            ps.setDouble(3, hall.getBasePricePerHour());
            ps.setString(4, hall.getInteriorDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    hall.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения зала", e);
        }
        return hall;
    }

    /** Обновляет все редактируемые поля зала по id. */
    @Override
    public boolean update(Hall hall) {
        String sql = "UPDATE halls SET name = ?, capacity = ?, base_price_per_hour = ?, " +
                "interior_description = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hall.getName());
            ps.setInt(2, hall.getCapacity());
            ps.setDouble(3, hall.getBasePricePerHour());
            ps.setString(4, hall.getInteriorDescription());
            ps.setInt(5, hall.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления зала", e);
        }
    }

    /**
     * Удаляет зал.
     * Не удалит запись, если на зал есть активные или исторические брони (ограничение FK в schema.sql).
     */
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM halls WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления зала", e);
        }
    }

    /** Собирает {@link Hall} из текущей строки ResultSet. */
    private Hall map(ResultSet rs) throws SQLException {
        return new Hall(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("capacity"),
                rs.getDouble("base_price_per_hour"),
                rs.getString("interior_description")
        );
    }
}
