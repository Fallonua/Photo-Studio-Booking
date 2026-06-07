package com.studio.booking;

import com.studio.booking.db.DatabaseManager;
import com.studio.booking.ui.LoginView;
import com.studio.booking.ui.UiUtils;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Главный класс приложения (точка входа JavaFX).
 * <p>
 * При запуске:
 * <ol>
 *     <li>проверяет подключение к БД;</li>
 *     <li>инициализирует схему и демонстрационные данные;</li>
 *     <li>показывает окно входа.</li>
 * </ol>
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Проверка корректности подключения к БД.
        if (!DatabaseManager.testConnection()) {
            showFatalError("Не удалось подключиться к базе данных.\nПриложение будет закрыто.");
            return;
        }
        // 2. Инициализация схемы и стартовых данных.
        try {
            DatabaseManager.initialize();
        } catch (Exception e) {
            showFatalError("Ошибка инициализации базы данных:\n" + e.getMessage());
            return;
        }
        // 3. Показ окна входа.
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
        new LoginView(primaryStage).show();
    }

    /** Показывает критическую ошибку и завершает приложение. */
    private void showFatalError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Критическая ошибка");
        alert.setHeaderText(null);
        var url = UiUtils.class.getResource(UiUtils.STYLESHEET);
        if (url != null) {
            alert.getDialogPane().getStylesheets().add(url.toExternalForm());
        }
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
