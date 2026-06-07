package com.studio.booking.dao.impl;

import com.studio.booking.dao.BookingDao;
import com.studio.booking.db.DatabaseManager;
import com.studio.booking.model.Booking;
import com.studio.booking.model.Equipment;
import com.studio.booking.model.HallLoadReport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-реализация {@link BookingDao}.
 * <p>
 * Основной слой доступа к бронированиям. Ключевые особенности:
 * <ul>
 *   <li>создание и редактирование брони выполняются в транзакции вместе со связями
 *       {@code booking_equipment};</li>
 *   <li>для таблицы «Все бронирования» данные подтягиваются JOIN-ом с залами и оборудованием;</li>
 *   <li>отмена — мягкая: статус меняется на {@code cancelled}, строка не удаляется.</li>
 * </ul>
 */
public class BookingDaoImpl implements BookingDao {

    /** Одна бронь по id без JOIN (только таблица {@code bookings}). */
    @Override
    public Optional<Booking> findById(int id) {
        String sql = "SELECT * FROM bookings WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска брони по id", e);
        }
        return Optional.empty();
    }

    /** Все брони только из таблицы {@code bookings}, без имён залов и оборудования. */
    @Override
    public List<Booking> findAll() {
        String sql = "SELECT * FROM bookings ORDER BY booking_date, start_time";
        List<Booking> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения списка броней", e);
        }
        return list;
    }

    /**
     * Брони для итоговой таблицы в UI: зал + оборудование одной строкой.
     * <p>
     * {@code GROUP_CONCAT(e.name, ', ')} склеивает названия выбранного оборудования через запятую.
     * Если оборудования нет, поле {@code equipment_summary} будет {@code null}
     * (в модели {@link Booking#getEquipmentSummary()} отображается как «—»).
     */
    @Override
    public List<Booking> findAllWithDetails() {
        String sql = "SELECT b.*, h.name AS hall_name, " +
                "GROUP_CONCAT(e.name, ', ') AS equipment_summary " +
                "FROM bookings b " +
                "JOIN halls h ON b.hall_id = h.id " +
                "LEFT JOIN booking_equipment be ON be.booking_id = b.id " +
                "LEFT JOIN equipment e ON e.id = be.equipment_id " +
                "GROUP BY b.id " +
                "ORDER BY b.booking_date DESC, b.start_time";
        List<Booking> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Booking b = map(rs);
                b.setHallName(rs.getString("hall_name"));
                b.setEquipmentSummary(rs.getString("equipment_summary"));
                list.add(b);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения всех броней", e);
        }
        return list;
    }

    /**
     * Активные брони зала на конкретную дату.
     * Используется {@code BookingService} для проверки пересечений временных слотов.
     */
    @Override
    public List<Booking> findActiveByHallAndDate(int hallId, LocalDate date) {
        String sql = "SELECT * FROM bookings " +
                "WHERE hall_id = ? AND booking_date = ? AND status = 'active' " +
                "ORDER BY start_time";
        List<Booking> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, hallId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения броней зала на дату", e);
        }
        return list;
    }

    /**
     * Создаёт бронь и связи с оборудованием атомарно.
     * <p>
     * Шаги транзакции: INSERT в {@code bookings} → batch INSERT в {@code booking_equipment}.
     * При любой ошибке выполняется {@code rollback}, чтобы не осталась «висячая» бронь без оборудования.
     */
    @Override
    public Booking createWithEquipment(Booking booking) {
        String insertBooking = "INSERT INTO bookings " +
                "(customer_name, customer_phone, hall_id, booking_date, start_time, end_time, total_price, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'active')";
        String insertLink = "INSERT INTO booking_equipment (booking_id, equipment_id) VALUES (?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // 1. Основная запись брони; id забираем из RETURN_GENERATED_KEYS
            try (PreparedStatement ps = conn.prepareStatement(insertBooking, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, booking.getCustomerName());
                ps.setString(2, booking.getCustomerPhone());
                ps.setInt(3, booking.getHallId());
                ps.setString(4, booking.getBookingDate().toString());
                ps.setString(5, booking.getStartTime().toString());
                ps.setString(6, booking.getEndTime().toString());
                ps.setDouble(7, booking.getTotalPrice());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        booking.setId(keys.getInt(1));
                    }
                }
            }

            // 2. Связи many-to-many: одна строка booking_equipment на каждую единицу оборудования
            if (booking.getEquipmentList() != null && !booking.getEquipmentList().isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(insertLink)) {
                    for (Equipment eq : booking.getEquipmentList()) {
                        ps.setInt(1, booking.getId());
                        ps.setInt(2, eq.getId());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            conn.commit();
            return booking;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // ошибка отката не должна маскировать исходное исключение
                }
            }
            throw new RuntimeException("Ошибка создания брони (транзакция отменена)", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    // игнорируем ошибку закрытия соединения
                }
            }
        }
    }

    /**
     * Обновляет бронь и полностью пересоздаёт список оборудования.
     * <p>
     * Сначала UPDATE активной брони, затем DELETE всех старых связей,
     * затем batch INSERT новых. Редактировать можно только брони со статусом {@code active}.
     */
    @Override
    public Booking updateWithEquipment(Booking booking) {
        String updateBooking = "UPDATE bookings SET customer_name = ?, customer_phone = ?, hall_id = ?, " +
                "booking_date = ?, start_time = ?, end_time = ?, total_price = ? " +
                "WHERE id = ? AND status = 'active'";
        String deleteLinks = "DELETE FROM booking_equipment WHERE booking_id = ?";
        String insertLink = "INSERT INTO booking_equipment (booking_id, equipment_id) VALUES (?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(updateBooking)) {
                ps.setString(1, booking.getCustomerName());
                ps.setString(2, booking.getCustomerPhone());
                ps.setInt(3, booking.getHallId());
                ps.setString(4, booking.getBookingDate().toString());
                ps.setString(5, booking.getStartTime().toString());
                ps.setString(6, booking.getEndTime().toString());
                ps.setDouble(7, booking.getTotalPrice());
                ps.setInt(8, booking.getId());
                if (ps.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Бронь не найдена или уже отменена.");
                }
            }

            // Старые связи удаляются целиком — проще, чем diff по equipment_id
            try (PreparedStatement ps = conn.prepareStatement(deleteLinks)) {
                ps.setInt(1, booking.getId());
                ps.executeUpdate();
            }

            if (booking.getEquipmentList() != null && !booking.getEquipmentList().isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(insertLink)) {
                    for (Equipment eq : booking.getEquipmentList()) {
                        ps.setInt(1, booking.getId());
                        ps.setInt(2, eq.getId());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            conn.commit();
            return booking;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // игнорируем
                }
            }
            throw new RuntimeException("Ошибка обновления брони (транзакция отменена)", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    // игнорируем
                }
            }
        }
    }

    /**
     * Список оборудования, привязанного к брони.
     * Нужен при открытии диалога редактирования — чтобы отметить уже выбранные позиции.
     */
    @Override
    public List<Equipment> findEquipmentByBookingId(int bookingId) {
        String sql = "SELECT e.* FROM equipment e " +
                "JOIN booking_equipment be ON e.id = be.equipment_id " +
                "WHERE be.booking_id = ? ORDER BY e.name";
        List<Equipment> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Equipment(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price_per_hour")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения оборудования брони", e);
        }
        return list;
    }

    /** Делегирует создание в {@link #createWithEquipment(Booking)} — интерфейс {@code Dao#save}. */
    @Override
    public Booking save(Booking booking) {
        return createWithEquipment(booking);
    }

    /**
     * Обновляет все поля брони, включая статус.
     * Не трогает {@code booking_equipment}; для UI редактирования используется {@link #updateWithEquipment}.
     */
    @Override
    public boolean update(Booking booking) {
        String sql = "UPDATE bookings SET customer_name = ?, customer_phone = ?, hall_id = ?, " +
                "booking_date = ?, start_time = ?, end_time = ?, total_price = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, booking.getCustomerName());
            ps.setString(2, booking.getCustomerPhone());
            ps.setInt(3, booking.getHallId());
            ps.setString(4, booking.getBookingDate().toString());
            ps.setString(5, booking.getStartTime().toString());
            ps.setString(6, booking.getEndTime().toString());
            ps.setDouble(7, booking.getTotalPrice());
            ps.setString(8, booking.getStatus());
            ps.setInt(9, booking.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления брони", e);
        }
    }

    /** Мягкая отмена: {@code status = 'cancelled'}. Запись и связи с оборудованием сохраняются. */
    @Override
    public boolean cancel(int bookingId) {
        String sql = "UPDATE bookings SET status = 'cancelled' WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка отмены брони", e);
        }
    }

    /** Физическое удаление брони (в приложении не используется из UI. BookingDao extends Dao<Booking> - поэтому оно тут). */
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM bookings WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления брони", e);
        }
    }

    /**
     * Отчёт по загрузке залов за период [from; to].
     * <p>
     * Для каждого зала считаются: число активных броней, суммарные часы (разница end − start) и выручка.
     * LEFT JOIN гарантирует, что залы без броней тоже попадут в отчёт с нулями.
     */
    @Override
    public List<HallLoadReport> loadReport(LocalDate from, LocalDate to) {
        String sql = "SELECT h.name AS hall_name, " +
                "COUNT(b.id) AS cnt, " +
                "COALESCE(SUM((strftime('%H', b.end_time) - strftime('%H', b.start_time))), 0) AS hours, " +
                "COALESCE(SUM(b.total_price), 0) AS revenue " +
                "FROM halls h " +
                "LEFT JOIN bookings b ON b.hall_id = h.id " +
                "   AND b.status = 'active' " +
                "   AND b.booking_date BETWEEN ? AND ? " +
                "GROUP BY h.id, h.name " +
                "ORDER BY h.name";
        List<HallLoadReport> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new HallLoadReport(
                            rs.getString("hall_name"),
                            rs.getInt("cnt"),
                            rs.getLong("hours"),
                            rs.getDouble("revenue")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка формирования отчёта", e);
        }
        return list;
    }

    /** Преобразует строку таблицы {@code bookings} в объект {@link Booking}. */
    private Booking map(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setId(rs.getInt("id"));
        b.setCustomerName(rs.getString("customer_name"));
        b.setCustomerPhone(rs.getString("customer_phone"));
        b.setHallId(rs.getInt("hall_id"));
        b.setBookingDate(LocalDate.parse(rs.getString("booking_date")));
        b.setStartTime(LocalTime.parse(rs.getString("start_time")));
        b.setEndTime(LocalTime.parse(rs.getString("end_time")));
        b.setTotalPrice(rs.getDouble("total_price"));
        b.setStatus(rs.getString("status"));
        return b;
    }
}
