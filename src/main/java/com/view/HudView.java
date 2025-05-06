package com.view;

import com.blueprinthell.Config;
import com.model.GameState;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

    private static final double METER_WIDTH = 150.0;
    private static final double METER_HEIGHT = 15.0;
    private static final double HUD_HEIGHT = 30.0;

    public HudView(GameState gameState) {
        if (gameState == null) throw new IllegalArgumentException("GameState cannot be null");

        this.setPrefHeight(HUD_HEIGHT);
        this.setMinHeight(HUD_HEIGHT);
        this.setMaxHeight(HUD_HEIGHT);
        this.setPadding(new Insets(5, 15, 5, 15));
        this.setSpacing(15);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setStyle("-fx-background-color: #1A1A2E;");

        StackPane wireMeterPane = new StackPane();
        Rectangle backgroundRect = new Rectangle(METER_WIDTH, METER_HEIGHT);
        backgroundRect.setFill(Color.rgb(50, 50, 60));
        backgroundRect.setStroke(Color.DARKSLATEGRAY);
        backgroundRect.setArcWidth(Config.SYSTEM_CORNER_ARC / 2);
        backgroundRect.setArcHeight(Config.SYSTEM_CORNER_ARC / 2);

        remainingWireRect = new Rectangle(METER_WIDTH, METER_HEIGHT);
        remainingWireRect.setFill(Config.COLOR_WIRE_NORMAL);
        remainingWireRect.setArcWidth(Config.SYSTEM_CORNER_ARC / 2);
        remainingWireRect.setArcHeight(Config.SYSTEM_CORNER_ARC / 2);
        remainingWireRect.widthProperty().bind(
                Bindings.max(0, gameState.remainingWireLengthProperty().divide(Config.INITIAL_WIRE_LENGTH).multiply(METER_WIDTH))
        );
        wireMeterPane.getChildren().addAll(backgroundRect, remainingWireRect);

        wireLengthLabel = new Label();
        wireLengthLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        wireLengthLabel.setTextFill(Config.COLOR_HUD_TEXT);
        wireLengthLabel.textProperty().bind( Bindings.format("Wire: %.0f", gameState.remainingWireLengthProperty()) );

        coinsLabel = new Label();
        coinsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        coinsLabel.setTextFill(Color.GOLD);
        coinsLabel.textProperty().bind( Bindings.format("Coins: %d", gameState.coinsProperty()) );

        lossLabel = new Label();
        lossLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lossLabel.setTextFill(Color.ORANGERED);
        lossLabel.textProperty().bind(
                Bindings.format("Loss: %d/%d (%.1f%%)",
                        gameState.packetLossProperty(),
                        gameState.totalPacketsSpawnedProperty(),
                        gameState.packetLossPercentageProperty()
                ));


        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        this.getChildren().addAll(wireMeterPane, wireLengthLabel, spacer, coinsLabel, lossLabel);
        this.setVisible(true);
        this.setMouseTransparent(true);
    }

    private Region Spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }


    public void showTemporary() { this.setVisible(true); }
    public void toggleVisibility() { this.setVisible(!this.isVisible()); }
}