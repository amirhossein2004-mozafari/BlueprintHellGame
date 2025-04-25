package com.view;

import com.blueprinthell.Config;
import com.model.GameState;
import com.model.SystemNode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;


public class GameView extends Pane {


    public GameView(double width, double height) {
        this.setPrefSize(width, height);
        this.setMinSize(width, height);
        this.setMaxSize(width, height);
        this.setStyle("-fx-background-color: #" + Config.COLOR_BACKGROUND.toString().substring(2) + ";");
    }


    public void drawInitialSystems(GameState gameState) {
        this.getChildren().clear();

        for (SystemNode system : gameState.getSystems()) {
            Rectangle systemRect = new Rectangle();
            systemRect.setLayoutX(system.getX());
            systemRect.setLayoutY(system.getY());
            systemRect.setWidth(system.getWidth());
            systemRect.setHeight(system.getHeight());
            systemRect.setFill(Config.COLOR_SYSTEM_RECT);
            systemRect.setStroke(Color.BLACK);
            systemRect.setStrokeWidth(1);
            this.getChildren().add(systemRect);
        }
    }

}