package com.studio.booking.dao.impl;

import com.studio.booking.dao.EquipmentDao;
import com.studio.booking.db.DatabaseManager;
import com.studio.booking.model.Equipment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-реализация {@link EquipmentDao}.
 * <p>
 * Работает со справочником дополнительного оборудования (вспышки, софтбоксы и т.д.).
 * Цена {@code price_per_hour} участвует в расчёте стоимости брони на уровне сервиса.
 * Связь оборудования с конкретной бронью хранится в таблице {@code booking_equipment}
 * и управляется через {@link BookingDaoImpl}.
 */
public class EquipmentDaoImpl implements EquipmentDao {

    /** Поиск одной позиции оборудования по первичному ключу. */
    @Override
    public Optional<Equipment> findById(int id) {
        String sql = "SELECT * FROM equipment WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска оборудования по id", e);
        }
        return Optional.empty();
    }

    /** Полный каталог оборудования для списков и диалога бронирования. */
    @Override
    public List<Equipment> findAll() {
        String sql = "SELECT * FROM equipment ORDER BY id";
        List<Equipment> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения списка оборудования", e);
        }
        return list;
    }

    /**
     * Добавляет новую позицию в справочник.
     * После вставки записывает сгенерированный SQLite-id обратно в переданный объект.
     */
    @Override
    public Equipment save(Equipment equipment) {
        String sql = "INSERT INTO equipment (name, price_per_hour) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, equipment.getName());
            ps.setDouble(2, equipment.getPricePerHour());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    equipment.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения оборудования", e);
        }
        return equipment;
    }

    /** Обновляет название и почасовую цену существующей записи. */
    @Override
    public boolean update(Equipment equipment) {
        String sql = "UPDATE equipment SET name = ?, price_per_hour = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, equipment.getName());
            ps.setDouble(2, equipment.getPricePerHour());
            ps.setInt(3, equipment.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления оборудования", e);
        }
    }

    /**
     * Удаляет запись из справочника.
     * Может завершиться ошибкой SQLite, если оборудование ещё привязано к броням.
     */
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM equipment WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления оборудования", e);
        }
    }

    /** Собирает {@link Equipment} из текущей строки ResultSet. */
    private Equipment map(ResultSet rs) throws SQLException {
        return new Equipment(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDouble("price_per_hour")
        );
    }
}
