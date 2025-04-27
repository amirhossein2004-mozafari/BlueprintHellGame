package com.controller;

import javafx.animation.AnimationTimer;
import javafx.scene.control.Label;

public class GameLoop extends AnimationTimer {

    private final GameController gameController;
    private long previousNanoTime = -1;
    private long lastFpsUpdateTime = 0;
    private int frameCount = 0;
    private Label fpsLabel = null;

    public GameLoop(GameController gameController) {
        if (gameController == null) {
            throw new IllegalArgumentException("GameController cannot be null for GameLoop");
        }
        this.gameController = gameController;
    }

    public GameLoop(GameController gameController, Label fpsLabel) {
        this(gameController);
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

        gameController.updateGame(deltaTime);
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