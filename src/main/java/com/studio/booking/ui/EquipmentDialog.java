package com.studio.booking.ui;

import com.studio.booking.dao.EquipmentDao;
import com.studio.booking.dao.impl.EquipmentDaoImpl;
import com.studio.booking.model.Equipment;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Диалог добавления/редактирования оборудования.
 */
public class EquipmentDialog {

    private final Stage owner;
    private final Equipment equipment;
    private final Runnable onSaved;
    private final EquipmentDao equipmentDao = new EquipmentDaoImpl();

    public EquipmentDialog(Stage owner, Equipment equipment, Runnable onSaved) {
        this.owner = owner;
        this.equipment = equipment;
        this.onSaved = onSaved;
    }

    public void show() {
        boolean editing = equipment != null;
        TextField nameField = new TextField(editing ? equipment.getName() : "");
        nameField.setPromptText("Название оборудования");
        TextField priceField = new TextField(editing ? String.valueOf(equipment.getPricePerHour()) : "");
        priceField.setPromptText("Например, 300");
        Label title = new Label(editing ? "Редактирование оборудования" : "Новое оборудование");
        title.getStyleClass().add("section-title");
        Button save = new Button("Сохранить");
        save.getStyleClass().add("button-primary");
        Button cancel = new Button("Отмена");
        Stage dialog = new Stage();
        save.setOnAction(e -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    UiUtils.warn("Введите название оборудования.");
                    return;
                }
                double price = Double.parseDouble(priceField.getText().trim().replace(",", "."));
                if (price < 0) {
                    UiUtils.warn("Цена не может быть отрицательной.");
                    return;
                }
                if (editing) {
                    equipment.setName(name);
                    equipment.setPricePerHour(price);
                    equipmentDao.update(equipment);
                } else {
                    equipmentDao.save(new Equipment(0, name, price));
                }
                onSaved.run();
                dialog.close();
            } catch (NumberFormatException ex) {
                UiUtils.warn("Цена должна быть числом.");
            }
        });
        cancel.setOnAction(e -> dialog.close());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, cancel, save);
        VBox card = new VBox(10,
                title,
                label("НАЗВАНИЕ"), nameField,
                label("ЦЕНА ЗА ЧАС"), priceField,
                buttons);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24));
        Scene scene = new Scene(card, 400, 320);
        UiUtils.applyStyles(scene);
        dialog.setScene(scene);
        dialog.setTitle(editing ? "Редактирование оборудования" : "Новое оборудование");
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
