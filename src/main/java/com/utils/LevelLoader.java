package com.utils;

import com.blueprinthell.Config;
import com.model.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.io.IOException;

public class LevelLoader {


    public static GameState loadLevel(int levelNumberToLoad) {
        String fileName = "/levels/level" + levelNumberToLoad + ".json";
        System.out.println("LevelLoader: Attempting to load level file: " + fileName);

        try (InputStream inputStream = LevelLoader.class.getResourceAsStream(fileName)) {

            if (inputStream == null) {
                System.err.println("LevelLoader Error: Could not find level file: " + fileName);
                return null;
            }

            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
                String jsonContent = scanner.useDelimiter("\\A").next();
                JSONObject levelJson = new JSONObject(jsonContent);

                int packetsToSpawn = levelJson.optInt("packetsToSpawn", 50);
                double levelDuration = levelJson.optDouble("levelDurationSeconds", 180.0);
                GameState gameState = new GameState(levelNumberToLoad, packetsToSpawn, levelDuration);
                gameState.remainingWireLengthProperty().set(levelJson.optDouble("initialWireLength", Config.INITIAL_WIRE_LENGTH));

                JSONArray systemsArray = levelJson.getJSONArray("systems");
                for (int i = 0; i < systemsArray.length(); i++) {
                    JSONObject systemJson = systemsArray.getJSONObject(i);
                    double x = systemJson.getDouble("x");
                    double y = systemJson.getDouble("y");
                    SystemRole role = SystemRole.valueOf(systemJson.getString("role").toUpperCase());

                    JSONObject portsJson = systemJson.optJSONObject("ports", new JSONObject());
                    int sqIn = portsJson.optInt("squareInputs", 0);
                    int trIn = portsJson.optInt("triangleInputs", 0);
                    int lgIn = portsJson.optInt("largeInputs", 0);
                    int msgIn = portsJson.optInt("messengerInputs", 0);

                    int sqOut = portsJson.optInt("squareOutputs", 0);
                    int trOut = portsJson.optInt("triangleOutputs", 0);
                    int lgOut = portsJson.optInt("largeOutputs", 0);
                    int msgOut = portsJson.optInt("messengerOutputs", 0);

                    SystemNode systemNode = new SystemNode(x, y, role, sqIn, trIn, lgIn, msgIn, sqOut, trOut, lgOut, msgOut);
                    gameState.addSystem(systemNode);
                }
                return gameState;
            }

        } catch (Exception e) {
            System.err.println("LevelLoader Error: Processing level " + levelNumberToLoad + " failed.");
            e.printStackTrace();
            return null;
        }
    }
}