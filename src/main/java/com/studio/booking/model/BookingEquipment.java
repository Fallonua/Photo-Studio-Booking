package com.studio.booking.model;

/**
 * Модель связи "бронирование <-> оборудование".
 * Соответствует строке таблицы booking_equipment.
 */
public class BookingEquipment {

    private int bookingId;
    private int equipmentId;

    public BookingEquipment() {
    }

    public BookingEquipment(int bookingId, int equipmentId) {
        this.bookingId = bookingId;
        this.equipmentId = equipmentId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
    }
}
