package com.studio.booking.ui;

import com.studio.booking.dao.EquipmentDao;
import com.studio.booking.dao.HallDao;
import com.studio.booking.dao.impl.EquipmentDaoImpl;
import com.studio.booking.dao.impl.HallDaoImpl;
import com.studio.booking.model.Booking;
import com.studio.booking.model.Equipment;
import com.studio.booking.model.Hall;
import com.studio.booking.model.HallLoadReport;
import com.studio.booking.service.BookingService;
import com.studio.booking.service.Session;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * Главное окно приложения после входа администратора.
 */
public class MainView {

    private final Stage stage;
    private final HallDao hallDao = new HallDaoImpl();

    private final EquipmentDao equipmentDao = new EquipmentDaoImpl();

    private final BookingService bookingService = new BookingService();
    private TabPane tabPane;
    private Tab homeTab;
    private Tab hallsTab;
    private Tab bookingsTab;
    private Tab manageHallsTab;
    private Tab manageEquipmentTab;
    private Tab reportTab;
    private TableView<Hall> hallsTable;
    private TableView<Booking> bookingsTable;
    private TableView<Hall> manageHallsTable;
    private TableView<Equipment> manageEquipmentTable;

    public MainView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        TabPane tabPane = new TabPane();
        this.tabPane = tabPane;
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setPadding(new Insets(8, 16, 16, 16));
        hallsTab = buildHallsTab();
        bookingsTab = buildBookingsTab();
        manageHallsTab = buildManageHallsTab();
        manageEquipmentTab = buildManageEquipmentTab();
        reportTab = buildReportTab();
        homeTab = buildHomeTab();
        tabPane.getTabs().addAll(
                homeTab, hallsTab, bookingsTab,
                manageHallsTab, manageEquipmentTab, reportTab);
        tabPane.getSelectionModel().select(homeTab);
        root.setCenter(tabPane);
        Scene scene = new Scene(root, 1200, 760);
        UiUtils.setScene(stage, scene, "Photo Studio — Главное окно", 1200, 760);
        stage.show();
    }

    private HBox buildHeader() {
        Label title = new Label("Photo Studio");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #6c5ce7;");
        Label greeting = new Label("  •  Администратор");
        greeting.getStyleClass().add("subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button homeBtn = new Button("На главную");
        homeBtn.setOnAction(e -> navigateTo(homeTab));
        Button logout = new Button("Выход");
        logout.getStyleClass().add("button-danger");
        logout.setOnAction(e -> {
            Session.clear();
            new LoginView(stage).show();
        });
        HBox header = new HBox(8, title, greeting, spacer, homeBtn, logout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 8, 20));
        return header;
    }

    private Tab buildHomeTab() {
        Label welcome = new Label("Добро пожаловать!");
        welcome.getStyleClass().add("title");
        Label subtitle = new Label("Панель администратора. Выберите раздел для работы.");
        subtitle.getStyleClass().add("subtitle");
        long activeBookings = bookingService.getAllBookings().stream()
                .filter(b -> "active".equals(b.getStatus())).count();
        int hallsCount = hallDao.findAll().size();
        HBox stats = new HBox(16,
                statCard(String.valueOf(hallsCount), "залов доступно"),
                statCard(String.valueOf(activeBookings), "активных броней"));
        Label menuTitle = new Label("Разделы приложения");
        menuTitle.getStyleClass().add("section-title");
        GridPane menuGrid = new GridPane();
        menuGrid.setHgap(16);
        menuGrid.setVgap(16);
        menuGrid.setPadding(new Insets(4, 0, 0, 0));
        int col = 0;
        int row = 0;
        col = addMenuRow(menuGrid, row, col,
                menuCard("🏛", "Создать бронь", "Выбор зала и оформление брони для клиента", hallsTab),
                menuCard("📅", "Бронирования", "Все записи, редактирование и отмена", bookingsTab),
                menuCard("📊", "Отчёт по загрузке", "Статистика залов за период", reportTab));
        row++;
        col = 0;
        col = addMenuRow(menuGrid, row, col,
                menuCard("⚙", "Управление залами", "Добавление и редактирование залов", manageHallsTab),
                menuCard("🔧", "Управление оборудованием", "Каталог оборудования студии", manageEquipmentTab));
        VBox content = new VBox(20, welcome, subtitle, stats, menuTitle, menuGrid);
        content.setPadding(new Insets(24, 12, 12, 12));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return tab("Главное меню", scroll);
    }

    private VBox statCard(String value, String caption) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("stat-caption");
        VBox card = new VBox(4, valueLabel, captionLabel);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(140);
        return card;
    }

    private VBox menuCard(String icon, String title, String description, Tab targetTab) {
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("menu-card-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("menu-card-title");
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("menu-card-desc");
        descLabel.setWrapText(true);
        VBox card = new VBox(8, iconLabel, titleLabel, descLabel);
        card.getStyleClass().add("menu-card");
        card.setPrefWidth(240);
        card.setMinHeight(130);
        card.setOnMouseClicked(e -> navigateTo(targetTab));
        return card;
    }

    private void navigateTo(Tab tab) {
        if (tab != null && tabPane != null) {
            tabPane.getSelectionModel().select(tab);
        }
    }

    private int addMenuRow(GridPane grid, int row, int startCol, VBox... cards) {
        int col = startCol;
        for (VBox card : cards) {
            grid.add(card, col++, row);
        }
        return col;
    }

    private Tab buildHallsTab() {
        hallsTable = new TableView<>();
        hallsTable.getColumns().add(col("Название", Hall::getName, 200));
        hallsTable.getColumns().add(col("Вместимость", h -> h.getCapacity() + " чел.", 110));
        hallsTable.getColumns().add(col("Цена/час", h -> money(h.getBasePricePerHour()), 110));
        hallsTable.getColumns().add(col("Интерьер", Hall::getInteriorDescription, 450));
        VBox.setVgrow(hallsTable, Priority.ALWAYS);
        Button bookBtn = new Button("Выбрать и забронировать");
        bookBtn.getStyleClass().add("button-primary");
        bookBtn.setOnAction(e -> {
            Hall selected = hallsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                UiUtils.warn("Выберите зал из списка.");
                return;
            }
            new BookingDialog(stage, selected, this::refreshBookings).show();
        });
        refreshHalls();
        Label hint = new Label("Выберите зал и нажмите кнопку, чтобы забронировать время для клиента.");
        hint.getStyleClass().add("hint");
        VBox box = wrapTab("Доступные залы", hallsTable, new HBox(bookBtn), hint);
        return tab("Создать бронь", box);
    }

    private void refreshHalls() {
        hallsTable.setItems(FXCollections.observableArrayList(hallDao.findAll()));
    }

    private Tab buildBookingsTab() {
        bookingsTable = new TableView<>();
        bookingsTable.getColumns().add(col("ID", b -> String.valueOf(b.getId()), 50));
        bookingsTable.getColumns().add(col("Клиент", Booking::getCustomerName, 160));
        bookingsTable.getColumns().add(col("Телефон", Booking::getCustomerPhone, 140));
        bookingsTable.getColumns().add(col("Зал", Booking::getHallName, 160));
        bookingsTable.getColumns().add(col("Дата", b -> b.getBookingDate().toString(), 100));
        bookingsTable.getColumns().add(col("Время", Booking::getTimeRange, 120));
        bookingsTable.getColumns().add(col("Оборудование", Booking::getEquipmentSummary, 240));
        bookingsTable.getColumns().add(col("Сумма", b -> money(b.getTotalPrice()), 100));
        bookingsTable.getColumns().add(statusColumn());
        VBox.setVgrow(bookingsTable, Priority.ALWAYS);
        Button editBtn = new Button("Редактировать");
        editBtn.getStyleClass().add("button-primary");
        editBtn.setOnAction(e -> openEditBooking(
                bookingsTable.getSelectionModel().getSelectedItem(), this::refreshBookings));
        Button cancelBtn = new Button("Отменить бронь");
        cancelBtn.getStyleClass().add("button-danger");
        cancelBtn.setOnAction(e -> {
            Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                UiUtils.warn("Выберите бронь для отмены.");
                return;
            }
            if ("cancelled".equals(selected.getStatus())) {
                UiUtils.warn("Эта бронь уже отменена.");
                return;
            }
            if (UiUtils.confirm("Отменить бронь зала \"" + selected.getHallName() + "\" для "
                    + selected.getCustomerName() + " на " + selected.getBookingDate() + "?")) {
                bookingService.cancelBooking(selected.getId());
                refreshBookings();
            }
        });
        refreshBookings();
        VBox box = wrapTab("Бронирования", bookingsTable, new HBox(10, editBtn, cancelBtn));
        return tab("Бронирования", box);
    }

    private void refreshBookings() {
        bookingsTable.setItems(FXCollections.observableArrayList(bookingService.getAllBookings()));
    }

    private void openEditBooking(Booking booking, Runnable onSaved) {
        if (booking == null) {
            UiUtils.warn("Выберите бронь для редактирования.");
            return;
        }
        if ("cancelled".equals(booking.getStatus())) {
            UiUtils.warn("Отменённую бронь нельзя редактировать.");
            return;
        }
        Hall hall = hallDao.findById(booking.getHallId()).orElse(null);
        if (hall == null) {
            UiUtils.error("Зал для этой брони не найден.");
            return;
        }
        new BookingDialog(stage, hall, booking, onSaved).show();
    }

    private Tab buildManageHallsTab() {
        manageHallsTable = new TableView<>();
        manageHallsTable.getColumns().add(col("ID", h -> String.valueOf(h.getId()), 50));
        manageHallsTable.getColumns().add(col("Название", Hall::getName, 180));
        manageHallsTable.getColumns().add(col("Вместимость", h -> String.valueOf(h.getCapacity()), 100));
        manageHallsTable.getColumns().add(col("Цена/час", h -> money(h.getBasePricePerHour()), 110));
        manageHallsTable.getColumns().add(col("Интерьер", Hall::getInteriorDescription, 380));
        VBox.setVgrow(manageHallsTable, Priority.ALWAYS);
        Button addBtn = primary("Добавить");
        Button editBtn = new Button("Редактировать");
        Button delBtn = new Button("Удалить");
        delBtn.getStyleClass().add("button-danger");
        addBtn.setOnAction(e -> new HallDialog(stage, null, this::refreshManageHalls).show());
        editBtn.setOnAction(e -> {
            Hall sel = manageHallsTable.getSelectionModel().getSelectedItem();
            if (sel == null) { UiUtils.warn("Выберите зал."); return; }
            new HallDialog(stage, sel, this::refreshManageHalls).show();
        });
        delBtn.setOnAction(e -> {
            Hall sel = manageHallsTable.getSelectionModel().getSelectedItem();
            if (sel == null) { UiUtils.warn("Выберите зал."); return; }
            if (UiUtils.confirm("Удалить зал \"" + sel.getName() + "\"?")) {
                try {
                    hallDao.delete(sel.getId());
                    refreshManageHalls();
                    refreshHalls();
                } catch (RuntimeException ex) {
                    UiUtils.error("Не удалось удалить зал. Возможно, по нему есть бронирования.");
                }
            }
        });
        refreshManageHalls();
        HBox buttons = new HBox(10, addBtn, editBtn, delBtn);
        VBox box = wrapTab("Управление залами", manageHallsTable, buttons);
        return tab("Управление залами", box);
    }

    private void refreshManageHalls() {
        manageHallsTable.setItems(FXCollections.observableArrayList(hallDao.findAll()));
    }

    private Tab buildManageEquipmentTab() {
        manageEquipmentTable = new TableView<>();
        manageEquipmentTable.getColumns().add(col("ID", e -> String.valueOf(e.getId()), 50));
        manageEquipmentTable.getColumns().add(col("Название", Equipment::getName, 320));
        manageEquipmentTable.getColumns().add(col("Цена/час", e -> money(e.getPricePerHour()), 140));
        VBox.setVgrow(manageEquipmentTable, Priority.ALWAYS);
        Button addBtn = primary("Добавить");
        Button editBtn = new Button("Редактировать");
        Button delBtn = new Button("Удалить");
        delBtn.getStyleClass().add("button-danger");
        addBtn.setOnAction(e -> new EquipmentDialog(stage, null, this::refreshManageEquipment).show());
        editBtn.setOnAction(e -> {
            Equipment sel = manageEquipmentTable.getSelectionModel().getSelectedItem();
            if (sel == null) { UiUtils.warn("Выберите оборудование."); return; }
            new EquipmentDialog(stage, sel, this::refreshManageEquipment).show();
        });
        delBtn.setOnAction(e -> {
            Equipment sel = manageEquipmentTable.getSelectionModel().getSelectedItem();
            if (sel == null) { UiUtils.warn("Выберите оборудование."); return; }
            if (UiUtils.confirm("Удалить \"" + sel.getName() + "\"?")) {
                try {
                    equipmentDao.delete(sel.getId());
                    refreshManageEquipment();
                } catch (RuntimeException ex) {
                    UiUtils.error("Не удалось удалить. Возможно, оборудование используется в бронированиях.");
                }
            }
        });
        refreshManageEquipment();
        HBox buttons = new HBox(10, addBtn, editBtn, delBtn);
        VBox box = wrapTab("Управление оборудованием", manageEquipmentTable, buttons);
        return tab("Управление оборудованием", box);
    }

    private void refreshManageEquipment() {
        manageEquipmentTable.setItems(FXCollections.observableArrayList(equipmentDao.findAll()));
    }

    private Tab buildReportTab() {
        DatePicker from = new DatePicker(LocalDate.now().minusDays(7));
        DatePicker to = new DatePicker(LocalDate.now().plusDays(30));
        TableView<HallLoadReport> table = new TableView<>();
        table.getColumns().add(col("Зал", HallLoadReport::getHallName, 220));
        table.getColumns().add(col("Бронирований", r -> String.valueOf(r.getBookingsCount()), 150));
        table.getColumns().add(col("Часов", r -> String.valueOf(r.getTotalHours()), 120));
        table.getColumns().add(col("Выручка", r -> money(r.getRevenue()), 150));
        VBox.setVgrow(table, Priority.ALWAYS);
        Button generate = primary("Сформировать");
        generate.setOnAction(e -> {
            if (from.getValue() == null || to.getValue() == null) {
                UiUtils.warn("Укажите обе даты периода.");
                return;
            }
            if (from.getValue().isAfter(to.getValue())) {
                UiUtils.warn("Дата \"от\" не может быть позже даты \"до\".");
                return;
            }
            List<HallLoadReport> rows = new com.studio.booking.dao.impl.BookingDaoImpl()
                    .loadReport(from.getValue(), to.getValue());
            table.setItems(FXCollections.observableArrayList(rows));
        });
        HBox controls = new HBox(12,
                labeledInline("Дата от:", from),
                labeledInline("Дата до:", to),
                generate);
        controls.setAlignment(Pos.CENTER_LEFT);
        VBox box = wrapTab("Отчёт по загрузке залов", controls, table);
        return tab("Отчёт по загрузке", box);
    }

    private <S> TableColumn<S, String> col(String title, Function<S, String> extractor, double width) {
        TableColumn<S, String> c = new TableColumn<>(title);
        c.setCellValueFactory(data -> new SimpleStringProperty(
                safe(extractor.apply(data.getValue()))));
        c.setPrefWidth(width);
        return c;
    }

    private TableColumn<Booking, String> statusColumn() {
        TableColumn<Booking, String> c = new TableColumn<>("Статус");
        c.setPrefWidth(120);
        c.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        c.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label badge = new Label("active".equals(status) ? "Активна" : "Отменена");
                badge.getStyleClass().add("active".equals(status) ? "badge-active" : "badge-cancelled");
                setGraphic(badge);
                setText(null);
            }
        });
        return c;
    }

    private Tab tab(String title, javafx.scene.Node content) {
        return new Tab(title, content);
    }

    private VBox wrapTab(String title, javafx.scene.Node... nodes) {
        Label header = new Label(title);
        header.getStyleClass().add("section-title");
        VBox box = new VBox(14);
        box.setPadding(new Insets(20, 6, 6, 6));
        box.getChildren().add(header);
        for (javafx.scene.Node node : nodes) {
            box.getChildren().add(node);
        }
        return box;
    }

    private HBox labeledInline(String label, javafx.scene.Node node) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        HBox h = new HBox(6, l, node);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private Button primary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("button-primary");
        return b;
    }

    private String money(double value) {
        return String.format("%,.0f ₽", value);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
