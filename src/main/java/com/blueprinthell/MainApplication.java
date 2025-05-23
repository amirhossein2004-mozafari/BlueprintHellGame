package com.blueprinthell;

import com.controller.*;
import com.model.GameState;
import com.utils.LevelLoader;
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
import com.controller.ShopController;
import com.view.ShopView;
import javafx.scene.layout.BorderPane;
import com.view.LevelSelectionView;
import com.utils.AudioManager;
import com.view.SettingsView;
import com.controller.SettingsController;

public class MainApplication extends Application {

    private Stage primaryStage;
    private GameState gameState;
    private GameController gameController;
    private GameView gameView;
    private GameLoop gameLoop;
    private HudView hudView;
    private ShopView shopViewInstance = null;
    private ShopController shopControllerInstance = null;
    private int currentLevelNumber = 0;
    private SettingsController settingsControllerInstance;
    private int currentLevelNumberPlayed = 0;
    private GameState completedGameStateForGameOver;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Blueprint Hell");
        primaryStage.setResizable(false);
        this.settingsControllerInstance = new SettingsController();

        AudioManager.loadSounds();
        AudioManager.playMenuBackgroundMusic(true);

        MenuController menuController = new MenuController(this);
        showMainMenu(menuController);
        primaryStage.show();
    }

    public GameState getGameStateForGameOver() {
        return completedGameStateForGameOver;
    }

    public boolean hasNextLevel(int nextLevelNumber) {
        String fileName = "/levels/level" + nextLevelNumber + ".json";
        try (java.io.InputStream inputStream = LevelLoader.class.getResourceAsStream(fileName)) {
            return inputStream != null;
        } catch (Exception e) {
            System.err.println("Error checking for next level file: " + fileName + " - " + e.getMessage());
            return false;
        }
    }

    public void stopCurrentGameLoop() {
        System.out.println("MainApplication.stopCurrentGameLoop: Initiating cleanup of active game components...");

        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
            System.out.println("MainApplication.stopCurrentGameLoop: GameLoop stopped and nulled.");
        }

        if (gameController != null) {
            gameController = null;
            System.out.println("MainApplication.stopCurrentGameLoop: GameController nulled.");
        }

        if (hudView != null) {
            hudView = null;
            System.out.println("MainApplication.stopCurrentGameLoop: HudView reference nulled.");
        }

        if (shopControllerInstance != null) {
            shopControllerInstance = null;
            System.out.println("MainApplication.stopCurrentGameLoop: ShopController instance nulled.");
        }

        if (primaryStage != null && primaryStage.getScene() != null &&
                primaryStage.getScene().getRoot() instanceof BorderPane) {
            BorderPane rootNode = (BorderPane) primaryStage.getScene().getRoot();
            if (shopViewInstance != null && rootNode.getRight() == shopViewInstance) {
                rootNode.setRight(null);
                System.out.println("MainApplication.stopCurrentGameLoop: ShopView removed from BorderPane's right.");
            }
        }
        if (shopViewInstance != null) {
            shopViewInstance.setVisible(false);
            shopViewInstance = null;
            System.out.println("MainApplication.stopCurrentGameLoop: ShopView instance nulled and set invisible.");
        }
        System.out.println("MainApplication.stopCurrentGameLoop: Cleanup of game components (excluding gameState itself) complete.");
    }

    public void showMainMenu(MenuController controller) {
        System.out.println("MainApplication.showMainMenu: Attempting to show Main Menu...");

        stopCurrentGameLoop();
        AudioManager.stopBackgroundMusic();

        this.gameState = null;
        this.completedGameStateForGameOver = null;
        System.out.println("MainApplication.showMainMenu: Previous game states nulled.");

        if (controller == null) {
            System.err.println("MainApplication.showMainMenu Error: MenuController is null. Creating a default one.");
            controller = new MenuController(this);
        }

        AudioManager.playMenuBackgroundMusic(true);
        System.out.println("MainApplication.showMainMenu: Menu background music started.");

        MenuView menuView = new MenuView(controller);
        Scene menuScene = new Scene(menuView, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);

        applyStylesheet(menuScene, "/styles/style.css");

        if (primaryStage != null) {
            primaryStage.setScene(menuScene);
            System.out.println("MainApplication.showMainMenu: Main Menu scene set on primaryStage.");
        } else {
            System.err.println("MainApplication.showMainMenu Error: primaryStage is null. Cannot set scene.");
        }
    }

    @Override
    public void stop() throws Exception {
        System.out.println("MainApplication.stop(): Application is stopping...");
        AudioManager.stopBackgroundMusic();
        System.out.println("MainApplication.stop(): All background music explicitly stopped.");
        super.stop();
    }

    public void startGame(int levelNumber) {
        this.currentLevelNumberPlayed = levelNumber;
        this.completedGameStateForGameOver = null;

        stopCurrentGameLoop();
        this.gameState = null;

        this.gameState = LevelLoader.loadLevel(levelNumber);

        if (this.gameState == null) {
            System.err.println("MainApplication.startGame: CRITICAL - Failed to load level " + levelNumber + ". GameState is null. Returning to main menu.");
            showMainMenu(new MenuController(this));
            return;
        }
        System.out.println("MainApplication.startGame: GameState for level " + levelNumber + " loaded. isGameRunning: " + this.gameState.isGameRunning());

        double gameViewHeight = Config.WINDOW_HEIGHT - Config.HUD_HEIGHT;
        if (gameViewHeight <= 0) {
            System.err.println("MainApplication.startGame: Warning - gameViewHeight is invalid. Using full window height.");
            gameViewHeight = Config.WINDOW_HEIGHT;
        }
        this.gameView = new GameView(Config.WINDOW_WIDTH, gameViewHeight);
        this.hudView = new HudView(this.gameState);

        BorderPane rootNode = new BorderPane();
        rootNode.setStyle("-fx-background-color: " + Config.COLOR_BACKGROUND.toString().substring(2) + ";");
        rootNode.setTop(this.hudView);
        rootNode.setCenter(this.gameView);

        this.gameController = new GameController(this.gameState, this.gameView, this);
        this.gameController.initializeGame();

        this.shopControllerInstance = new ShopController(this, this.gameState, this.gameController);
        AudioManager.playGameBackgroundMusic(true);
        Scene gameScene = new Scene(rootNode, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        setupInputHandlers(gameScene, this.gameController, this.hudView);
        applyStylesheet(gameScene, "/styles/style.css");

        this.gameLoop = new GameLoop(this.gameController, this.gameState);
        this.gameLoop.start();

        primaryStage.setScene(gameScene);
        System.out.println("MainApplication.startGame: Game scene for level " + levelNumber + " is set. CurrentLevelPlayed: " + this.currentLevelNumberPlayed);
    }



    private void setupInputHandlers(Scene gameScene, GameController controller, HudView pHudView) {
        gameScene.setOnMousePressed(event -> {
            if (gameState != null && !gameState.isGameLogicPaused() && this.gameController != null) {
                this.gameController.handleMousePress(event);
            }
        });
        gameScene.setOnMouseDragged(event -> {
            if (gameState != null && !gameState.isGameLogicPaused() && this.gameController != null) {
                this.gameController.handleMouseDrag(event);
            }
        });
        gameScene.setOnMouseReleased(event -> {
            if (gameState != null && !gameState.isGameLogicPaused() && this.gameController != null) {
                this.gameController.handleMouseRelease(event);
            }
        });

        gameScene.setOnKeyPressed(event -> {
            if (gameState == null) {
                System.out.println("Key Pressed but gameState is null. Ignoring.");
                return;
            }

            KeyCode code = event.getCode();

            if (shopViewInstance != null && shopViewInstance.isVisible() && code == KeyCode.ESCAPE) {
                System.out.println("MainApp (Input): ESC pressed while shop is open - closing shop...");
                hideShop();
                event.consume();
                return;
            }

            if (code == Config.KEY_PAUSE) {
                if (gameState.isGameRunning() && this.gameController != null) {
                    if (shopViewInstance != null && shopViewInstance.isVisible()) {
                        System.out.println("MainApp (Input): Pause key (" + code + ") pressed, but shop is open. General pause ignored.");
                    } else {
                        if (gameState.isGameLogicPaused()) {
                            this.gameController.resumeGameLogic();
                            System.out.println("MainApp (Input): Game RESUMED by Pause Key (" + code + ").");
                        } else {
                            this.gameController.pauseGameLogic();
                            System.out.println("MainApp (Input): Game PAUSED by Pause Key (" + code + ").");
                        }
                    }
                } else {
                    System.out.println("MainApp (Input): Pause key (" + code + ") pressed, but game is not running or gameController is null. Ignoring.");
                }
                event.consume();
                return;
            }

            if (gameState.isGameLogicPaused() && (shopViewInstance == null || !shopViewInstance.isVisible())) {
                System.out.println("MainApp (Input): Key " + code + " pressed while game logic is paused (NOT by shop).");
                if (code == Config.KEY_EXIT_TO_MENU) {
                    System.out.println("MainApp (Input): Exit to Menu key pressed while paused. Returning to main menu.");
                    showMainMenu(new MenuController(this));
                } else {
                    System.out.println("MainApp (Input): Key " + code + " ignored as game is paused (not by shop) and not Exit key.");
                }
                event.consume();
                return;
            }

            if (gameState.isGameRunning() && !gameState.isGameLogicPaused()) {
                if (code == Config.KEY_SHOP) {
                    System.out.println("MainApp (Input): --- SHOP KEY (" + code + ") PRESSED --- Calling showShop()...");
                    showShop();
                } else if (code == Config.KEY_TOGGLE_HUD && pHudView != null) {
                    pHudView.toggleVisibility();
                    System.out.println("MainApp (Input): HUD visibility toggled.");
                } else if (code == Config.KEY_EXIT_TO_MENU) {
                    System.out.println("MainApp (Input): Exit to Menu key pressed. Returning to main menu.");
                    showMainMenu(new MenuController(this));
                } else {
                    if (this.gameController != null) {
                        System.out.println("MainApp (Input): Forwarding key " + code + " to GameController.");
                        this.gameController.handleKeyPress(code);
                    } else {
                        System.out.println("MainApp (Input): Key " + code + " pressed, but GameController is null (should not happen if game is running).");
                    }
                }
            } else if (!gameState.isGameRunning()) {
                System.out.println("MainApp (Input): Key " + code + " pressed, but game is not running (e.g., waiting for Start button). Ignoring general game keys.");
                if (code == KeyCode.ESCAPE || code == Config.KEY_EXIT_TO_MENU) {
                    System.out.println("MainApp (Input): Exit key pressed before game formally started. Closing app.");
                    primaryStage.close();
                }
            }
            event.consume();
        });
    }

    private void applyStylesheet(Scene scene, String cssResourcePath) {
        try {
            String cssPath = java.util.Objects.requireNonNull(getClass().getResource(cssResourcePath)).toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("Warning: Could not load stylesheet: " + cssResourcePath + " - " + e.getMessage());
        }
    }

    public void showSettings() {
        stopCurrentGameLoop();
        System.out.println("Attempting to show Settings Screen...");
        MenuController currentMenuController = new MenuController(this);
        SettingsView settingsView = new SettingsView(this, settingsControllerInstance, currentMenuController);
        Scene settingsScene = new Scene(settingsView, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        applyStylesheet(settingsScene, "/styles/style.css");
        primaryStage.setScene(settingsScene);
        System.out.println("Settings Screen shown.");
    }

    public void showShop() {
        if (gameState == null || gameController == null || shopControllerInstance == null) {
            System.err.println("MainApp Error: Cannot show shop. GameState, GameController, or ShopController is null.");
            return;
        }

        if (!(primaryStage.getScene().getRoot() instanceof BorderPane rootNode)) {
            System.err.println("MainApp Error: Root node is not a BorderPane. Cannot display shop.");
            return;
        }

        if (shopViewInstance != null && rootNode.getRight() == shopViewInstance && shopViewInstance.isVisible()) {
            System.out.println("MainApp: Shop key pressed, but shop is ALREADY visible. Closing it instead.");
            hideShop();
            return;
        }

        if (!gameState.isGameLogicPaused()) {
            gameController.pauseGameLogic();
            System.out.println("MainApp: Game logic PAUSED for showing shop.");
        } else {
            System.out.println("MainApp: Game logic was ALREADY paused when trying to show shop.");
        }

        if (shopViewInstance == null) {
            shopViewInstance = new ShopView(shopControllerInstance, gameState);
            System.out.println("MainApp: New ShopView instance created and assigned.");
        } else {
            System.out.println("MainApp: Reusing existing ShopView instance.");
        }

        rootNode.setRight(shopViewInstance);
        shopViewInstance.setVisible(true);
        shopViewInstance.requestFocus();

        System.out.println("MainApp: ShopView instance (" + shopViewInstance.hashCode() +
                ") set to BorderPane's right. IsVisible: " + shopViewInstance.isVisible() +
                ", Parent: " + shopViewInstance.getParent());

        javafx.application.Platform.runLater(() -> {
            if (shopViewInstance.getParent() == rootNode) {
                System.out.println("MainApp (runLater): ShopView IS a child of rootNode. Bounds: " +
                        shopViewInstance.getBoundsInParent());
            } else {
                System.err.println("MainApp (runLater) ERROR: ShopView is NOT a child of rootNode or its parent is null!");
            }
            if(!primaryStage.isShowing() || !primaryStage.isFocused()){
                System.err.println("MainApp (runLater) WARNING: Primary stage might not be showing or focused.");
            }
        });
    }

    public void hideShop() {

        if (shopViewInstance == null) {
            System.out.println("MainApp (hideShop): No ShopView instance to hide.");
            if (gameState != null && gameState.isGameLogicPaused() && gameController != null) {
                gameController.resumeGameLogic();
                System.out.println("MainApp (hideShop): Game logic resumed (shop instance was null).");
            }
            return;
        }

        if (primaryStage.getScene() != null && primaryStage.getScene().getRoot() instanceof BorderPane rootNode) {
            if (rootNode.getRight() == shopViewInstance) {
                rootNode.setRight(null);
                System.out.println("MainApp: ShopView removed from BorderPane.");
            } else {
                System.out.println("MainApp (hideShop): ShopView was not the right child of BorderPane or root is not BorderPane.");
            }
        } else {
            System.out.println("MainApp (hideShop): Scene or root is null or not BorderPane.");
        }
        shopViewInstance.setVisible(false);

        if (gameState != null && gameState.isGameLogicPaused() && gameController != null) {
            gameController.resumeGameLogic();
            System.out.println("MainApp: Game logic RESUMED after hiding shop.");
        } else {
            System.out.println("MainApp (hideShop): Game logic was not paused or gameState/gameController is null.");
        }
    }

    public void showLevelSelectionScreen(MenuController menuController) {
        stopCurrentGameLoop();
        System.out.println("Attempting to show Level Selection Screen...");
        LevelSelectionView levelSelectionView = new LevelSelectionView(this, menuController);
        Scene levelSelectionScene = new Scene(levelSelectionView, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        applyStylesheet(levelSelectionScene, "/styles/style.css");
        primaryStage.setScene(levelSelectionScene);
        System.out.println("Level Selection Screen shown.");
    }

    public ShopView getShopViewInstance() { return shopViewInstance; }

    public void showGameOver(boolean success, int score, int packetsLostOrPercentage) {
        System.out.println("MainApplication.showGameOver: Called. Success: " + success + ", Score: " + score +
                ", Loss/Info: " + packetsLostOrPercentage + ", LevelPlayed: " + currentLevelNumberPlayed);

        AudioManager.stopBackgroundMusic();
        System.out.println("MainApp.showGameOver: Background music stopped.");

        if (success) {
            AudioManager.playSoundEffect(AudioManager.SFX_LEVEL_COMPLETE);
            System.out.println("MainApp.showGameOver: LEVEL_COMPLETE sound effect played.");
        } else {
            AudioManager.playSoundEffect(AudioManager.SFX_GAME_OVER);
            System.out.println("MainApp.showGameOver: GAME_OVER sound effect played.");
        }

        if (this.gameState != null) {
            this.completedGameStateForGameOver = this.gameState;
        } else if (this.completedGameStateForGameOver == null && currentLevelNumberPlayed > 0) {
            System.err.println("MainApp.showGameOver WARNING: Current gameState is null, creating placeholder for GameOverView.");
            this.completedGameStateForGameOver = new GameState(currentLevelNumberPlayed,0,0);
        }

        stopCurrentGameLoop();

        this.gameState = null;
        System.out.println("MainApp.showGameOver: Main gameState nulled.");


        GameOverView gameOverView = new GameOverView(this, success, score, packetsLostOrPercentage, this.currentLevelNumberPlayed);
        Scene gameOverScene = new Scene(gameOverView, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        applyStylesheet(gameOverScene, "/styles/style.css");

        primaryStage.setScene(gameOverScene);
        System.out.println("MainApp.showGameOver: Game Over/Win screen shown for level " + this.currentLevelNumberPlayed);

    }
    public Stage getPrimaryStage() { return primaryStage; }
    public GameState getGameState() { return gameState; }

    public static void main(String[] args) {
        launch(args);
    }
}