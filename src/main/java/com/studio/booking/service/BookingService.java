package com.studio.booking.service;

import com.studio.booking.dao.BookingDao;
import com.studio.booking.dao.HallDao;
import com.studio.booking.dao.impl.BookingDaoImpl;
import com.studio.booking.dao.impl.HallDaoImpl;
import com.studio.booking.model.Booking;
import com.studio.booking.model.Equipment;
import com.studio.booking.model.Hall;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Бизнес-логика бронирования: расчёт цены, проверка доступности,
 * проверка ограничений по времени и создание брони.
 */
public class BookingService {

    /** Рабочее время студии (начало) — 9:00. */
    public static final int WORK_START_HOUR = 9;
    /** Рабочее время студии (конец) — 21:00. */
    public static final int WORK_END_HOUR = 21;
    /** Минимальная длительность брони, часов. */
    public static final int MIN_DURATION_HOURS = 1;
    /** Максимальная длительность брони, часов. */
    public static final int MAX_DURATION_HOURS = 8;
    /** На сколько дней вперёд можно бронировать. */
    public static final int MAX_DAYS_AHEAD = 30;
    /** Минимальный отступ от текущего момента, минут. */
    public static final int MIN_LEAD_MINUTES = 30;

    private final BookingDao bookingDao = new BookingDaoImpl();
    private final HallDao hallDao = new HallDaoImpl();

    public List<LocalTime> findAvailableStartTimes(int hallId, LocalDate date) {
        return findAvailableStartTimes(hallId, date, null);
    }

    public List<LocalTime> findAvailableStartTimes(int hallId, LocalDate date, Integer excludeBookingId) {
        List<Booking> active = bookingDao.findActiveByHallAndDate(hallId, date);
        List<LocalTime> result = new ArrayList<>();
        for (int hour = WORK_START_HOUR; hour < WORK_END_HOUR; hour++) {
            LocalTime slotStart = LocalTime.of(hour, 0);
            LocalTime slotEnd = slotStart.plusHours(1);
            if (!overlapsAny(active, slotStart, slotEnd, excludeBookingId)) {
                result.add(slotStart);
            }
        }
        return result;
    }

    public List<LocalTime> findAvailableEndTimes(int hallId, LocalDate date, LocalTime start) {
        return findAvailableEndTimes(hallId, date, start, null);
    }

    public List<LocalTime> findAvailableEndTimes(int hallId, LocalDate date, LocalTime start,
                                               Integer excludeBookingId) {
        List<Booking> active = bookingDao.findActiveByHallAndDate(hallId, date);
        List<LocalTime> result = new ArrayList<>();
        for (int duration = MIN_DURATION_HOURS; duration <= MAX_DURATION_HOURS; duration++) {
            LocalTime end = start.plusHours(duration);
            if (end.getHour() > WORK_END_HOUR || (end.getHour() == 0)) {
                break;
            }
            if (end.isAfter(LocalTime.of(WORK_END_HOUR, 0))) {
                break;
            }
            if (overlapsAny(active, start, end, excludeBookingId)) {
                break;
            }
            result.add(end);
        }
        return result;
    }

    private boolean overlapsAny(List<Booking> active, LocalTime start, LocalTime end,
                              Integer excludeBookingId) {
        for (Booking b : active) {
            if (excludeBookingId != null && b.getId() == excludeBookingId) {
                continue;
            }
            if (start.isBefore(b.getEndTime()) && end.isAfter(b.getStartTime())) {
                return true;
            }
        }
        return false;
    }

    public double calculatePrice(Hall hall, LocalTime start, LocalTime end, List<Equipment> equipment) {
        long hours = java.time.Duration.between(start, end).toHours();
        double equipmentPerHour = equipment.stream().mapToDouble(Equipment::getPricePerHour).sum();
        return (hall.getBasePricePerHour() + equipmentPerHour) * hours;
    }

