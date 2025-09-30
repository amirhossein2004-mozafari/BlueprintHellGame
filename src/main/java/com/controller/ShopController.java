package com.controller;

import com.blueprinthell.MainApplication;
import com.model.EffectType;
import com.model.GameState;
import com.utils.AudioManager;
import com.api.enums.AbilityType;
import com.utils.NetworkManager;
import org.json.JSONObject;

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

        System.out.println("ShopController: Attempting to send BUY_SKILL request for " + ability.name());

        JSONObject payload = new JSONObject();
        payload.put("abilityType", ability.name());

        NetworkManager.getInstance().sendAction("BUY_SKILL", payload);
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