package com.controller;

import com.blueprinthell.Config;
import com.blueprinthell.MainApplication;
import com.model.GameState;
import com.model.ShopSkill;

public class ShopController {

    private final MainApplication mainApp;
    private final GameState gameState;
    private final GameController gameController;

    public ShopController(MainApplication mainApp, GameState gameState, GameController gameController) {
        this.mainApp = mainApp;
        this.gameState = gameState;
        this.gameController = gameController;
    }

    public void buySkill(ShopSkill skill, double duration, int cost) {
        if (gameState.getCoins() >= cost) {
            if (gameState.decreaseCoins(cost)) {
                gameState.activateSkill(skill, duration);
                System.out.println("Skill " + skill.name() + " purchased!");
            } else {
                System.out.println("Could not decrease coins, purchase failed.");
            }
        } else {
            System.out.println("Not enough coins to buy " + skill.name());
        }
    }

    public void closeShop() {
        System.out.println("ShopController: Requesting to close shop via MainApplication.");
        mainApp.hideShop();
    }
}