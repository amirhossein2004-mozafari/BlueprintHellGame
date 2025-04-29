package com.view;

import com.blueprinthell.Config;
import com.model.GameState;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class WireIndicatorView extends StackPane {

    private final Rectangle backgroundRect;
    private final Label wireLengthLabel;

    public WireIndicatorView(GameState gameState) {
        backgroundRect = new Rectangle(Config.WIRE_INDICATOR_WIDTH, Config.WIRE_INDICATOR_HEIGHT);
        backgroundRect.setFill(Config.COLOR_SYSTEM_RECT);
        backgroundRect.setStroke(Color.BLACK);
        backgroundRect.setStrokeWidth(0.5);
        backgroundRect.setArcWidth(Config.WIRE_INDICATOR_CORNER_ARC);
        backgroundRect.setArcHeight(Config.WIRE_INDICATOR_CORNER_ARC);

        wireLengthLabel = new Label();
        wireLengthLabel.setTextFill(Config.COLOR_HUD_TEXT);
        wireLengthLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        wireLengthLabel.textProperty().bind(
                Bindings.format(
                        "Wire: %.1f",
                        gameState.remainingWireLengthProperty()
                )
        );

        this.setAlignment(Pos.CENTER);
        this.getChildren().addAll(backgroundRect, wireLengthLabel);
    }
}