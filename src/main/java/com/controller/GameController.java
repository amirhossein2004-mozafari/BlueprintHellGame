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
        SystemNode system1 = new SystemNode(100, 150);
        SystemNode system2 = new SystemNode(400, 300);
        SystemNode system3 = new SystemNode(700, 100);

        gameState.addSystem(system1);
        gameState.addSystem(system2);
        gameState.addSystem(system3);

        gameView.drawInitialSystems(gameState);

        System.out.println("Game Initialized with " + gameState.getSystems().size() + " systems.");
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