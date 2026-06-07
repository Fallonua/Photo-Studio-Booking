package com.studio.booking.ui;

import com.studio.booking.model.Booking;
import com.studio.booking.model.Equipment;
import com.studio.booking.model.Hall;
import com.studio.booking.dao.impl.EquipmentDaoImpl;
import com.studio.booking.service.BookingService;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Окно бронирования зала: выбор даты, расписание, время, оборудование,
 * расчёт итоговой цены и подтверждение брони.
 */
public class BookingDialog {

    private final Stage owner;
    private final Hall hall;
    private final Booking existingBooking;
    private final Runnable onBooked;
    private final BookingService bookingService = new BookingService();

    // Элементы, к которым обращаемся из обработчиков.
    private TextField customerNameField;
    private TextField customerPhoneField;
    private DatePicker datePicker;
    private FlowPane scheduleBox;
    private ComboBox<LocalTime> startCombo;
    private ComboBox<LocalTime> endCombo;
    private ListView<Equipment> equipmentList;
    private Label priceLabel;
    private Stage dialog;
    /** Режим создания новой брони. */
    public BookingDialog(Stage owner, Hall hall, Runnable onBooked) {
        this(owner, hall, null, onBooked);
    }

    /** Режим создания или редактирования (existingBooking != null — редактирование). */
    public BookingDialog(Stage owner, Hall hall, Booking existingBooking, Runnable onBooked) {
        this.owner = owner;
        this.hall = hall;
        this.existingBooking = existingBooking;
        this.onBooked = onBooked;
    }

    private boolean isEditMode() {
        return existingBooking != null;
    }

    private Integer excludeBookingId() {
        return isEditMode() ? existingBooking.getId() : null;
    }

    public void show() {
        dialog = new Stage();
        // --- Заголовок и информация о зале ---
        Label title = new Label(isEditMode()
                ? "Редактирование брони: " + hall.getName()
                : "Бронирование: " + hall.getName());
        title.getStyleClass().add("title");
        title.setStyle("-fx-font-size: 22px;");
        Label hallInfo = new Label(String.format(
                "Вместимость: %d чел.   •   Базовая цена: %s ₽/час",
                hall.getCapacity(), fmt(hall.getBasePricePerHour())));
        hallInfo.getStyleClass().add("subtitle");
        Label interior = new Label(hall.getInteriorDescription());
        interior.getStyleClass().add("hint");
        interior.setWrapText(true);
        customerNameField = new TextField();
        customerNameField.setPromptText("Фамилия и имя");
        customerPhoneField = new TextField();
        customerPhoneField.setPromptText("+7 900 000-00-00");
        HBox customerRow = new HBox(12,
                labeled("Имя клиента", customerNameField),
                labeled("Телефон", customerPhoneField));
        HBox.setHgrow(customerRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(customerRow.getChildren().get(1), Priority.ALWAYS);
        // --- Выбор даты ---
        LocalDate initialDate = isEditMode() ? existingBooking.getBookingDate() : LocalDate.now();
        datePicker = new DatePicker(initialDate);
        restrictDates(datePicker);
        datePicker.valueProperty().addListener((o, ov, nv) -> onDateChanged());
        // --- Расписание (занятые/свободные часы) ---
        scheduleBox = new FlowPane(8, 8);
        scheduleBox.setPadding(new Insets(4));
        // --- Время начала / окончания ---
        startCombo = new ComboBox<>();
        startCombo.setPromptText("Начало");
        startCombo.setPrefWidth(140);
        startCombo.valueProperty().addListener((o, ov, nv) -> onStartChanged());
        endCombo = new ComboBox<>();
        endCombo.setPromptText("Окончание");
        endCombo.setPrefWidth(140);
        endCombo.valueProperty().addListener((o, ov, nv) -> updatePrice());
        HBox timeRow = new HBox(12,
                labeled("Время начала", startCombo),
                labeled("Время окончания", endCombo));
        // --- Оборудование (множественный выбор) ---
        equipmentList = new ListView<>();
        equipmentList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        equipmentList.setItems(FXCollections.observableArrayList(new EquipmentDaoImpl().findAll()));
        equipmentList.setPrefHeight(150);
        equipmentList.getSelectionModel().getSelectedItems()
                .addListener((javafx.collections.ListChangeListener<Equipment>) c -> updatePrice());
        Label equipHint = new Label("Удерживайте Ctrl для выбора нескольких позиций");
        equipHint.getStyleClass().add("hint");
        // --- Цена и кнопки ---
        priceLabel = new Label("Итого: 0 ₽");
        priceLabel.getStyleClass().add("price-label");
        Button bookBtn = new Button(isEditMode() ? "Сохранить изменения" : "Забронировать");
        bookBtn.getStyleClass().add("button-primary");
        bookBtn.setOnAction(e -> {
            if (isEditMode()) {
                doUpdate();
            } else {
                doBooking();
            }
        });
        Button cancelBtn = new Button("Закрыть");
        cancelBtn.setOnAction(e -> dialog.close());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(12, priceLabel, spacer, cancelBtn, bookBtn);
        bottom.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(14,
                title, hallInfo, interior,
                separator(),
                sectionLabel("Данные клиента"),
                customerRow,
                separator(),
                labeled("Дата бронирования", datePicker),
                sectionLabel("Расписание на выбранный день"),
                scheduleBox,
                timeRow,
                sectionLabel("Дополнительное оборудование"),
                equipmentList, equipHint,
                separator(),
                bottom);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(28));
        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #f4f5fb; -fx-background: #f4f5fb;");
        scroll.setPadding(new Insets(20));
        Scene scene = new Scene(scroll, 640, 780);
        UiUtils.applyStyles(scene);
        dialog.setScene(scene);
        dialog.setTitle(isEditMode() ? "Редактирование брони" : "Бронирование зала");
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        if (isEditMode()) {
            initializeEditValues();
        } else {
            onDateChanged();
        }
        dialog.showAndWait();
    }

