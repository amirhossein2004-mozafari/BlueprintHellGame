package com.view;

import com.blueprinthell.Config;
import com.model.GameState;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class HudView extends HBox {

    private final Label wireLengthLabel;
    private final Rectangle remainingWireRect;
    private static final double METER_WIDTH = 150.0;
    private static final double METER_HEIGHT = 15.0;

    public HudView(GameState gameState) {
        if (gameState == null) throw new IllegalArgumentException("GameState cannot be null");

        this.setPadding(new Insets(0));
        this.setSpacing(10);
        this.setAlignment(Pos.CENTER_LEFT);

        StackPane wireMeterPane = new StackPane();
        Rectangle backgroundRect = new Rectangle(METER_WIDTH, METER_HEIGHT);
        backgroundRect.setFill(Color.rgb(50, 50, 60));
        backgroundRect.setStroke(Color.BLACK);
        backgroundRect.setArcWidth(Config.SYSTEM_CORNER_ARC / 2);
        backgroundRect.setArcHeight(Config.SYSTEM_CORNER_ARC / 2);

        remainingWireRect = new Rectangle(METER_WIDTH, METER_HEIGHT);
        remainingWireRect.setFill(Config.COLOR_WIRE_NORMAL);
        remainingWireRect.setArcWidth(Config.SYSTEM_CORNER_ARC / 2);
        remainingWireRect.setArcHeight(Config.SYSTEM_CORNER_ARC / 2);

        remainingWireRect.widthProperty().bind(
                Bindings.max(0,
                        gameState.remainingWireLengthProperty()
                                .divide(Config.INITIAL_WIRE_LENGTH).multiply(METER_WIDTH))
        );

        wireMeterPane.getChildren().addAll(backgroundRect, remainingWireRect);

        wireLengthLabel = new Label();
        wireLengthLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        wireLengthLabel.setTextFill(Config.COLOR_HUD_TEXT);
        wireLengthLabel.textProperty().bind(
                Bindings.format("Wire: %.0f", gameState.remainingWireLengthProperty())
        );

        this.getChildren().addAll(wireMeterPane, wireLengthLabel);
        this.setVisible(true);
        this.setMouseTransparent(true);
    }

    public void showTemporary() { this.setVisible(true); }
    public void toggleVisibility() { this.setVisible(!this.isVisible()); }
}