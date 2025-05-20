package com.view;

import com.blueprinthell.Config;
import com.blueprinthell.MainApplication;
import com.controller.MenuController;
import com.controller.SettingsController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.input.KeyCode;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsView extends VBox {

    private final MainApplication mainApp;
    private final SettingsController settingsController;
    private final Map<String, Label> keyBindingLabels = new LinkedHashMap<>();

    public SettingsView(MainApplication mainApp, SettingsController settingsController, MenuController menuController) {
        this.mainApp = mainApp;
        this.settingsController = settingsController;

        this.setAlignment(Pos.TOP_CENTER);
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: #2A2A35;");

        Label titleLabel = new Label("تنظیمات");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Config.COLOR_HUD_TEXT);

        VBox soundSettingsBox = new VBox(10);
        soundSettingsBox.setAlignment(Pos.CENTER_LEFT);
        Label soundTitleLabel = new Label("تنظیمات صدا:");
        styleSectionTitle(soundTitleLabel);

        HBox volumeControlBox = new HBox(10);
        volumeControlBox.setAlignment(Pos.CENTER_LEFT);
        Label volumeLabel = new Label("حجم کلی صدا:");
        styleSettingLabel(volumeLabel);

        Slider volumeSlider = new Slider(0, 100, settingsController.getCurrentGlobalVolume() * 100);
        volumeSlider.setPrefWidth(250);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setMajorTickUnit(25);
        volumeSlider.setMinorTickCount(4);
        volumeSlider.setBlockIncrement(5);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            settingsController.setGlobalVolume(newVal.doubleValue() / 100.0);
        });
        volumeControlBox.getChildren().addAll(volumeLabel, volumeSlider);
        soundSettingsBox.getChildren().addAll(soundTitleLabel, volumeControlBox);


        VBox keyBindingSettingsBox = new VBox(10);
        keyBindingSettingsBox.setAlignment(Pos.CENTER_LEFT);
        Label keyBindingTitleLabel = new Label("تغییر دکمه‌های بازی (Key Bindings):");
        styleSectionTitle(keyBindingTitleLabel);

        GridPane keyGrid = new GridPane();
        keyGrid.setHgap(15);
        keyGrid.setVgap(8);
        keyGrid.setPadding(new Insets(5));

        addKeyBindingEntry(keyGrid, 0, "حرکت زمان به جلو", "KEY_TIME_FORWARD", Config.KEY_TIME_FORWARD);
        addKeyBindingEntry(keyGrid, 1, "حرکت زمان به عقب", "KEY_TIME_BACKWARD", Config.KEY_TIME_BACKWARD);
        addKeyBindingEntry(keyGrid, 2, "باز کردن فروشگاه", "KEY_SHOP", Config.KEY_SHOP);
        addKeyBindingEntry(keyGrid, 4, "تغییر نمایش HUD", "KEY_TOGGLE_HUD", Config.KEY_TOGGLE_HUD);
        addKeyBindingEntry(keyGrid, 5, "Pause/Resume بازی", "KEY_PAUSE", Config.KEY_PAUSE);
        addKeyBindingEntry(keyGrid, 6, "خروج به منوی اصلی", "KEY_EXIT_TO_MENU", Config.KEY_EXIT_TO_MENU);

        Label keyBindingInfo = new Label("برای تغییر، روی دکمه 'تغییر' کلیک کرده و سپس کلید جدید را فشار دهید.\nبرای لغو، ESC را بزنید. دقت کنید کلید جدید تکراری نباشد.");
        styleInfoLabel(keyBindingInfo);

        keyBindingSettingsBox.getChildren().addAll(keyBindingTitleLabel, keyGrid, keyBindingInfo);

        Button backButton = new Button("بازگشت به منوی اصلی");
        styleMenuButton(backButton);
        backButton.setOnAction(e -> mainApp.showMainMenu(menuController));

        this.getChildren().addAll(titleLabel, soundSettingsBox, keyBindingSettingsBox, backButton);
        System.out.println("SettingsView constructed.");
    }


    private void addKeyBindingEntry(GridPane grid, int rowIndex, String actionDisplayName, String actionKeyName, KeyCode currentKeyCode) {
        Label actionLabel = new Label(actionDisplayName + ":");
        styleSettingLabel(actionLabel);

        Label currentKeyLabel = new Label(currentKeyCode.getName());
        styleKeyLabel(currentKeyLabel);
        keyBindingLabels.put(actionKeyName, currentKeyLabel);

        Button changeButton = new Button("تغییر");
        styleMenuButton(changeButton);
        changeButton.setOnAction(e -> {
            settingsController.startKeyRebindingProcess(actionKeyName, actionDisplayName, this);
        });

        grid.add(actionLabel, 0, rowIndex);
        grid.add(currentKeyLabel, 1, rowIndex);
        grid.add(changeButton, 2, rowIndex);
    }

    public void updateKeyBindingDisplay(String actionKeyName, KeyCode newKeyCode) {
        Label labelToUpdate = keyBindingLabels.get(actionKeyName);
        if (labelToUpdate != null) {
            labelToUpdate.setText(newKeyCode.getName());
            System.out.println("SettingsView: Display updated for " + actionKeyName + " to " + newKeyCode.getName());
        } else {
            System.err.println("SettingsView Error: Could not find label for actionKeyName: " + actionKeyName);
        }
    }


    private void styleSectionTitle(Label label) {
        label.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        label.setTextFill(Config.COLOR_HUD_TEXT.deriveColor(0, 1, 1, 0.9));
        label.setPadding(new Insets(10, 0, 5, 0));
    }

    private void styleSettingLabel(Label label) {
        label.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        label.setTextFill(Config.COLOR_HUD_TEXT);
        label.setMinWidth(180);
    }

    private void styleKeyLabel(Label label) {
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setTextFill(Color.LIGHTSKYBLUE);
        label.setMinWidth(100);
    }

    private void styleInfoLabel(Label label) {
        label.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        label.setTextFill(Config.COLOR_HUD_TEXT.deriveColor(0, 1, 1, 0.7));
        label.setWrapText(true);
        label.setPadding(new Insets(10,0,0,0));
    }

    private void styleMenuButton(Button button) {
        button.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        String baseStyle = "-fx-background-color: " + formatColorToCss(Config.COLOR_SHOP_BUTTON) + "; -fx-text-fill: " + formatColorToCss(Config.COLOR_SHOP_TEXT) + "; -fx-background-radius: 3;";
        String hoverStyle = "-fx-background-color: " + formatColorToCss(Config.COLOR_SHOP_BUTTON_HOVER) + "; -fx-text-fill: " + formatColorToCss(Config.COLOR_SHOP_TEXT) + "; -fx-background-radius: 3;";
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
        button.setPrefWidth(150);
    }

    private String formatColorToCss(Color color) {
        if (color == null) return "transparent";
        if (color.getOpacity() < 1.0) {
            return String.format("rgba(%d, %d, %d, %.2f)", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255), color.getOpacity());
        } else {
            return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
        }
    }
}