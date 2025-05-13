package com.blueprinthell;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public final class Config {

    public static final double WINDOW_WIDTH = 1024;
    public static final double WINDOW_HEIGHT = 768;
    public static final double HUD_HEIGHT = 30.0;


    public static final double STAGE_DURATION_SECONDS = 60.0;

    public static final double MAX_PACKET_LOSS_PERCENTAGE = 50.0;
    public static final double INITIAL_WIRE_LENGTH = 5000.0;
    public static final int SYSTEM_PACKET_STORAGE_CAPACITY = 5;
    public static final double COLLISION_DEVIATION_THRESHOLD = 15.0;
    public static final int COINS_PER_PACKET_DELIVERED = 1;

    public static final double BASE_PACKET_SPEED = 100.0;
    public static final double TRIANGLE_PACKET_ACCELERATION = 20.0;
    public static final double SPEED_FACTOR_INCOMPATIBLE_START = 0.5;


    public static final double COLLISION_IMPACT_RADIUS = 100.0;
    public static final double COLLISION_IMPACT_BASE_FORCE = 100.0;
    public static final double PACKET_STABILITY_REDUCTION_FACTOR = 0.15;
    public static final double PACKET_NOISE_TO_STABILITY_FACTOR = 0.1;
    public static final double WIRE_RETURN_FORCE_STRENGTH = 0.6;
    public static final double OFF_WIRE_FRICTION = 0.99;

    public static final int ATAR_SKILL_COST = 3;
    public static final int AIRYAMAN_SKILL_COST = 4;
    public static final int ANAHITA_SKILL_COST = 5;
    public static final double ATAR_SKILL_DURATION_SECONDS = 10.0;
    public static final double AIRYAMAN_SKILL_DURATION_SECONDS = 5.0;

    public static final double TIME_SCALE_INCREMENT = 0.2;
    public static final double MAX_TIME_SCALE = 3.0;
    public static final double MIN_TIME_SCALE = 0.1;
    public static final double DEFAULT_TIME_SCALE = 1.0;

    public static final Duration HUD_AUTO_HIDE_DELAY = Duration.seconds(3);

    public static final double SYSTEM_WIDTH = 80.0;
    public static final double SYSTEM_HEIGHT = 75.0;
    public static final double SYSTEM_CORNER_ARC = 14.0;
    public static final double SYSTEM_DIVIDER_X_OFFSET = 20.0;
    public static final double PORT_OFFSET_X = 8.0;
    public static final double PORT_BASE_SIZE_UNIT = 4.0;
    public static final double PORT_VISUAL_SIZE_SQUARE = 4.0;
    public static final double PORT_VISUAL_SIZE_TRIANGLE = 6.0 ;
    public static final double INDICATOR_HEIGHT = 4.0;
    public static final double INDICATOR_PADDING_X = 5.0;
    public static final double INDICATOR_Y_POS = 5.0;
    public static final double WIRE_THICKNESS = 2.0;
    public static final double WIRE_INDICATOR_WIDTH = 120.0;
    public static final double WIRE_INDICATOR_HEIGHT = 35.0;
    public static final double WIRE_INDICATOR_CORNER_ARC = 8.0;
    public static final double WIRE_INDICATOR_PADDING = 10.0;

    public static final Color COLOR_BACKGROUND = Color.rgb(20, 20, 30);
    public static final Color COLOR_SYSTEM_RECT = Color.rgb(70, 70, 90);
    public static final Color COLOR_SYSTEM_DIVIDER = Color.rgb(50, 50, 65);
    public static final Color COLOR_SYSTEM_INDICATOR_OFF = Color.rgb(255, 70, 70);
    public static final Color COLOR_SYSTEM_INDICATOR_ON = Color.rgb(70, 255, 70);
    public static final Color COLOR_PORT_SQUARE = Color.rgb(70, 130, 180);
    public static final Color COLOR_PORT_TRIANGLE = Color.rgb(144, 238, 144);
    public static final Color COLOR_PACKET_SQUARE = Color.rgb(135, 206, 250);
    public static final Color COLOR_PACKET_TRIANGLE = Color.rgb(240, 230, 140);
    public static final Color COLOR_WIRE_NORMAL = Color.rgb(200, 200, 200);
    public static final Color COLOR_WIRE_HIGHLIGHT = Color.WHITE;
    public static final Color COLOR_WIRE_TEMP_VALID = Color.LIMEGREEN;
    public static final Color COLOR_WIRE_TEMP_INVALID = Color.RED;
    public static final Color COLOR_HUD_TEXT = Color.WHITE;
    public static final Color COLOR_SHOP_BACKGROUND = Color.rgb(40, 40, 60, 0.9);
    public static final Color COLOR_SHOP_TEXT = Color.WHITE;
    public static final Color COLOR_SHOP_BUTTON = Color.rgb(80, 80, 120);
    public static final Color COLOR_SHOP_BUTTON_HOVER = Color.rgb(100, 100, 150);

    public static KeyCode KEY_TIME_FORWARD = KeyCode.RIGHT;
    public static KeyCode KEY_TIME_BACKWARD = KeyCode.LEFT;
    public static KeyCode KEY_SHOP = KeyCode.B;
    public static KeyCode KEY_EXECUTE_RUN = KeyCode.SPACE;
    public static KeyCode KEY_TOGGLE_HUD = KeyCode.H;
    public static KeyCode KEY_PAUSE = KeyCode.P;
    public static KeyCode KEY_EXIT_TO_MENU = KeyCode.ESCAPE;

    public static final String SOUND_CONNECT_SUCCESS = "/sounds/connect.wav";
    public static final String SOUND_PACKET_DAMAGE = "/sounds/damage.wav";
    public static final String SOUND_BACKGROUND_MUSIC = "/sounds/background.mp3";
    public static final String SOUND_SHOP_BUY = "/sounds/buy.wav";
    public static final String SOUND_LEVEL_COMPLETE = "/sounds/complete.wav";
    public static final String SOUND_GAME_OVER = "/sounds/gameover.wav";

    private Config() {
    }
}