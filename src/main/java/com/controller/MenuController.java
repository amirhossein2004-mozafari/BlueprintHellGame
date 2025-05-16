package com.controller;

import com.blueprinthell.MainApplication;

public class MenuController {
    private final MainApplication mainApp;

    public MenuController(MainApplication mainApp) {
        this.mainApp = mainApp;
    }

    public void handleStartGameButton() {
        System.out.println("Menu: Start Game button pressed.");
        mainApp.startGame(1);
    }

    public void handleSelectLevelButton() {
        System.out.println("Menu: Select Level button pressed - Not implemented yet.");
    }

    public void handleSettingsButton() {
        System.out.println("Menu: Settings button pressed.");
        mainApp.showSettings();
    }

    public void handleExitButton() {
        System.out.println("Menu: Exit button pressed.");
        mainApp.getPrimaryStage().close();
    }
}