# Описание архитектуры Photo Studio Booking

Документ для разработчика: структура проекта, назначение модулей и классов,
ключевые решения. Соответствует разделу 10 спецификации.

---

## 1. Общая структура

Приложение — однооконное настольное приложение на **Java 17 + JavaFX**.
Код разделён на слои:

```
интерфейс (ui)  →  сервисы (service)  →  доступ к данным (dao)  →  БД (db)
                                   ↘  модели (model)
```

- **ui** — экраны и диалоги JavaFX (программная вёрстка, без FXML);
- **service** — бизнес-логика (аутентификация, бронирование, расчёт цены, валидация);
- **dao** — доступ к SQLite через JDBC (интерфейсы + реализации в `impl/`);
- **db** — подключение к БД, выполнение `schema.sql`, демо-данные;
- **model** — простые классы-сущности (POJO).

Слой интерфейса обращается к сервисам и DAO, сервисы — к DAO.
Обратных зависимостей нет: например, `dao` ничего не знает про `ui`.

---

## 2. Структура пакетов и классов

```
com.studio.booking
├── Main                      — точка входа JavaFX: проверка БД, инициализация, окно входа
│
├── model                     — классы данных
│   ├── Admin                 — учётная запись администратора
│   ├── Hall                    — зал (название, вместимость, цена, описание)
│   ├── Equipment               — дополнительное оборудование (цена/час)
│   ├── Booking                 — бронирование зала для клиента
│   ├── BookingEquipment        — связь брони и оборудования (модель связи)
│   └── HallLoadReport          — строка отчёта по загрузке зала
│
├── db
│   └── DatabaseManager         — подключение к SQLite, schema.sql, seed-данные
│
├── dao                       — интерфейсы доступа к данным
│   ├── Dao<T>                  — базовый CRUD-интерфейс
│   ├── AdminDao                — проверка пароля администратора
│   ├── HallDao                 — CRUD залов
│   ├── EquipmentDao            — CRUD оборудования
│   ├── BookingDao              — брони, слоты, транзакции, отчёт
│   └── impl/                   — реализации на чистом JDBC
│       ├── AdminDaoImpl
│       ├── HallDaoImpl
│       ├── EquipmentDaoImpl
│       └── BookingDaoImpl
│
├── service                   — бизнес-логика
│   ├── AuthService             — вход администратора по паролю
│   ├── BookingService          — валидация, слоты, расчёт цены, создание/изменение/отмена
│   └── Session                 — флаг авторизации текущей сессии
│
└── ui                        — графический интерфейс (JavaFX)
    ├── LoginView               — экран входа (пароль)
    ├── MainView                — главное окно с вкладками
    ├── BookingDialog           — создание и редактирование брони
    ├── HallDialog              — добавление/редактирование зала
    ├── EquipmentDialog         — добавление/редактирование оборудования
    └── UiUtils                 — стили, диалоги сообщений, настройка сцены
```

Ресурсы:

```
src/main/resources/
├── schema.sql                  — DDL таблиц SQLite
└── styles.css                  — тема оформления интерфейса
```

---

## 3. Модель данных (БД SQLite)

База создаётся автоматически в файле `studio.db` в рабочей директории приложения.

**Таблица `admin`** — учётная запись администратора (одна строка):

| Поле | Тип | Описание |
| --- | --- | --- |
| id | INTEGER, PK | всегда 1 |
| password | TEXT | пароль (в демо — `admin`) |

**Таблица `halls`** — залы студии:

| Поле | Тип | Описание |
| --- | --- | --- |
| id | INTEGER, PK, auto | идентификатор |
| name | TEXT | название зала |
| capacity | INTEGER | вместимость (чел.) |
| base_price_per_hour | REAL | базовая цена за час |
| interior_description | TEXT | описание интерьера |

**Таблица `equipment`** — дополнительное оборудование:

| Поле | Тип | Описание |
| --- | --- | --- |
| id | INTEGER, PK, auto | идентификатор |
| name | TEXT | название |
| price_per_hour | REAL | цена аренды за час |

**Таблица `bookings`** — бронирования:

| Поле | Тип | Описание |
| --- | --- | --- |
| id | INTEGER, PK, auto | идентификатор |
| customer_name | TEXT | имя клиента |
| customer_phone | TEXT | телефон клиента |
| hall_id | INTEGER, FK → halls | зал |
| booking_date | TEXT | дата (ISO) |
| start_time | TEXT | время начала |
| end_time | TEXT | время окончания |
| total_price | REAL | итоговая стоимость |
| status | TEXT | `active` или `cancelled` |

**Таблица `booking_equipment`** — связь «бронь ↔ оборудование» (многие ко многим):

| Поле | Тип | Описание |
| --- | --- | --- |
| booking_id | INTEGER, FK → bookings | бронь |
| equipment_id | INTEGER, FK → equipment | оборудование |

