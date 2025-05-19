package com.view;

import com.blueprinthell.Config;
import com.blueprinthell.MainApplication;
import com.controller.MenuController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameOverView extends VBox {

    public GameOverView(MainApplication mainApp, boolean success, int score, int packetsLost, int currentLevelNumber) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(25);
        this.setPadding(new Insets(50));
        this.setStyle("-fx-background-color: #20202D;");

        Label titleLabel = new Label();
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));

        Label resultLabel = new Label();
        resultLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        if (success) {
            titleLabel.setText("مرحله تکمیل شد!");
            titleLabel.setTextFill(Color.LIGHTGREEN);
            resultLabel.setText(" شما این مرحله را با موفقیت به پایان رساندید.");
        } else {
            titleLabel.setText("پایان بازی!");
            titleLabel.setTextFill(Color.INDIANRED);
            resultLabel.setText("متاسفانه شما در این مرحله شکست خوردید.");
        }

        Label scoreLabel = new Label("امتیاز شما: " + score + " سکه");
        scoreLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        scoreLabel.setTextFill(Config.COLOR_HUD_TEXT);

        Label packetsLostLabel = new Label("پاکت‌های از دست رفته: " + packetsLost);
        packetsLostLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        packetsLostLabel.setTextFill(Config.COLOR_HUD_TEXT);


        VBox buttonsBox = new VBox(15);
        buttonsBox.setAlignment(Pos.CENTER);

        Button restartButton = new Button("شروع مجدد مرحله " + currentLevelNumber);
        styleMenuButton(restartButton);
        restartButton.setOnAction(e -> {
            if (mainApp != null) {
                mainApp.startGame(currentLevelNumber);
            }
        });

        Button mainMenuButton = new Button("بازگشت به منوی اصلی");
        styleMenuButton(mainMenuButton);
        mainMenuButton.setOnAction(e -> {
            if (mainApp != null) {
                mainApp.showMainMenu(new MenuController(mainApp));
            }
        });

        buttonsBox.getChildren().addAll(restartButton, mainMenuButton);

        if (success) {
            int nextLevelNumber = currentLevelNumber + 1;
            Button nextLevelButton = new Button("رفتن به مرحله " + nextLevelNumber);
            styleMenuButton(nextLevelButton);
            nextLevelButton.setOnAction(e -> {
                if (mainApp != null) {
                    System.out.println("Attempting to start next level: " + nextLevelNumber);
                    mainApp.startGame(nextLevelNumber);
                }
            });
        }


        this.getChildren().addAll(titleLabel, resultLabel, scoreLabel, packetsLostLabel, buttonsBox);
    }

    private void styleMenuButton(Button button) {
        button.setPrefWidth(220);
        button.setPrefHeight(45);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        button.setStyle("-fx-background-color: " + formatColorToCss(Config.COLOR_SHOP_BUTTON) +
                "; -fx-text-fill: " + formatColorToCss(Config.COLOR_SHOP_TEXT) +
                "; -fx-background-radius: 5;");
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