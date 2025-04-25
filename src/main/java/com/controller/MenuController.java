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

    public void handleStartGameButton() {
        System.out.println("Start Game button clicked - Starting default level 1.");
        mainApp.startGame(1);
    }

    public void handleSelectLevelButton() {
        System.out.println("Select Level button clicked - Not implemented yet.");
    }

    public void handleSettingsButton() {
        System.out.println("Settings button clicked.");
        mainApp.showSettings();
    }

    public void handleExitButton() {
        System.out.println("Exit button clicked - Exiting application.");
        if (mainApp != null && mainApp.getPrimaryStage() != null) {
            mainApp.getPrimaryStage().close();
        } else {
            System.exit(0);
        }
    }
}