    /** Предзаполняет поля при редактировании существующей брони. */
    private void initializeEditValues() {
        customerNameField.setText(existingBooking.getCustomerName());
        customerPhoneField.setText(existingBooking.getCustomerPhone());
        onDateChanged();
        LocalTime savedStart = existingBooking.getStartTime();
        LocalTime savedEnd = existingBooking.getEndTime();
        if (startCombo.getItems().contains(savedStart)) {
            startCombo.setValue(savedStart);
        } else if (!startCombo.getItems().isEmpty()) {
            startCombo.setValue(startCombo.getItems().get(0));
        }
        onStartChanged();
        if (endCombo.getItems().contains(savedEnd)) {
            endCombo.setValue(savedEnd);
        } else if (!endCombo.getItems().isEmpty()) {
            endCombo.setValue(endCombo.getItems().get(endCombo.getItems().size() - 1));
        }
        List<Equipment> selectedEquipment = bookingService.getBookingEquipment(existingBooking.getId());
        for (Equipment eq : selectedEquipment) {
            int index = equipmentList.getItems().indexOf(eq);
            if (index >= 0) {
                equipmentList.getSelectionModel().select(index);
            } else {
                // Сравнение по id, если equals не совпал
                for (int i = 0; i < equipmentList.getItems().size(); i++) {
                    if (equipmentList.getItems().get(i).getId() == eq.getId()) {
                        equipmentList.getSelectionModel().select(i);
                        break;
                    }
                }
            }
        }
        updatePrice();
    }

