package com.studio.booking.ui;

import com.studio.booking.service.AuthService;
import com.studio.booking.service.Session;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Окно входа администратора (только пароль).
 */
public class LoginView {

    private final Stage stage;
    private final AuthService authService = new AuthService();

    public LoginView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        GridPane root = new GridPane();
        VBox sidebar = buildSidebar();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(48));
        sidebar.setAlignment(Pos.CENTER_LEFT);
        VBox formPane = buildForm();
        formPane.setAlignment(Pos.CENTER);
        formPane.setPadding(new Insets(48));
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(42);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(58);
        root.getColumnConstraints().addAll(left, right);
        GridPane.setVgrow(sidebar, Priority.ALWAYS);
        GridPane.setVgrow(formPane, Priority.ALWAYS);
        root.add(sidebar, 0, 0);
        root.add(formPane, 1, 0);
        Scene scene = new Scene(root, 960, 640);
        UiUtils.setScene(stage, scene, "Photo Studio — Вход", 960, 640);
        stage.show();
    }

    private VBox buildSidebar() {
        Label brand = new Label("PHOTO STUDIO");
        brand.getStyleClass().add("sidebar-title");
        Label tagline = new Label("Панель\nадминистратора");
        tagline.getStyleClass().add("sidebar-title");
        tagline.setStyle("-fx-font-size: 30px;");
        Label desc = new Label(
                "Бронируйте залы за клиентов,\n" +
                "управляйте расписанием и каталогом.");
        desc.getStyleClass().add("sidebar-text");
        Label hint = new Label("Демо-пароль: admin");
        hint.getStyleClass().add("sidebar-text");
        hint.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 12; -fx-padding: 16;");
        VBox box = new VBox(18, brand, tagline, desc, spacer(), hint);
        box.setFillWidth(true);
        return box;
    }

    private VBox buildForm() {
        Label title = new Label("Вход администратора");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Введите пароль для доступа к системе");
        subtitle.getStyleClass().add("subtitle");
        Label passLabel = new Label("ПАРОЛЬ");
        passLabel.getStyleClass().add("field-label");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Введите пароль");
        Button loginBtn = new Button("Войти");
        loginBtn.getStyleClass().add("button-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> doLogin(passField.getText()));
        passField.setOnAction(e -> doLogin(passField.getText()));
        VBox card = new VBox(10,
                title, subtitle,
                new Region() {{ setPrefHeight(8); }},
                passLabel, passField,
                new Region() {{ setPrefHeight(8); }},
                loginBtn);
        card.getStyleClass().add("card");
        card.setMaxWidth(380);
        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private void doLogin(String password) {
        if (authService.login(password)) {
            Session.setLoggedIn(true);
            new MainView(stage).show();
        } else {
            UiUtils.error("Неверный пароль.");
        }
    }

    private Region spacer() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }
}
