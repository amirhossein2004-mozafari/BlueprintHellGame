package com.view;

import com.blueprinthell.Config;
import com.controller.MenuController;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class MenuView extends VBox {

    private final MenuController controller;

    public MenuView(MenuController menuController) {
        if (menuController == null) {
            throw new IllegalArgumentException("MenuController cannot be null for MenuView");
        }
        this.controller = menuController;

        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setPadding(new Insets(50));
        this.setStyle("-fx-background-color: #2E2E3A;");

        Label titleLabel = new Label("Blueprint Hell");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Config.COLOR_HUD_TEXT);
        Button startGameButton = createMenuButton("شروع بازی (مرحله ۱)");
        startGameButton.setOnAction(e -> controller.handleStartGameButton(1));

        Button selectLevelButton = createMenuButton("انتخاب مرحله");
        selectLevelButton.setOnAction(e -> controller.handleSelectLevelButton());
        Button settingsButton = createMenuButton("تنظیمات");
        settingsButton.setOnAction(e -> controller.handleSettingsButton());

        Button exitButton = createMenuButton("خروج");
        exitButton.setOnAction(e -> controller.handleExitButton());

        this.getChildren().addAll(titleLabel, startGameButton, selectLevelButton, settingsButton, exitButton);
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(200);
        button.setPrefHeight(40);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        return button;
    }
}