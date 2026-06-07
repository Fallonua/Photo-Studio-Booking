package com.studio.booking.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель бронирования.
 * Помимо полей таблицы хранит вспомогательные данные для отображения
 * (имя зала) и список выбранного оборудования.
 */
public class Booking {

    private int id;
    private String customerName;
    private String customerPhone;
    private int hallId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private double totalPrice;
    private String status;

    // Поле для удобного отображения в таблицах (заполняется при выборках с JOIN)
    private String hallName;
    private String equipmentSummary;

    // Выбранное оборудование (для создания брони и для отображения)
    private List<Equipment> equipmentList = new ArrayList<>();

    public Booking() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public int getHallId() {
        return hallId;
    }

    public void setHallId(int hallId) {
        this.hallId = hallId;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    // если нет оборудования, то показывается прочерк
    // если есть - группируется в BookingDaoImpl
    public String getEquipmentSummary() {
        if (equipmentSummary == null || equipmentSummary.isBlank()) {
            return "—";
        }
        return equipmentSummary;
    }

    public void setEquipmentSummary(String equipmentSummary) {
        this.equipmentSummary = equipmentSummary;
    }

    public List<Equipment> getEquipmentList() {
        return equipmentList;
    }

    public void setEquipmentList(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList;
    }

    /** Длительность брони в часах. */
    public long getDurationHours() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toHours();
    }

    /** Строка с временным интервалом, например "10:00 — 13:00". */
    public String getTimeRange() {
        return startTime + " — " + endTime;
    }
}