    public Booking createBooking(String customerName, String customerPhone, Hall hall, LocalDate date,
                                 LocalTime start, LocalTime end, List<Equipment> equipment) {
        validateCustomer(customerName, customerPhone);
        validate(hall, date, start, end);
        Booking booking = new Booking();
        booking.setCustomerName(customerName.trim());
        booking.setCustomerPhone(customerPhone.trim());
        booking.setHallId(hall.getId());
        booking.setBookingDate(date);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setEquipmentList(equipment);
        booking.setTotalPrice(calculatePrice(hall, start, end, equipment));
        booking.setStatus("active");
        return bookingDao.createWithEquipment(booking);
    }

    public Booking updateBooking(int bookingId, String customerName, String customerPhone,
                                 Hall hall, LocalDate date, LocalTime start, LocalTime end,
                                 List<Equipment> equipment) {
        Booking existing = bookingDao.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Бронь не найдена."));
        if (!"active".equals(existing.getStatus())) {
            throw new IllegalArgumentException("Можно редактировать только активные брони.");
        }
        validateCustomer(customerName, customerPhone);
        validate(hall, date, start, end);
        existing.setCustomerName(customerName.trim());
        existing.setCustomerPhone(customerPhone.trim());
        existing.setHallId(hall.getId());
        existing.setBookingDate(date);
        existing.setStartTime(start);
        existing.setEndTime(end);
        existing.setEquipmentList(equipment);
        existing.setTotalPrice(calculatePrice(hall, start, end, equipment));
        return bookingDao.updateWithEquipment(existing);
    }

    public List<Equipment> getBookingEquipment(int bookingId) {
        return bookingDao.findEquipmentByBookingId(bookingId);
    }

    private void validateCustomer(String customerName, String customerPhone) {
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Укажите имя клиента.");
        }
        if (customerPhone == null || customerPhone.isBlank()) {
            throw new IllegalArgumentException("Укажите телефон клиента.");
        }
    }

    private void validate(Hall hall, LocalDate date, LocalTime start, LocalTime end) {
        if (hall == null) {
            throw new IllegalArgumentException("Не выбран зал.");
        }
        if (date == null || start == null || end == null) {
            throw new IllegalArgumentException("Не выбраны дата или время.");
        }
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Нельзя бронировать дату в прошлом.");
        }
        if (date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new IllegalArgumentException(
                    "Бронировать можно не более чем на " + MAX_DAYS_AHEAD + " дней вперёд.");
        }
        long duration = java.time.Duration.between(start, end).toHours();
        if (duration < MIN_DURATION_HOURS) {
            throw new IllegalArgumentException(
                    "Минимальная длительность брони — " + MIN_DURATION_HOURS + " час.");
        }
        if (duration > MAX_DURATION_HOURS) {
            throw new IllegalArgumentException(
                    "Максимальная длительность брони — " + MAX_DURATION_HOURS + " часов.");
        }
        if (start.getHour() < WORK_START_HOUR || end.getHour() > WORK_END_HOUR
                || (end.getHour() == WORK_END_HOUR && end.getMinute() > 0)) {
            throw new IllegalArgumentException(
                    "Студия работает с " + WORK_START_HOUR + ":00 до " + WORK_END_HOUR + ":00.");
        }
        if (date.isEqual(today)) {
            LocalDateTime startMoment = LocalDateTime.of(date, start);
            LocalDateTime earliest = LocalDateTime.now().plusMinutes(MIN_LEAD_MINUTES);
            if (startMoment.isBefore(earliest)) {
                throw new IllegalArgumentException(
                        "Начать бронь можно не ранее чем через " + MIN_LEAD_MINUTES
                                + " минут от текущего времени.");
            }
        }
    }

    public List<Booking> getAllBookings() {
        return bookingDao.findAllWithDetails();
    }

    public boolean cancelBooking(int bookingId) {
        return bookingDao.cancel(bookingId);
    }
}
