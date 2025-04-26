package com.controller;

import com.model.GameState;
import com.model.SystemNode;
import com.view.GameView;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;

public class GameController {

    private final GameState gameState;
    private final GameView gameView;

    public GameController(GameState gameState, GameView gameView) {
        this.gameState = gameState;
        this.gameView = gameView;
    }

    public void initializeGame() {
        gameState.clearSystems();
        gameState.addSystem(new SystemNode(150, 100));
        gameState.addSystem(new SystemNode(700, 100));
        gameState.addSystem(new SystemNode(400, 300));
        gameState.addSystem(new SystemNode(150, 500));
        gameState.addSystem(new SystemNode(700, 500));
        gameView.drawInitialState(gameState);
        System.out.println("Game Initialized with " + gameState.getSystems().size() + " systems in a fixed layout (random ports).");
    }

    public void handleMousePress(MouseEvent event) {
        System.out.println("Mouse Pressed at: (" + event.getX() + ", " + event.getY() + ")");
    }

    public void handleMouseDrag(MouseEvent event) {
    }

    public void handleMouseRelease(MouseEvent event) {
        System.out.println("Mouse Released at: (" + event.getX() + ", " + event.getY() + ")");
    }

    public void handleKeyPress(KeyCode code) {
        System.out.println("Key Pressed: " + code);
    }

    public void updateGame(double deltaTime) {
    }

    public void pauseGameLogic() {
        System.out.println("Game Logic Paused (by Controller)");
    }

    public void resumeGameLogic() {
        System.out.println("Game Logic Resumed (by Controller)");
    }

    public void pauseGame() {
        System.out.println("Game Paused (Animation Stopped)");
    }

    public void resumeGame() {
        System.out.println("Game Resumed (Animation Started)");
    }
}