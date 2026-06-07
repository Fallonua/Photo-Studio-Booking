package com.studio.booking.ui;

import com.studio.booking.dao.HallDao;
import com.studio.booking.dao.impl.HallDaoImpl;
import com.studio.booking.model.Hall;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Диалог добавления/редактирования зала.
 * Если hall == null — режим добавления, иначе — редактирования.
 */
public class HallDialog {

    private final Stage owner;
    private final Hall hall;
    private final Runnable onSaved;
    private final HallDao hallDao = new HallDaoImpl();

    public HallDialog(Stage owner, Hall hall, Runnable onSaved) {
        this.owner = owner;
        this.hall = hall;
        this.onSaved = onSaved;
    }

    public void show() {
        boolean editing = hall != null;
        TextField nameField = new TextField(editing ? hall.getName() : "");
        nameField.setPromptText("Название зала");
        TextField capacityField = new TextField(editing ? String.valueOf(hall.getCapacity()) : "");
        capacityField.setPromptText("Например, 8");
        TextField priceField = new TextField(editing ? String.valueOf(hall.getBasePricePerHour()) : "");
        priceField.setPromptText("Например, 1500");
        TextArea interiorField = new TextArea(editing ? hall.getInteriorDescription() : "");
        interiorField.setPromptText("Описание интерьера");
        interiorField.setPrefRowCount(3);
        interiorField.setWrapText(true);
        Label title = new Label(editing ? "Редактирование зала" : "Новый зал");
        title.getStyleClass().add("section-title");
        Button save = new Button("Сохранить");
        save.getStyleClass().add("button-primary");
        Button cancel = new Button("Отмена");
        Stage dialog = new Stage();
        save.setOnAction(e -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    UiUtils.warn("Введите название зала.");
                    return;
                }
                int capacity = Integer.parseInt(capacityField.getText().trim());
                double price = Double.parseDouble(priceField.getText().trim().replace(",", "."));
                if (capacity <= 0 || price <= 0) {
                    UiUtils.warn("Вместимость и цена должны быть положительными.");
                    return;
                }
                if (editing) {
                    hall.setName(name);
                    hall.setCapacity(capacity);
                    hall.setBasePricePerHour(price);
                    hall.setInteriorDescription(interiorField.getText().trim());
                    hallDao.update(hall);
                } else {
                    Hall newHall = new Hall(0, name, capacity, price, interiorField.getText().trim());
                    hallDao.save(newHall);
                }
                onSaved.run();
                dialog.close();
            } catch (NumberFormatException ex) {
                UiUtils.warn("Вместимость и цена должны быть числами.");
            }
        });
        cancel.setOnAction(e -> dialog.close());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, cancel, save);
        VBox card = new VBox(10,
                title,
                label("НАЗВАНИЕ"), nameField,
                label("ВМЕСТИМОСТЬ (чел.)"), capacityField,
                label("БАЗОВАЯ ЦЕНА ЗА ЧАС"), priceField,
                label("ИНТЕРЬЕР"), interiorField,
                buttons);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24));
        Scene scene = new Scene(card, 440, 540);
        UiUtils.applyStyles(scene);
        dialog.setScene(scene);
        dialog.setTitle(editing ? "Редактирование зала" : "Новый зал");
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.showAndWait();
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }
}
