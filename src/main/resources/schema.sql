-- ============================================================
--  Скрипт инициализации базы данных (SQLite)
--  Создаёт таблицы и наполняет их демонстрационными данными.
--  Все CREATE используют IF NOT EXISTS, поэтому повторный
--  запуск не вызовет ошибок.
-- ============================================================

-- Учётная запись администратора (одна запись)
CREATE TABLE IF NOT EXISTS admin (
    id       INTEGER PRIMARY KEY CHECK (id = 1),
    password TEXT NOT NULL
);

-- Залы (фотостудии)
CREATE TABLE IF NOT EXISTS halls (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    name                 TEXT NOT NULL,
    capacity             INTEGER NOT NULL,
    base_price_per_hour  REAL NOT NULL,
    interior_description TEXT
);

-- Дополнительное оборудование
CREATE TABLE IF NOT EXISTS equipment (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    name           TEXT NOT NULL,
    price_per_hour REAL NOT NULL
);

-- Бронирования
CREATE TABLE IF NOT EXISTS bookings (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_name  TEXT NOT NULL,
    customer_phone TEXT NOT NULL,
    hall_id        INTEGER NOT NULL,
    booking_date   TEXT NOT NULL,
    start_time     TEXT NOT NULL,
    end_time       TEXT NOT NULL,
    total_price    REAL NOT NULL,
    status         TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'cancelled')),
    FOREIGN KEY (hall_id) REFERENCES halls(id)
);

-- Связь "бронирование <-> оборудование" (многие ко многим)
CREATE TABLE IF NOT EXISTS booking_equipment (
    booking_id   INTEGER NOT NULL,
    equipment_id INTEGER NOT NULL,
    PRIMARY KEY (booking_id, equipment_id),
    FOREIGN KEY (booking_id)   REFERENCES bookings(id)   ON DELETE CASCADE,
    FOREIGN KEY (equipment_id) REFERENCES equipment(id)
);
