package com.view;

import com.blueprinthell.Config;
import com.controller.GameController;
import com.model.GameState;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class HudView extends HBox {

    private final Label wireLengthLabel;
    private final Label coinsLabel;
    private final Label lossLabel;
    private final Button startGameButton;
    private final Label timeElapsedLabel;
    private final Label currentLevelLabel;
    private final Label spawnedPacketsLabel;
    private final Label setupTimerLabel;
    private final Button readyButton;
    private final Label p1ReadyLabel;
    private final Label p2ReadyLabel;

    private final HBox infoGroupMiddle;
    private final HBox setupUIGroup;
    private final GameState gameState;

    private static final double METER_WIDTH = 130.0;
    private static final double METER_HEIGHT = 14.0;


    public HudView(GameState gameState, GameController gameController) {
        if (gameState == null) {
            throw new IllegalArgumentException("GameState cannot be null for HudView");
        }
        this.gameState = gameState;

        this.setPadding(new Insets(5, 10, 5, 10));
        this.setSpacing(10);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setStyle("-fx-background-color: rgba(26, 26, 46, 0.7); -fx-background-radius: 0;");


        startGameButton = new Button("شروع بازی");
        startGameButton.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        startGameButton.disableProperty().bind(gameState.allPortsConnectedProperty().not());
        startGameButton.managedProperty().bind(startGameButton.visibleProperty());
        startGameButton.setOnAction(event -> {
            if (gameState.areAllPortsConnected()) {
                gameState.setGameRunning(true);
            }
        });

        currentLevelLabel = createLabel("", Config.COLOR_HUD_TEXT, 12, true);
        currentLevelLabel.textProperty().bind(Bindings.format("مرحله: %d", gameState.currentLevelNumberProperty()));

        timeElapsedLabel = createLabel("", Config.COLOR_HUD_TEXT, 12, true);
        timeElapsedLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            double seconds = gameState.getGameTimeElapsedSeconds();
            return String.format("زمان: %02d:%02d", (int)(seconds/60), (int)(seconds%60));
        }, gameState.gameTimeElapsedSecondsProperty()));

        spawnedPacketsLabel = createLabel("", Config.COLOR_HUD_TEXT, 11, true);
        spawnedPacketsLabel.textProperty().bind(Bindings.format("پاکت خارج شده: %d / %d",
                gameState.totalPacketsSuccessfullySpawnedCountProperty(), gameState.packetsToSpawnInLevelProperty()));

        wireLengthLabel = createLabel("", Config.COLOR_HUD_TEXT, 11, true);
        wireLengthLabel.textProperty().bind(Bindings.format("سیم: %.0f", gameState.remainingWireLengthProperty()));

        coinsLabel = createLabel("", Color.GOLD, 11, true);
        coinsLabel.textProperty().bind(Bindings.format("سکه: %d", gameState.coinsProperty()));

        lossLabel = createLabel("", Color.ORANGERED, 11, true);
        lossLabel.textProperty().bind(Bindings.format("از دست رفته: %.0f%%", gameState.packetLossPercentageBySizeProperty()));

        StackPane wireMeterPane = createWireMeter();

        setupTimerLabel = createLabel("آماده سازی: 30", Color.YELLOW, 16, true);
        p1ReadyLabel = createLabel("بازیکن ۱: منتظر", Color.ORANGERED, 12, false);
        p2ReadyLabel = createLabel("بازیکن ۲: منتظر", Color.ORANGERED, 12, false);
        readyButton = new Button("آماده‌ام!");
        readyButton.setOnAction(e -> {
            if (gameController != null) {
                gameController.sendReadySignal();
                readyButton.setDisable(true);
            }
        });


        HBox infoGroupLeft = new HBox(10, currentLevelLabel, timeElapsedLabel);
        infoGroupLeft.setAlignment(Pos.CENTER_LEFT);

        HBox wireGroup = new HBox(5, wireMeterPane, wireLengthLabel);
        infoGroupMiddle = new HBox(10, wireGroup, spawnedPacketsLabel);
        infoGroupMiddle.setAlignment(Pos.CENTER_LEFT);

        HBox statsGroupRight = new HBox(10, coinsLabel, lossLabel);
        statsGroupRight.setAlignment(Pos.CENTER_LEFT);

        setupUIGroup = new HBox(15, setupTimerLabel, p1ReadyLabel, p2ReadyLabel, readyButton);
        setupUIGroup.setAlignment(Pos.CENTER);
        setupUIGroup.setVisible(false);


        StackPane middleContainer = new StackPane(infoGroupMiddle, setupUIGroup);
        middleContainer.setAlignment(Pos.CENTER);

        Region spacer1 = new Region(); HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region(); HBox.setHgrow(spacer2, Priority.ALWAYS);

        this.getChildren().clear();
        this.getChildren().addAll(infoGroupLeft, spacer1, middleContainer, spacer2, statsGroupRight, startGameButton);

        startGameButton.setVisible(true);
        setupUIGroup.setVisible(false);
    }


    private Label createLabel(String text, Color color, int size, boolean isBold) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", isBold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        label.setTextFill(color);
        return label;
    }

    private StackPane createWireMeter() {
        Rectangle bg = new Rectangle(METER_WIDTH, METER_HEIGHT);
        bg.setFill(Color.rgb(50, 50, 60, 0.8));
        Rectangle fg = new Rectangle(METER_WIDTH, METER_HEIGHT);
        fg.setFill(Config.COLOR_WIRE_NORMAL);
        fg.widthProperty().bind(Bindings.max(0, gameState.remainingWireLengthProperty()
                .divide(Config.INITIAL_WIRE_LENGTH).multiply(METER_WIDTH)));
        StackPane meter = new StackPane(bg, fg);
        bg.setArcWidth(Config.SYSTEM_CORNER_ARC / 3);
        bg.setArcHeight(Config.SYSTEM_CORNER_ARC / 3);
        fg.setArcWidth(Config.SYSTEM_CORNER_ARC / 3);
        fg.setArcHeight(Config.SYSTEM_CORNER_ARC / 3);
        return meter;
    }

    public void showSetupUI(boolean show) {
        infoGroupMiddle.setVisible(!show);
        setupUIGroup.setVisible(show);

        currentLevelLabel.setVisible(!show);
        timeElapsedLabel.setVisible(!show);
        coinsLabel.setVisible(!show);
        lossLabel.setVisible(!show);


        boolean isGameCurrentlyRunning = (gameState != null) && gameState.isGameRunning();
        startGameButton.setVisible(!show && !isGameCurrentlyRunning);
    }

    public void updateSetupUI(int time, boolean p1Ready, boolean p2Ready) {
        setupTimerLabel.setText("آماده سازی: " + time);
        if (p1Ready) {
            p1ReadyLabel.setText("بازیکن ۱: آماده");
            p1ReadyLabel.setTextFill(Color.LIMEGREEN);
        }
        if (p2Ready) {
            p2ReadyLabel.setText("بازیکن ۲: آماده");
            p2ReadyLabel.setTextFill(Color.LIMEGREEN);
        }
    }

    public void toggleVisibility() {
        this.setVisible(!this.isVisible());
    }
}