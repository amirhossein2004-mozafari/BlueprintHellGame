package com.controller;

import com.blueprinthell.Config;
import com.blueprinthell.MainApplication;
import com.model.GameState;
import com.model.ShopSkill;
import com.utils.AudioManager;

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
        if (gameState == null) {
            System.err.println("ShopController Error: GameState is null. Cannot process skill purchase.");
            return;
        }

        System.out.println("ShopController: Attempting to buy skill - " + skill.name() +
                ", Cost: " + cost + ", Current Coins: " + gameState.getCoins());

        if (gameState.getCoins() >= cost) {
            if ((skill == ShopSkill.ATAR && gameState.isSkillActive(ShopSkill.ATAR)) ||
                    (skill == ShopSkill.AIRYAMAN && gameState.isSkillActive(ShopSkill.AIRYAMAN))) {
                System.out.println("ShopController: Skill " + skill.name() + " is already active. Purchase aborted by controller.");
                return;
            }

            if (gameState.decreaseCoins(cost)) {
                gameState.activateSkill(skill, duration);

                AudioManager.playSoundEffect(AudioManager.SFX_SHOP_BUY);

                System.out.println("ShopController: Skill " + skill.name() + " purchased and activated successfully!");

            } else {
                System.err.println("ShopController Error: Could not decrease coins for skill " + skill.name() +
                        " even though player had enough. Purchase failed.");
            }
        } else {
            System.out.println("ShopController: Not enough coins to buy " + skill.name() +
                    ". Required: " + cost + ", Available: " + gameState.getCoins());
        }
    }

    public void closeShop() {
        System.out.println("ShopController: Requesting to close shop via MainApplication.");
        if (mainApp != null) {
            mainApp.hideShop();
        } else {
            System.err.println("ShopController Error: mainApp reference is null. Cannot close shop.");
        }
    }
}