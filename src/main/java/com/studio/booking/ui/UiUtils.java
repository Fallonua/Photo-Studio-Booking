package com.studio.booking.ui;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Вспомогательные методы для UI: применение стилей, диалоги.
 */
public final class UiUtils {

    /** Путь к таблице стилей в classpath. */
    public static final String STYLESHEET = "/styles.css";

    private UiUtils() {
    }

    /** Подключает общую таблицу стилей к сцене. */
    public static void applyStyles(Scene scene) {
        var url = UiUtils.class.getResource(STYLESHEET);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }

    public static void info(String message) {
        show(Alert.AlertType.INFORMATION, "Информация", message);
    }

    public static void error(String message) {
        show(Alert.AlertType.ERROR, "Ошибка", message);
    }

    public static void warn(String message) {
        show(Alert.AlertType.WARNING, "Внимание", message);
    }

    /** Диалог подтверждения "Да/Нет". Возвращает true, если пользователь подтвердил. */
    public static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message,
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Подтверждение");
        alert.setHeaderText(null);
        styleDialog(alert);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        styleDialog(alert);
        alert.showAndWait();
    }

    /** Применяет стили к диалоговому окну. */
    private static void styleDialog(Alert alert) {
        var pane = alert.getDialogPane();
        var url = UiUtils.class.getResource(STYLESHEET);
        if (url != null) {
            pane.getStylesheets().add(url.toExternalForm());
        }
    }

    /** Центрирует и показывает сцену в переданном окне. */
    public static void setScene(Stage stage, Scene scene, String title, double w, double h) {
        applyStyles(scene);
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setWidth(w);
        stage.setHeight(h);
        stage.centerOnScreen();
    }
}
