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
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.util.Objects;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;


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
        MenuController menuController = new MenuController(this);
        showMainMenu(menuController);
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

        if (controller == null) {
            System.err.println("MainApp Error: MenuController is null for showMainMenu.");
            return;
        }
        MenuView menuView = new MenuView(controller);
        Scene menuScene = new Scene(menuView, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        applyStylesheet(menuScene, "/styles/style.css");
        primaryStage.setScene(menuScene);
        System.out.println("Main Menu shown.");
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

    public void showSettings() {
        stopCurrentGameLoop();
        System.out.println("Show Settings called - View not implemented yet.");
        showMainMenu(new MenuController(this));
    }
    public void showShop() {
        if (gameState == null || gameController == null) {
            System.err.println("MainApp: Cannot show shop - GameState or GameController is null.");
            return;
        }

        System.out.println("--- Attempting to show RED Test Rectangle ---");

        if (!gameState.isGameLogicPaused()) {
            System.out.println("MainApp: Pausing game logic to show test rect.");
            gameController.pauseGameLogic();
            if (gameLoop != null) gameLoop.stop();

            if (!gameState.isGameLogicPaused()){
                System.err.println("MainApp Warning: Game logic did not pause as expected. Forcing pause.");
                gameState.setGameLogicPaused(true);
            }

            if (primaryStage.getScene().getRoot() instanceof BorderPane rootNode) {
                System.out.println("MainApp: Root is BorderPane. Creating RED test rectangle.");
                Rectangle testRect = new Rectangle(250, 300, Color.RED);
                testRect.setStroke(Color.YELLOW);
                testRect.setStrokeWidth(3);

                if (rootNode.getRight() != null) {
                    System.out.println("MainApp: Clearing previous content from BorderPane.right.");
                    rootNode.setRight(null);
                }

                rootNode.setRight(testRect);
                testRect.setVisible(true);

                System.out.println("MainApp: RED Test Rectangle should be on the right side and visible.");
                javafx.application.Platform.runLater(() -> {
                    System.out.println("MainApp (runLater): TestRect actual size: " + testRect.getWidth() + "x" + testRect.getHeight());
                    System.out.println("MainApp (runLater): TestRect parent: " + testRect.getParent());
                    System.out.println("MainApp (runLater): RootNode right child: " + rootNode.getRight());
                });

            } else {
                System.err.println("MainApp: Cannot show test rect - Root node is not a BorderPane.");
                gameController.resumeGameLogic();
            }
        } else {
            System.out.println("MainApp: Shop key pressed, but game logic already paused.");
        }
    }

    public void hideShop() {
        if (gameState == null || gameController == null) return;
        System.out.println("--- Attempting to hide (remove RED Test Rectangle) ---");
        if (primaryStage.getScene().getRoot() instanceof BorderPane rootNode) {
            if (rootNode.getRight() instanceof Rectangle) {
                rootNode.setRight(null);
                System.out.println("MainApp: RED Test Rectangle removed from BorderPane right side.");
            }
        }
        if (gameState.isGameLogicPaused()) {
            System.out.println("MainApp: Resuming game logic after attempting to hide element.");
            gameController.resumeGameLogic();
            if (gameLoop != null && !gameState.isGameRunning()) {
            } else if (gameLoop != null && gameState.isGameRunning()){
                System.out.println("MainApp: GameLoop was running, consider if it needs restart from here.");
            }
        }
    }

    public ShopView getShopViewInstance() { return shopViewInstance; }
    public void showGameOver(boolean success, int score, int packetsLost) {
        stopCurrentGameLoop();
        System.out.println(">>>> GAME OVER! Success: " + success + " | Score: " + score + " | Packets Lost: " + packetsLost + " <<<<");
    }
    public Stage getPrimaryStage() { return primaryStage; }
    public GameState getGameState() { return gameState; }

    public static void main(String[] args) {
        launch(args);
    }
}