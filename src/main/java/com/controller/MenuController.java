package com.controller;

import com.blueprinthell.MainApplication;

public class MenuController {
    private final MainApplication mainApp;

    public MenuController(MainApplication mainApp) {
        if (mainApp == null) {
            throw new IllegalArgumentException("MainApplication cannot be null for MenuController");
        }
        this.mainApp = mainApp;
    }

    public void handleStartGameButton(int levelNumber) {
        System.out.println("Menu: Start Game (Level " + levelNumber + ") button pressed.");
        mainApp.startGame(levelNumber);
    }

    public void handleSelectLevelButton() {
        System.out.println("Menu: Select Level button pressed.");
        mainApp.showLevelSelectionScreen(this);
    }

    public void handleSettingsButton() {
        System.out.println("Menu: Settings button pressed.");
        if (mainApp != null) {
            mainApp.showSettings();
        }
    }

    public void handleExitButton() {
        System.out.println("Menu: Exit button pressed.");
        if (mainApp != null && mainApp.getPrimaryStage() != null) {
            mainApp.getPrimaryStage().close();
        }
    }
}