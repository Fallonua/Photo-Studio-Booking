package com.studio.booking.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Менеджер подключения к базе данных SQLite.
 * <p>
 * Отвечает за:
 * <ul>
 *     <li>создание/открытие файла БД;</li>
 *     <li>выполнение скрипта инициализации (schema.sql);</li>
 *     <li>наполнение демонстрационными данными при первом запуске;</li>
 *     <li>выдачу новых соединений (Connection) для DAO.</li>
 * </ul>
 */
public final class DatabaseManager {

    /** Имя файла базы данных (создаётся в рабочей директории). */
    private static final String DB_FILE = "studio.db";
    /** Строка подключения JDBC к SQLite. */
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_FILE;
    private DatabaseManager() {
        // Утилитный класс — экземпляры не нужны.
    }

    /**
     * Возвращает новое соединение с базой данных.
     * Включает поддержку внешних ключей (по умолчанию в SQLite выключена).
     */
    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(JDBC_URL);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        }
        return connection;
    }

    /**
     * Проверка корректности подключения при запуске приложения.
     *
     * @return true, если соединение успешно установлено.
     */
    public static boolean testConnection() {
        try (Connection ignored = getConnection()) {
            return true;
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к БД: " + e.getMessage());
            return false;
        }
    }

    /**
     * Инициализирует базу данных: создаёт таблицы из schema.sql
     * и при необходимости наполняет демонстрационными данными.
     */
    public static void initialize() throws SQLException, IOException {
        String schema = readResource("/schema.sql");
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            // Выполняем скрипт создания таблиц.
            // SQLite (через xerial) выполняет один statement за раз — разбиваем по ';'.
            for (String sql : schema.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        }
        seedDataIfEmpty();
    }

    /**
     * Наполняет таблицы демонстрационными данными, если они пусты.
     * Делает запуск приложения наглядным "из коробки".
     */
    private static void seedDataIfEmpty() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            // Администратор
            if (isEmpty(connection, "admin")) {
                statement.executeUpdate(
                        "INSERT INTO admin (id, password) VALUES (1, 'admin')");
            }
            // Залы
            if (isEmpty(connection, "halls")) {
                statement.executeUpdate(
                        "INSERT INTO halls (name, capacity, base_price_per_hour, interior_description) VALUES " +
                        "('Циклорама Белый', 8, 1500, 'Большой белый циклорамный зал с естественным светом и студийными вспышками.')," +
                        "('Лофт Кирпич', 6, 1200, 'Уютный лофт с кирпичной стеной, деревянным полом и большими окнами.')," +
                        "('Тёмный зал', 4, 1000, 'Чёрный фон, идеален для контрастных портретов и предметной съёмки.')," +
                        "('Интерьерный Прованс', 10, 2000, 'Светлая комната в стиле прованс с мебелью, камином и декором.')");
            }
            // Оборудование
            if (isEmpty(connection, "equipment")) {
                statement.executeUpdate(
                        "INSERT INTO equipment (name, price_per_hour) VALUES " +
                        "('Студийная вспышка Godox', 300)," +
                        "('Софтбокс 120см', 150)," +
                        "('Фон бумажный (рулон)', 200)," +
                        "('Отражатель 5-в-1', 100)," +
                        "('Постоянный свет LED', 250)," +
                        "('Дым-машина', 400)");
            }
        }
    }

    /** Проверяет, что таблица пуста. */
    private static boolean isEmpty(Connection connection, String table) throws SQLException {
        try (Statement st = connection.createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    /** Читает текстовый ресурс из classpath. */
    private static String readResource(String path) throws IOException {
        try (InputStream is = DatabaseManager.class.getResourceAsStream(path)) {

            if (is == null) {
                throw new IOException("Не найден ресурс: " + path);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}
