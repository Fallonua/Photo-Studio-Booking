package com.studio.booking.dao;

import com.studio.booking.model.Booking;
import com.studio.booking.model.Equipment;
import com.studio.booking.model.HallLoadReport;

import java.time.LocalDate;
import java.util.List;

/**
 * DAO для работы с бронированиями.
 */
public interface BookingDao extends Dao<Booking> {

    /** Все бронирования (с именами зала). */
    List<Booking> findAllWithDetails();

    /** Активные бронирования зала на конкретную дату (для проверки занятости слотов). */
    List<Booking> findActiveByHallAndDate(int hallId, LocalDate date);

    /**
     * Транзакционное создание брони: вставляет запись бронирования и связи с оборудованием в одной транзакции.
     *
     * @return созданная бронь с присвоенным id
     */
    Booking createWithEquipment(Booking booking);

    /**
     * Транзакционное обновление брони: обновляет запись и пересоздаёт связи с оборудованием в одной транзакции.
     */
    Booking updateWithEquipment(Booking booking);

    /** Оборудование, привязанное к брони. */
    List<Equipment> findEquipmentByBookingId(int bookingId);

    /** Отменить бронь (status = 'cancelled'). */
    boolean cancel(int bookingId);

    /** Сформировать отчёт по загрузке залов за период [from; to]. */
    List<HallLoadReport> loadReport(LocalDate from, LocalDate to);
}
