package com.controller;

import com.blueprinthell.MainApplication;
import com.model.EffectType;
import com.model.GameState;
import com.utils.AudioManager;
import com.api.enums.AbilityType;

public class ShopController {

    private final MainApplication mainApp;
    private final GameState gameState;
    private final GameController gameController;

    public ShopController(MainApplication mainApp, GameState gameState, GameController gameController) {
        this.mainApp = mainApp;
        this.gameState = gameState;
        this.gameController = gameController;
    }

    public void buySkill(AbilityType ability, double duration, int cost) {
        if (gameState == null) {
            System.err.println("ShopController Error: GameState is null. Cannot process skill purchase.");
            return;
        }

        System.out.println("ShopController: Attempting to buy skill - " + ability.name() +
                ", Cost: " + cost + ", Current Coins: " + gameState.getCoins());

        if (gameState.getCoins() >= cost) {
            if ((ability == AbilityType.ATAR && gameState.isSkillActive(AbilityType.ATAR)) ||
                    (ability == AbilityType.AIRYAMAN && gameState.isSkillActive(AbilityType.AIRYAMAN))) {
                System.out.println("ShopController: Skill " + ability.name() + " is already active. Purchase aborted by controller.");
                return;
            }

            if (gameState.decreaseCoins(cost)) {
                gameState.activateSkill(ability, duration);

                AudioManager.playSoundEffect(AudioManager.SFX_SHOP_BUY);

                System.out.println("ShopController: Skill " + ability.name() + " purchased and activated successfully!");

            } else {
                System.err.println("ShopController Error: Could not decrease coins for skill " + ability.name() +
                        " even though player had enough. Purchase failed.");
            }
        } else {
            System.out.println("ShopController: Not enough coins to buy " + ability.name() +
                    ". Required: " + cost + ", Available: " + gameState.getCoins());
        }
    }


    public void enterPlacementMode(EffectType effectType, int cost, double duration) {
        if (gameState.getCoins() >= cost) {
            System.out.println("Entering placement mode for: " + effectType);
            gameController.startEffectPlacement(effectType, cost, duration);
            closeShop();
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