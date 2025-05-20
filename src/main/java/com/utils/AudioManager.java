package com.utils;

import com.blueprinthell.Config;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AudioManager {

    private static final Map<String, AudioClip> soundEffects = new HashMap<>();
    private static MediaPlayer backgroundMusicPlayerMenu;
    private static MediaPlayer backgroundMusicPlayerGame;

    private static double globalVolume = 0.5;
    private static double musicVolumeFactor = 0.5;
    private static double sfxVolumeFactor = 0.8;
    public static final String SFX_CONNECT_SUCCESS = "CONNECT_SUCCESS";
    public static final String SFX_PACKET_DAMAGE = "PACKET_DAMAGE";
    public static final String SFX_SHOP_BUY = "SHOP_BUY";
    public static final String SFX_LEVEL_COMPLETE = "LEVEL_COMPLETE";
    public static final String SFX_GAME_OVER = "GAME_OVER";

    public static void loadSounds() {
        System.out.println("AudioManager: Loading sound effects using paths from Config.java...");
        try {
            loadSoundEffect(SFX_CONNECT_SUCCESS, Config.SOUND_CONNECT_SUCCESS);
            loadSoundEffect(SFX_PACKET_DAMAGE, Config.SOUND_PACKET_DAMAGE);
            loadSoundEffect(SFX_SHOP_BUY, Config.SOUND_SHOP_BUY);
            loadSoundEffect(SFX_LEVEL_COMPLETE, Config.SOUND_LEVEL_COMPLETE);
            loadSoundEffect(SFX_GAME_OVER, Config.SOUND_GAME_OVER);

            System.out.println("AudioManager: Sound effects loaded.");
        } catch (Exception e) {
            System.err.println("AudioManager: Error during sound effect loading.");
            e.printStackTrace();
        }
    }

    private static void loadSoundEffect(String keyName, String filePath) {
        try {
            URL resourceUrl = AudioManager.class.getResource(filePath);
            if (resourceUrl == null) {
                System.err.println("AudioManager Error: Could not find sound effect file: " + filePath + " for key: " + keyName);
                return;
            }
            AudioClip clip = new AudioClip(resourceUrl.toExternalForm());
            soundEffects.put(keyName, clip);
            System.out.println("AudioManager: Loaded SFX - " + keyName + " from " + filePath);
        } catch (Exception e) {
            System.err.println("AudioManager Error: Failed to load sound effect: " + keyName + " from " + filePath);
            e.printStackTrace();
        }
    }

    public static void playSoundEffect(String keyName) {
        AudioClip clip = soundEffects.get(keyName);
        if (clip != null) {
            clip.setVolume(globalVolume * sfxVolumeFactor);
            clip.play();
            System.out.println("AudioManager: Playing SFX - " + keyName);
        } else {
            System.err.println("AudioManager Error: Sound effect not found for key: " + keyName);
        }
    }

    public static void playMenuBackgroundMusic(boolean loop) {
        playSpecificBackgroundMusic(Config.SOUND_BACKGROUND_MUSIC, true, MusicType.MENU);
    }

    public static void playGameBackgroundMusic(boolean loop) {
        playSpecificBackgroundMusic(Config.SOUND_BACKGROUND_MUSIC, true, MusicType.GAME);
    }

    private enum MusicType { MENU, GAME }

    private static void playSpecificBackgroundMusic(String musicFilePath, boolean loop, MusicType type) {
        stopSpecificBackgroundMusic(type == MusicType.MENU ? backgroundMusicPlayerMenu : backgroundMusicPlayerGame);

        MediaPlayer currentPlayer;
        if (type == MusicType.MENU) {
            if (backgroundMusicPlayerGame != null) backgroundMusicPlayerGame.setMute(true);
        } else {
            if (backgroundMusicPlayerMenu != null) backgroundMusicPlayerMenu.setMute(true);
        }

        try {
            URL resourceUrl = AudioManager.class.getResource(musicFilePath);
            if (resourceUrl == null) {
                System.err.println("AudioManager Error: Could not find background music file: " + musicFilePath);
                return;
            }
            Media media = new Media(resourceUrl.toExternalForm());
            currentPlayer = new MediaPlayer(media);
            currentPlayer.setVolume(globalVolume * musicVolumeFactor);
            currentPlayer.setMute(false);
            if (loop) {
                currentPlayer.setOnEndOfMedia(() -> currentPlayer.seek(Duration.ZERO));
            }
            currentPlayer.play();

            if (type == MusicType.MENU) {
                backgroundMusicPlayerMenu = currentPlayer;
                System.out.println("AudioManager: Playing MENU background music - " + musicFilePath);
            } else {
                backgroundMusicPlayerGame = currentPlayer;
                System.out.println("AudioManager: Playing GAME background music - " + musicFilePath);
            }
        } catch (Exception e) {
            System.err.println("AudioManager Error: Playing background music: " + musicFilePath);
            e.printStackTrace();
        }
    }

    public static void stopBackgroundMusic() {
        stopSpecificBackgroundMusic(backgroundMusicPlayerMenu);
        backgroundMusicPlayerMenu = null;
        stopSpecificBackgroundMusic(backgroundMusicPlayerGame);
        backgroundMusicPlayerGame = null;
        System.out.println("AudioManager: All background music stopped.");
    }

    private static void stopSpecificBackgroundMusic(MediaPlayer player) {
        if (player != null) {
            player.stop();
        }
    }

    public static void setGlobalVolume(double volume) {
        globalVolume = Math.max(0.0, Math.min(1.0, volume));
        if (backgroundMusicPlayerMenu != null && backgroundMusicPlayerMenu.getStatus() == MediaPlayer.Status.PLAYING) {
            backgroundMusicPlayerMenu.setVolume(globalVolume * musicVolumeFactor);
        }
        if (backgroundMusicPlayerGame != null && backgroundMusicPlayerGame.getStatus() == MediaPlayer.Status.PLAYING) {
            backgroundMusicPlayerGame.setVolume(globalVolume * musicVolumeFactor);
        }
        System.out.println("AudioManager: Global volume set to " + String.format("%.2f", globalVolume));
    }

    public static double getGlobalVolume() {
        return globalVolume;
    }
}