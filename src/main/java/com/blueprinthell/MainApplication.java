package com.blueprinthell;

import com.controller.*;
import com.model.GameState;
import com.view.*;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.util.Objects;

public class MainApplication extends Application {

    private Stage primaryStage;
    private GameState gameState;
    private GameController gameController;
    private GameLoop gameLoop;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Blueprint Hell");
        primaryStage.setResizable(false);
        startGame(1);
        primaryStage.show();
    }

    private void stopCurrentGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
        }
        if (gameController != null) {
            gameController = null;
        }
        gameState = null;
    }

    public void showMainMenu(MenuController controller) {
        stopCurrentGameLoop();
        System.out.println("Show Main Menu - Not implemented yet.");

    }

    public void startGame(int levelNumber) {
        stopCurrentGameLoop();
        gameState = new GameState();
        GameView gameView = new GameView(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        StackPane gameStackPane = new StackPane(gameView);
        gameController = new GameController(gameState, gameView);
        gameController.initializeGame();
        Scene gameScene = new Scene(gameStackPane, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        setupInputHandlers(gameScene, gameController);
        applyStylesheet(gameScene, "/styles/style.css");
        gameLoop = new GameLoop(gameController);
        gameLoop.start();
        primaryStage.setScene(gameScene);
        System.out.println("Game scene set and loop started for level " + levelNumber);
    }

    private void setupInputHandlers(Scene gameScene, GameController controller) {
        gameScene.setOnMousePressed(event -> {
            if (gameState != null) controller.handleMousePress(event);
        });
        gameScene.setOnMouseDragged(event -> {
            if (gameState != null) controller.handleMouseDrag(event);
        });
        gameScene.setOnMouseReleased(event -> {
            if (gameState != null) controller.handleMouseRelease(event);
        });
        gameScene.setOnKeyPressed(event -> {
            if (gameState == null) return;
            KeyCode code = event.getCode();
            controller.handleKeyPress(code);
            if (code == KeyCode.ESCAPE) {
                System.out.println("Escape pressed, exiting application.");
                stopCurrentGameLoop();
                primaryStage.close();
            }
        });
    }

    private void applyStylesheet(Scene scene, String cssResourcePath) {
        try {
            String cssPath = Objects.requireNonNull(getClass().getResource(cssResourcePath)).toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (NullPointerException e) {
            System.err.println("Warning: Could not load stylesheet " + cssResourcePath);
        } catch (Exception e) {
            System.err.println("Error applying stylesheet " + cssResourcePath + ": " + e.getMessage());
        }
    }

    public void showSettings() { stopCurrentGameLoop(); System.out.println("Show Settings - NIY."); }
    public void showShop() { System.out.println("Show Shop - NIY."); }
    public void hideShop() { System.out.println("Hide Shop - NIY."); }
    public void showGameOver(boolean success, int score, int packetsLost) { stopCurrentGameLoop(); System.out.println("Show GameOver - NIY. Success: " + success); }


    public Stage getPrimaryStage() { return primaryStage; }
    public GameState getGameState() { return gameState; }

    public static void main(String[] args) {
        launch(args);
    }
}