package com.view;

import com.blueprinthell.Config;
import com.model.GameState;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    private final Rectangle remainingWireRect;
    private final Label coinsLabel;
    private final Label lossLabel;
    private final Button startGameButton;

    private static final double METER_WIDTH = 130.0;
    private static final double METER_HEIGHT = 14.0;

    public HudView(GameState gameState) {
        if (gameState == null) {
            throw new IllegalArgumentException("GameState cannot be null for HudView");
        }

        this.setPadding(new Insets(5, 10, 5, 10));
        this.setSpacing(10);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setStyle("-fx-background-color: rgba(26, 26, 46, 0.7); -fx-background-radius: 0;");

        startGameButton = new Button("شروع بازی");
        startGameButton.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        startGameButton.getStyleClass().add("start-game-button");
        startGameButton.disableProperty().bind(
                gameState.allPortsConnectedProperty().not()
        );
        startGameButton.visibleProperty().bind(gameState.gameRunningProperty().not());
        startGameButton.managedProperty().bind(startGameButton.visibleProperty());
        startGameButton.setOnAction(event -> {
            System.out.println("--- HUD: Start Game Button Clicked (Inside setOnAction) ---");
            if (gameState.areAllPortsConnected()) {
                System.out.println("--- HUD: All ports ARE connected. Setting game to running. ---");
                gameState.setGameRunning(true);
                System.out.println("--- HUD: gameState.isGameRunning is NOW: " + gameState.isGameRunning() + " ---");
            } else {
                System.out.println("--- HUD: Start Button Clicked, BUT gameState.areAllPortsConnected() is FALSE ---");
            }
        });

        StackPane wireMeterPane = new StackPane();
        Rectangle backgroundRect = new Rectangle(METER_WIDTH, METER_HEIGHT);
        backgroundRect.setFill(Color.rgb(50, 50, 60, 0.8));
        backgroundRect.setStroke(Color.DARKSLATEGRAY);
        backgroundRect.setArcWidth(Config.SYSTEM_CORNER_ARC / 3);
        backgroundRect.setArcHeight(Config.SYSTEM_CORNER_ARC / 3);

        remainingWireRect = new Rectangle(METER_WIDTH, METER_HEIGHT);
        remainingWireRect.setFill(Config.COLOR_WIRE_NORMAL);
        remainingWireRect.setArcWidth(Config.SYSTEM_CORNER_ARC / 3);
        remainingWireRect.setArcHeight(Config.SYSTEM_CORNER_ARC / 3);
        remainingWireRect.widthProperty().bind(
                Bindings.max(0, gameState.remainingWireLengthProperty()
                        .divide(Config.INITIAL_WIRE_LENGTH)
                        .multiply(METER_WIDTH)
                )
        );
        wireMeterPane.getChildren().addAll(backgroundRect, remainingWireRect);

        wireLengthLabel = new Label();
        wireLengthLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        wireLengthLabel.setTextFill(Config.COLOR_HUD_TEXT);
        wireLengthLabel.textProperty().bind(
                Bindings.format("سیم: %.0f", gameState.remainingWireLengthProperty())
        );
        HBox wireGroup = new HBox(5, wireMeterPane, wireLengthLabel);
        wireGroup.setAlignment(Pos.CENTER_LEFT);


        coinsLabel = new Label();
        coinsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        coinsLabel.setTextFill(Color.GOLD);
        coinsLabel.textProperty().bind(
                Bindings.format("سکه: %d", gameState.coinsProperty())
        );

        lossLabel = new Label();
        lossLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        lossLabel.setTextFill(Color.ORANGERED);
        lossLabel.textProperty().bind(
                Bindings.format("لاس: %d/%d (%.0f%%)",
                        gameState.packetLossProperty(),
                        gameState.totalPacketsSpawnedProperty(),
                        gameState.packetLossPercentageProperty()
                ));
        HBox statsGroup = new HBox(10, coinsLabel, lossLabel);
        statsGroup.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);


        this.getChildren().addAll(startGameButton, wireGroup, spacer, statsGroup);

        this.setVisible(true);
        this.setMouseTransparent(false);
    }

    public void toggleVisibility() {
        this.setVisible(!this.isVisible());
    }
}