Первичный ключ `(booking_id, equipment_id)`. При удалении брони связи
удаляются каскадно (`ON DELETE CASCADE`).

---

## 4. Ключевые архитектурные решения

### Слой DAO с общим интерфейсом CRUD
Все справочники (залы, оборудование) реализуют базовый интерфейс `Dao<T>`
с методами `findById`, `findAll`, `save`, `update`, `delete`.
`BookingDao` расширяет его специфическими методами: `createWithEquipment`,
`updateWithEquipment`, `findActiveByHallAndDate`, `loadReport` и др.

### Транзакционное сохранение брони (ФР-05, НФР-05)
Создание и обновление брони с оборудованием выполняется в одной JDBC-транзакции
в `BookingDaoImpl.createWithEquipment` / `updateWithEquipment`:
либо сохраняются и бронь, и все связи с оборудованием, либо откатывается всё.

### Проверка доступности слотов
`BookingService` определяет свободные часовые интервалы 09:00–21:00,
исключая пересечения с активными бронями того же зала на ту же дату.
При редактировании текущая бронь исключается из проверки (`excludeBookingId`).

Расчёт цены: `(цена зала/час + сумма цен оборудования/час) × длительность`.

### Валидация бизнес-правил в сервисном слое
Все ограничения (дата не в прошлом, не более 30 дней вперёд, длительность 1–8 ч,
рабочие часы 09:00–21:00, минимум 30 минут до начала «на сегодня») сосредоточены
в `BookingService.validate`. UI перехватывает `IllegalArgumentException`
и показывает сообщение пользователю.

### Программный JavaFX без FXML
Интерфейс строится кодом в классах `*View` и `*Dialog`, стилизация — через
`styles.css` и утилиты `UiUtils`. Это упрощает навигацию по проекту
для курсовой работы без отдельных FXML-файлов.

### Инициализация БД при первом запуске
`DatabaseManager.initialize()` выполняет `schema.sql` и при пустых таблицах
наполняет демо-данными (4 зала, 6 единиц оборудования, пароль `admin`).

### Обработка ошибок БД при старте (НФР-04)
`Main.start()` проверяет подключение (`testConnection`) и инициализацию.
При сбое показывается диалог с понятным текстом, приложение не продолжает работу.

---

## 5. Сборка и зависимости

Сборка — Maven (`pom.xml`). Запуск — `mvn clean javafx:run`.

| Зависимость | Назначение |
| --- | --- |
| `org.openjfx:javafx-controls` | виджеты JavaFX |
| `org.openjfx:javafx-graphics` | графика JavaFX |
| `org.openjfx:javafx-base` | базовые классы JavaFX |
| `org.xerial:sqlite-jdbc` | драйвер SQLite |
| `org.slf4j:slf4j-simple` | логирование (требуется sqlite-jdbc) |
| `org.junit.jupiter:junit-jupiter` (test) | unit-тесты сервисов |

Сторонних веб-фреймворков, Spring и ORM нет — доступ к БД через чистый JDBC и SQL.

---

## 6. Соответствие требованиям

| Требование | Где реализовано |
| --- | --- |
| ФР-01 (аутентификация) | `LoginView`, `AuthService`, `AdminDao` |
| ФР-02 (каталог залов) | `MainView.buildHallsTab`, `HallDao` |
| ФР-03 (создание брони) | `BookingDialog`, `BookingService.createBooking` |
| ФР-04 (расчёт стоимости) | `BookingService.calculatePrice` |
| ФР-05 (транзакция бронь + оборудование) | `BookingDaoImpl.createWithEquipment` |
| ФР-06 (доступные слоты 09:00–21:00) | `BookingService.findAvailableStartTimes/EndTimes` |
| ФР-07 (список бронирований) | `MainView.buildBookingsTab`, `BookingDao.findAllWithDetails` |
| ФР-08 (редактирование active) | `BookingDialog` (режим edit), `BookingService.updateBooking` |
| ФР-09 (отмена active) | `MainView`, `BookingService.cancelBooking` |
| ФР-10 (CRUD залов) | `MainView.buildManageHallsTab`, `HallDialog`, `HallDao` |
| ФР-11 (CRUD оборудования) | `MainView.buildManageEquipmentTab`, `EquipmentDialog`, `EquipmentDao` |
| ФР-12 (отчёт за период) | `MainView.buildReportTab`, `BookingDao.loadReport` |
| ФР-13 (выход) | `MainView.buildHeader` → `LoginView` |
| ФР-14, ФР-15 (ограничения даты/времени) | `BookingService.validate` |
| НФР-04 (ошибка БД при старте) | `Main.start`, `DatabaseManager.testConnection` |
| НФР-05 (атомарность сохранения) | `BookingDaoImpl` (JDBC transaction) |
| НФР-08 (расширяемость отчётов) | отдельный метод `loadReport` в DAO, модель `HallLoadReport` |
