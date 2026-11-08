package com.controller;

import com.model.GameState;
import javafx.animation.AnimationTimer;
import javafx.scene.control.Label;

public class GameLoop extends AnimationTimer {

    private final GameController gameController;
    private final GameState gameState;
    private long previousNanoTime = -1;
    private long lastFpsUpdateTime = 0;
    private int frameCount = 0;
    private Label fpsLabel = null;

    public GameLoop(GameController gameController, GameState gameState) {
        if (gameController == null || gameState == null) {
            throw new IllegalArgumentException("GameController and GameState cannot be null for GameLoop");
        }
        this.gameController = gameController;
        this.gameState = gameState;
    }

    public GameLoop(GameController gameController, GameState gameState, Label fpsLabel) {
        this(gameController, gameState);
        this.fpsLabel = fpsLabel;
    }

    @Override
    public void handle(long currentNanoTime) {
        if (previousNanoTime == -1) {
            previousNanoTime = currentNanoTime;
            return;
        }
        double deltaTime = (currentNanoTime - previousNanoTime) / 1_000_000_000.0;
        previousNanoTime = currentNanoTime;

        frameCount++;
        if (currentNanoTime - lastFpsUpdateTime >= 1_000_000_000) {
            if (fpsLabel != null) {
                double fps = frameCount / ((currentNanoTime - lastFpsUpdateTime) / 1_000_000_000.0);
                fpsLabel.setText(String.format("FPS: %.1f", fps));
            }
            frameCount = 0;
            lastFpsUpdateTime = currentNanoTime;
        }

        if (gameState != null) {
            double scaledDeltaTime = deltaTime * gameState.getCurrentTimeScale();
            gameController.updateGame(scaledDeltaTime);
        } else {
            System.err.println("Error: GameState is null in GameLoop.handle()!");
        }
    }

    @Override
    public void start() {
        previousNanoTime = -1;
        lastFpsUpdateTime = 0;
        frameCount = 0;
        System.out.println("GameLoop Started.");
        super.start();
    }

    @Override
    public void stop() {
        System.out.println("GameLoop Stopped.");
        super.stop();
    }
}