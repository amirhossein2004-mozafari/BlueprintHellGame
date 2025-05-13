package com.blueprinthell;

import com.controller.*;
import com.model.GameState;
import com.view.*;
import com.controller.ShopController;
import com.view.ShopView;

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
    private ShopView shopViewInstance = null;

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
        shopViewInstance = null;
    }

    public void showMainMenu(MenuController controller) {
        stopCurrentGameLoop();
        System.out.println("Show Main Menu ");
    }

    public void startGame(int levelNumber) {
        stopCurrentGameLoop();
        gameState = new GameState();

        double gameViewHeight = Config.WINDOW_HEIGHT - Config.HUD_HEIGHT;
        if (gameViewHeight < 0) {
            System.err.println("Warning: HUD_HEIGHT might be larger than WINDOW_HEIGHT. Setting GameView height to full window.");
            gameViewHeight = Config.WINDOW_HEIGHT;
        }
        GameView gameView = new GameView(Config.WINDOW_WIDTH, gameViewHeight);
        this.hudView = new HudView(gameState);

        BorderPane rootNode = new BorderPane();
        rootNode.setTop(this.hudView);
        rootNode.setCenter(gameView);


        gameController = new GameController(gameState, gameView, this);
        gameController.initializeGame();

        Scene gameScene = new Scene(rootNode, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        setupInputHandlers(gameScene, gameController, this.hudView);
        applyStylesheet(gameScene, "/styles/style.css");

        gameLoop = new GameLoop(gameController, gameState);
        gameLoop.start();
        primaryStage.setScene(gameScene);
        System.out.println("Game scene set with Top HUD and loop started for level " + levelNumber);
    }

    private void setupInputHandlers(Scene gameScene, GameController controller, HudView pHudView) {
        gameScene.setOnMousePressed(event -> {
            if (gameState != null && !gameState.isGameLogicPaused()) controller.handleMousePress(event);
        });
        gameScene.setOnMouseDragged(event -> {
            if (gameState != null && !gameState.isGameLogicPaused()) controller.handleMouseDrag(event);
        });
        gameScene.setOnMouseReleased(event -> {
            if (gameState != null && !gameState.isGameLogicPaused()) controller.handleMouseRelease(event);
        });

        gameScene.setOnKeyPressed(event -> {
            if (gameState == null) return;
            KeyCode code = event.getCode();

            if (gameState.isGameLogicPaused()) {
                if (code == KeyCode.ESCAPE && shopViewInstance != null && shopViewInstance.isVisible()) {
                    System.out.println("ESC pressed while shop is open - closing shop...");
                    hideShop();
                }
            } else {
                if (code == Config.KEY_SHOP) {
                    System.out.println("--- SHOP KEY PRESSED --- Calling showShop()...");
                    showShop();
                } else if (code == Config.KEY_TOGGLE_HUD && pHudView != null) {
                    pHudView.toggleVisibility();
                } else if (code == KeyCode.ESCAPE) {
                    System.out.println("Escape pressed, exiting application.");
                    stopCurrentGameLoop();
                    primaryStage.close();
                } else {
                    controller.handleKeyPress(code);
                }
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

    public void showShop() {
        if (gameState == null || gameController == null) {
            System.err.println("Cannot show shop: GameState or GameController is null.");
            return;
        }

        if (!gameState.isGameLogicPaused()) {
            gameController.pauseGameLogic();

            if (shopViewInstance == null) {
                ShopController shopCtrl = new ShopController(this, gameState, gameController);
                shopViewInstance = new ShopView(shopCtrl, gameState);
            } else {
                shopViewInstance.updateAvailableCoins(gameState.getCoins());
            }

            if (primaryStage.getScene().getRoot() instanceof BorderPane rootNode) {
                if (rootNode.getCenter() == shopViewInstance) rootNode.setCenter(null);
                if (rootNode.getRight() == shopViewInstance) rootNode.setRight(null);

                rootNode.setRight(shopViewInstance);
                shopViewInstance.setVisible(true);
                System.out.println("ShopView set to BorderPane right side.");
            } else {
                System.err.println("Cannot show shop: Root node is not a BorderPane to set right child.");
                gameController.resumeGameLogic();
            }
        } else {
            System.out.println("Shop key pressed, but game logic already paused. Shop might be open or other pause.");
            if(shopViewInstance != null && shopViewInstance.isVisible()){
                hideShop();
            }
        }
    }

    public void hideShop() {
        if (gameState == null || gameController == null) return;

        if (primaryStage.getScene().getRoot() instanceof BorderPane rootNode && shopViewInstance != null) {
            if (rootNode.getRight() == shopViewInstance) {
                rootNode.setRight(null);
                System.out.println("ShopView removed from BorderPane right side.");
            }
            shopViewInstance.setVisible(false);
        }

        if (gameState.isGameLogicPaused()) {
            gameController.resumeGameLogic();
        }
    }

    public ShopView getShopViewInstance() {
        return shopViewInstance;
    }

    public void showGameOver(boolean success, int score, int packetsLost) {
        stopCurrentGameLoop();
        System.out.println(">>>> GAME OVER! Success: " + success + " | Score: " + score + " | Packets Lost: " + packetsLost + " <<<<");    }

    public Stage getPrimaryStage() { return primaryStage; }
    public GameState getGameState() { return gameState; }

    public static void main(String[] args) {
        launch(args);
    }
}