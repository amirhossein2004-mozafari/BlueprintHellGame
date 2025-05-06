package com.blueprinthell;

import com.controller.*;
import com.model.GameState;
import com.view.*;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;
import javafx.scene.layout.BorderPane;

public class MainApplication extends Application {

    private Stage primaryStage;
    private GameState gameState;
    private GameController gameController;
    private GameLoop gameLoop;
    private HudView hudView;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Blueprint Hell");
        primaryStage.setResizable(false);
        startGame(1);
        primaryStage.show();
    }

    private void stopCurrentGameLoop() {
        if (gameLoop != null) { gameLoop.stop(); gameLoop = null; }
        if (gameController != null) { gameController = null; }
        gameState = null;
        hudView = null;
    }

    public void showMainMenu(MenuController controller) {
        stopCurrentGameLoop();
        System.out.println("Show Main Menu ");
    }

    public void startGame(int levelNumber) {
        stopCurrentGameLoop();
        gameState = new GameState();
        double gameViewHeight = Config.WINDOW_HEIGHT - 30.0;
        GameView gameView = new GameView(Config.WINDOW_WIDTH, gameViewHeight);
        this.hudView = new HudView(gameState);

        BorderPane rootNode = new BorderPane();
        rootNode.setTop(hudView);
        rootNode.setCenter(gameView);
        gameController = new GameController(gameState, gameView, this);
        gameController.initializeGame();

        Scene gameScene = new Scene(rootNode, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        setupInputHandlers(gameScene, gameController, hudView);
        applyStylesheet(gameScene, "/styles/style.css");

        gameLoop = new GameLoop(gameController);
        gameLoop.start();

        primaryStage.setScene(gameScene);
        System.out.println("Game scene with Top HUD set and loop started for level " + levelNumber);
    }

    private void setupInputHandlers(Scene gameScene, GameController controller, HudView pHudView) {
        gameScene.setOnMousePressed(event -> { if (gameState != null) controller.handleMousePress(event); });
        gameScene.setOnMouseDragged(event -> { if (gameState != null) controller.handleMouseDrag(event); });
        gameScene.setOnMouseReleased(event -> { if (gameState != null) controller.handleMouseRelease(event); });

        gameScene.setOnKeyPressed(event -> {
            if (gameState == null) return;
            KeyCode code = event.getCode();

            if (code == Config.KEY_TOGGLE_HUD && pHudView != null) {
                System.out.println("--- HUD TOGGLE KEY PRESSED --- Toggling HUD visibility...");
              //  pHudView.toggleVisibility();
            } else if (code == KeyCode.ESCAPE) {
                System.out.println("Escape pressed, exiting application.");
                stopCurrentGameLoop();
                primaryStage.close();
            } else {
                controller.handleKeyPress(code);
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

    public void showGameOver(boolean success, int score, int packetsLost) {
        stopCurrentGameLoop();
        System.out.println(">>>> GAME OVER! Success: " + success + " | Score: " + score + " | Packets Lost: " + packetsLost + " <<<<");    }

    public Stage getPrimaryStage() { return primaryStage; }
    public GameState getGameState() { return gameState; }

    public static void main(String[] args) {
        launch(args);
    }
}