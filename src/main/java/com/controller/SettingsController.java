package com.controller;

import com.blueprinthell.Config;
import com.utils.AudioManager;
import com.view.SettingsView;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class SettingsController {

    private String currentActionToRebind = null;
    private SettingsView settingsViewInstance = null;

    public SettingsController() {
    }

    public void setGlobalVolume(double volume) {
        AudioManager.setGlobalVolume(volume);
    }

    public double getCurrentGlobalVolume() {
        return AudioManager.getGlobalVolume();
    }

    public void startKeyRebindingProcess(String actionKeyName, String actionDisplayName, SettingsView viewInstance) {
        this.currentActionToRebind = actionKeyName;
        this.settingsViewInstance = viewInstance;

        if (viewInstance == null || viewInstance.getScene() == null) {
            System.err.println("SettingsController Error: SettingsView or its Scene is null. Cannot set key listener.");
            this.currentActionToRebind = null;
            return;
        }

        System.out.println("SettingsController: Waiting for new key for action: " + actionDisplayName + " (Internal KeyName: " + actionKeyName + ")");
        viewInstance.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::handleNewKeyInput);
    }

    private void handleNewKeyInput(KeyEvent event) {
        Scene sceneOfSettingsView = (settingsViewInstance != null) ? settingsViewInstance.getScene() : null;

        try {
            if (currentActionToRebind == null || settingsViewInstance == null || sceneOfSettingsView == null) {
                return;
            }

            KeyCode newKeyCode = event.getCode();
            event.consume();

            System.out.println("SettingsController (handleNewKeyInput): Key " + newKeyCode.getName() + " pressed for action: " + currentActionToRebind);

            if (newKeyCode == KeyCode.ESCAPE) {
                System.out.println("SettingsController: Key rebinding cancelled by user (ESC) for action: " + currentActionToRebind);
                return;
            }

            if (isKeyAlreadyBound(newKeyCode, currentActionToRebind)) {
                System.out.println("SettingsController: Key " + newKeyCode.getName() + " is already bound (excluding " + currentActionToRebind + ").");
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("خطا در تغییر دکمه");
                errorAlert.setHeaderText("کلید تکراری");
                errorAlert.setContentText("کلید '" + newKeyCode.getName() + "' از قبل برای عملیات دیگری استفاده شده است.");
                errorAlert.showAndWait();
                return;
            }

            KeyCode oldKeyCode = getCurrentConfigValue(currentActionToRebind);
            updateKeyCodeInConfig(currentActionToRebind, newKeyCode);
            KeyCode updatedKeyCodeAfterChange = getCurrentConfigValue(currentActionToRebind);

            settingsViewInstance.updateKeyBindingDisplay(currentActionToRebind, newKeyCode);

            System.out.println("SettingsController: Key for '" + currentActionToRebind + "' changed from '" +
                    (oldKeyCode != null ? oldKeyCode.getName() : "N/A") + "' to '" + newKeyCode.getName() + "'. " +
                    "Config."+currentActionToRebind+" is now: " + (updatedKeyCodeAfterChange != null ? updatedKeyCodeAfterChange.getName() : "ERROR"));

        } finally {
            if (sceneOfSettingsView != null) {
                sceneOfSettingsView.removeEventFilter(KeyEvent.KEY_PRESSED, this::handleNewKeyInput);
            }
            currentActionToRebind = null;
        }
    }

    private boolean isKeyAlreadyBound(KeyCode keyCodeToCheck, String currentActionToExclude) {
        if (Config.KEY_TIME_FORWARD == keyCodeToCheck && !"KEY_TIME_FORWARD".equals(currentActionToExclude)) return true;
        if (Config.KEY_TIME_BACKWARD == keyCodeToCheck && !"KEY_TIME_BACKWARD".equals(currentActionToExclude)) return true;
        if (Config.KEY_SHOP == keyCodeToCheck && !"KEY_SHOP".equals(currentActionToExclude)) return true;
        if (Config.KEY_TOGGLE_HUD == keyCodeToCheck && !"KEY_TOGGLE_HUD".equals(currentActionToExclude)) return true;
        if (Config.KEY_PAUSE == keyCodeToCheck && !"KEY_PAUSE".equals(currentActionToExclude)) return true;
        if (Config.KEY_EXIT_TO_MENU == keyCodeToCheck && !"KEY_EXIT_TO_MENU".equals(currentActionToExclude)) return true;
        return false;
    }

    private void updateKeyCodeInConfig(String actionKeyName, KeyCode newKeyCode) {
        System.out.println("SettingsController: Updating Config." + actionKeyName + " to " + newKeyCode.getName());
        switch (actionKeyName) {
            case "KEY_TIME_FORWARD": Config.KEY_TIME_FORWARD = newKeyCode; break;
            case "KEY_TIME_BACKWARD": Config.KEY_TIME_BACKWARD = newKeyCode; break;
            case "KEY_SHOP": Config.KEY_SHOP = newKeyCode; break;
            case "KEY_TOGGLE_HUD": Config.KEY_TOGGLE_HUD = newKeyCode; break;
            case "KEY_PAUSE": Config.KEY_PAUSE = newKeyCode; break;
            case "KEY_EXIT_TO_MENU": Config.KEY_EXIT_TO_MENU = newKeyCode; break;
            default:
                System.err.println("SettingsController Error: Unknown actionKeyName for updating Config: " + actionKeyName);
        }
    }

    private KeyCode getCurrentConfigValue(String actionKeyName) {
        if (actionKeyName == null) return null;
        switch (actionKeyName) {
            case "KEY_TIME_FORWARD": return Config.KEY_TIME_FORWARD;
            case "KEY_TIME_BACKWARD": return Config.KEY_TIME_BACKWARD;
            case "KEY_SHOP": return Config.KEY_SHOP;
            case "KEY_TOGGLE_HUD": return Config.KEY_TOGGLE_HUD;
            case "KEY_PAUSE": return Config.KEY_PAUSE;
            case "KEY_EXIT_TO_MENU": return Config.KEY_EXIT_TO_MENU;
            default:
                return null;
        }
    }
}