    // ============================================================
    //  Обработчики
    // ============================================================
    /** Перестроить расписание и доступные времена начала при смене даты. */
    private void onDateChanged() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            return;
        }
        renderSchedule(date);
        List<LocalTime> starts = bookingService.findAvailableStartTimes(
                hall.getId(), date, excludeBookingId());
        startCombo.setItems(FXCollections.observableArrayList(starts));
        startCombo.getSelectionModel().clearSelection();
        startCombo.setValue(null);
        endCombo.setItems(FXCollections.observableArrayList());
        endCombo.setValue(null);
        updatePrice();
    }

    /** Обновить доступные времена окончания при смене начала. */
    private void onStartChanged() {
        LocalDate date = datePicker.getValue();
        LocalTime start = startCombo.getValue();
        endCombo.setValue(null);
        if (date == null || start == null) {
            endCombo.setItems(FXCollections.observableArrayList());
            updatePrice();
            return;
        }
        List<LocalTime> ends = bookingService.findAvailableEndTimes(
                hall.getId(), date, start, excludeBookingId());
        endCombo.setItems(FXCollections.observableArrayList(ends));
        updatePrice();
    }

    /** Пересчитать и показать итоговую цену. */
    private void updatePrice() {
        LocalTime start = startCombo.getValue();
        LocalTime end = endCombo.getValue();
        if (start == null || end == null || !end.isAfter(start)) {
            priceLabel.setText("Итого: 0 ₽");
            return;
        }
        List<Equipment> selected = new ArrayList<>(equipmentList.getSelectionModel().getSelectedItems());
        double price = bookingService.calculatePrice(hall, start, end, selected);
        long hours = java.time.Duration.between(start, end).toHours();
        priceLabel.setText(String.format("Итого: %s ₽  (%d ч.)", fmt(price), hours));
    }

    /** Создать бронь. */
    private void doBooking() {
        String customerName = customerNameField.getText();
        String customerPhone = customerPhoneField.getText();
        LocalDate date = datePicker.getValue();
        LocalTime start = startCombo.getValue();
        LocalTime end = endCombo.getValue();
        if (customerName == null || customerName.isBlank()
                || customerPhone == null || customerPhone.isBlank()) {
            UiUtils.warn("Укажите имя и телефон клиента.");
            return;
        }
        if (date == null || start == null || end == null) {
            UiUtils.warn("Выберите дату, время начала и окончания.");
            return;
        }
        List<Equipment> selected = new ArrayList<>(equipmentList.getSelectionModel().getSelectedItems());
        try {
            Booking booking = bookingService.createBooking(
                    customerName, customerPhone, hall, date, start, end, selected);
            UiUtils.info(String.format(
                    "Бронь создана!\nКлиент: %s (%s)\nЗал: %s\nДата: %s\nВремя: %s — %s\nСумма: %s ₽",
                    customerName.trim(), customerPhone.trim(),
                    hall.getName(), date, start, end, fmt(booking.getTotalPrice())));
            if (onBooked != null) {
                onBooked.run();
            }
            dialog.close();
        } catch (IllegalArgumentException ex) {
            UiUtils.error(ex.getMessage());
        }
    }

    /** Сохранить изменения существующей брони. */
    private void doUpdate() {
        String customerName = customerNameField.getText();
        String customerPhone = customerPhoneField.getText();
        LocalDate date = datePicker.getValue();
        LocalTime start = startCombo.getValue();
        LocalTime end = endCombo.getValue();
        if (customerName == null || customerName.isBlank()
                || customerPhone == null || customerPhone.isBlank()) {
            UiUtils.warn("Укажите имя и телефон клиента.");
            return;
        }
        if (date == null || start == null || end == null) {
            UiUtils.warn("Выберите дату, время начала и окончания.");
            return;
        }
        List<Equipment> selected = new ArrayList<>(equipmentList.getSelectionModel().getSelectedItems());
        try {
            Booking booking = bookingService.updateBooking(
                    existingBooking.getId(),
                    customerName, customerPhone,
                    hall, date, start, end, selected);
            UiUtils.info(String.format(
                    "Бронь обновлена!\nКлиент: %s (%s)\nЗал: %s\nДата: %s\nВремя: %s — %s\nСумма: %s ₽",
                    customerName.trim(), customerPhone.trim(),
                    hall.getName(), date, start, end, fmt(booking.getTotalPrice())));
            if (onBooked != null) {
                onBooked.run();
            }
            dialog.close();
        } catch (IllegalArgumentException ex) {
            UiUtils.error(ex.getMessage());
        }
    }

    // ============================================================
    //  Отрисовка расписания
    // ============================================================
    /** Рисует часовые ячейки 9..21, помечая занятые красным. */
    private void renderSchedule(LocalDate date) {
        scheduleBox.getChildren().clear();
        List<LocalTime> freeStarts = bookingService.findAvailableStartTimes(
                hall.getId(), date, excludeBookingId());
        for (int hour = BookingService.WORK_START_HOUR; hour < BookingService.WORK_END_HOUR; hour++) {
            LocalTime slotStart = LocalTime.of(hour, 0);
            boolean busy = !freeStarts.contains(slotStart);
            Label chip = new Label(String.format("%02d:00", hour));
            chip.setMinWidth(58);
            chip.setAlignment(Pos.CENTER);
            chip.getStyleClass().add(busy ? "badge-cancelled" : "badge-active");
            chip.setStyle(chip.getStyle() + "-fx-padding: 8 4 8 4; -fx-font-size: 13px;");
            scheduleBox.getChildren().add(chip);
        }
    }

    // ============================================================
    //  Вспомогательное
    // ============================================================
    /** Ограничивает выбор дат: нельзя в прошлом и более чем на 30 дней вперёд. */
    private void restrictDates(DatePicker picker) {
        picker.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                LocalDate today = LocalDate.now();
                if (item.isBefore(today) || item.isAfter(today.plusDays(BookingService.MAX_DAYS_AHEAD))) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f0f0f5;");
                }
            }
        });
    }

    private VBox labeled(String label, javafx.scene.Node node) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        return new VBox(4, l, node);
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    private Region separator() {
        Region r = new Region();
        r.setMinHeight(1);
        r.setStyle("-fx-background-color: #ececf4;");
        return r;
    }

    private String fmt(double value) {
        return String.format("%,.0f", value);
    }
}
