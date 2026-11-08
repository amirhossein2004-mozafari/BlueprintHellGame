package com.view;

import com.blueprinthell.Config;
import com.blueprinthell.MainApplication;
import com.controller.MenuController;
import com.model.GameState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameOverView extends VBox {

    private final MainApplication mainApp;

    public GameOverView(MainApplication mainApp, boolean success, int score, int packetsLostOrPercentage, int currentLevelPlayed) {
        this.mainApp = mainApp;

        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setPadding(new Insets(50));
        this.setStyle("-fx-background-color: #20202D;");

        Label titleLabel = new Label();
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));

        Label resultMessageLabel = new Label();
        resultMessageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        resultMessageLabel.setWrapText(true);
        resultMessageLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        if (success) {
            titleLabel.setText("مرحله " + currentLevelPlayed + " با موفقیت انجام شد!");
            titleLabel.setTextFill(Color.PALEGREEN);
            resultMessageLabel.setText("عالی بود! شما توانستید این مرحله را با موفقیت به پایان برسانید.");
        } else {
            titleLabel.setText("پایان بازی در مرحله " + currentLevelPlayed);
            titleLabel.setTextFill(Color.SALMON);
            resultMessageLabel.setText("متاسفانه شما در این مرحله موفق نشدید. بیشتر تلاش کنید!");
        }

        VBox statsBox = new VBox(10);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setPadding(new Insets(10, 0, 20, 0));

        Label scoreLabel = new Label("سکه های کسب شده: " + score);
        styleStatsLabel(scoreLabel);


        Label lossLabel = new Label("میزان از دست رفتن پاکت‌ها (بر اساس اندازه): " + packetsLostOrPercentage + "%");
        styleStatsLabel(lossLabel);
        if (success) {
            lossLabel.setTextFill(Color.LIGHTGRAY);
        } else {
            lossLabel.setTextFill(Color.ORANGERED.darker());
        }

        statsBox.getChildren().addAll(scoreLabel, lossLabel);

        GameState completedGameState = mainApp.getGameStateForGameOver();
        if (completedGameState != null) {
            Label requiredPacketsLabel = new Label("تعداد پاکت‌های هدف مرحله: " + completedGameState.getPacketsToSpawnInLevel());
            styleStatsLabel(requiredPacketsLabel);
            statsBox.getChildren().add(requiredPacketsLabel);
        }

        HBox buttonsBox = new HBox(20);
        buttonsBox.setAlignment(Pos.CENTER);

        Button restartButton = new Button("تلاش مجدد (مرحله " + currentLevelPlayed + ")");
        styleMenuButton(restartButton);
        restartButton.setOnAction(e -> {
            if (this.mainApp != null) {
                this.mainApp.startGame(currentLevelPlayed);
            }
        });

        Button mainMenuButton = new Button("بازگشت به منوی اصلی");
        styleMenuButton(mainMenuButton);
        mainMenuButton.setOnAction(e -> {
            if (this.mainApp != null) {
                this.mainApp.showMainMenu(new MenuController(this.mainApp));
            }
        });

        buttonsBox.getChildren().addAll(restartButton, mainMenuButton);

        if (success) {

            int nextLevel = currentLevelPlayed + 1;
            if (mainApp.hasNextLevel(nextLevel)) {
                Button nextLevelButton = new Button("رفتن به مرحله " + nextLevel);
                styleMenuButton(nextLevelButton);
                nextLevelButton.setOnAction(e -> {
                    if (this.mainApp != null) {
                        this.mainApp.startGame(nextLevel);
                    }
                });
                buttonsBox.getChildren().add(nextLevelButton);
            }
        }

        this.getChildren().addAll(titleLabel, resultMessageLabel, statsBox, buttonsBox);
    }

    private void styleStatsLabel(Label label) {
        label.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        label.setTextFill(Config.COLOR_HUD_TEXT);
    }

    private void styleMenuButton(Button button) {
        button.setPrefWidth(200);
        button.setPrefHeight(40);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        String baseStyle = "-fx-background-color: " + formatColorToCss(Config.COLOR_SHOP_BUTTON) +
                "; -fx-text-fill: " + formatColorToCss(Config.COLOR_SHOP_TEXT) +
                "; -fx-background-radius: 4;";
        String hoverStyle = "-fx-background-color: " + formatColorToCss(Config.COLOR_SHOP_BUTTON_HOVER) +
                "; -fx-text-fill: " + formatColorToCss(Config.COLOR_SHOP_TEXT) +
                "; -fx-background-radius: 4;";
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }

    private String formatColorToCss(Color color) {
        if (color == null) return "transparent";
        if (color.getOpacity() < 1.0) {
            return String.format("rgba(%d, %d, %d, %.2f)",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255),
                    color.getOpacity());
        } else {
            return String.format("#%02X%02X%02X",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255));
        }
    }
}