package com.studio.booking.model;

/**
 * Модель дополнительного оборудования.
 */
public class Equipment {

    private int id;
    private String name;
    private double pricePerHour;

    public Equipment() {
    }

    public Equipment(int id, String name, double pricePerHour) {
        this.id = id;
        this.name = name;
        this.pricePerHour = pricePerHour;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(double pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    @Override
    public String toString() {
        return name + " (" + String.format("%.0f", pricePerHour) + " ₽/час)";
    }
}
