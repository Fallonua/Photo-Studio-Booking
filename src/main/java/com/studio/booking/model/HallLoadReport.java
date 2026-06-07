package com.studio.booking.model;

/**
 * Строка отчёта по загрузке залов за период.
 */
public class HallLoadReport {

    private final String hallName;
    private final int bookingsCount;
    private final long totalHours;
    private final double revenue;

    public HallLoadReport(String hallName, int bookingsCount, long totalHours, double revenue) {
        this.hallName = hallName;
        this.bookingsCount = bookingsCount;
        this.totalHours = totalHours;
        this.revenue = revenue;
    }

    public String getHallName() {
        return hallName;
    }

    public int getBookingsCount() {
        return bookingsCount;
    }

    public long getTotalHours() {
        return totalHours;
    }

    public double getRevenue() {
        return revenue;
    }
}
