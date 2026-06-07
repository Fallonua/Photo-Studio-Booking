package com.studio.booking.service;

import com.studio.booking.model.Equipment;
import com.studio.booking.model.Hall;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingServiceTest {

    // Сервис бронирования — тестируемый объект
    private final BookingService bookingService = new BookingService();

    @Test
    void calculatePrice_sumsHallBasePriceAndEquipmentPerHour() {
        // Зал: базовая цена 1000 ₽/час
        Hall hall = new Hall(1, "Зал A", 10, 1000.0, "Описание");
        // Дополнительное оборудование: 500 ₽/час
        Equipment light = new Equipment(1, "Свет", 500.0);
        // Интервал брони: с 10:00 до 13:00 (3 часа)
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(13, 0);

        // Итог: (1000 + 500) × 3 = 4500 ₽
        double price = bookingService.calculatePrice(hall, start, end, List.of(light));

        // Проверяем, что рассчитанная сумма совпадает с ожидаемой
        assertEquals(4500.0, price);
    }
}
