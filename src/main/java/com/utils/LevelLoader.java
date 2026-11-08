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
                System.err.println("LevelLoader CRITICAL Error: Could not find level file: " + fileName);
                return null;
            }

            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
                if (!scanner.hasNext()) {
                    System.err.println("LevelLoader Error: Level file " + fileName + " is empty.");
                    System.err.println("LevelLoader: Returning a default GameState for level " + levelNumberToLoad + " due to empty file.");
                    return new GameState(levelNumberToLoad, 30, 90.0);
                }
                String jsonContent = scanner.useDelimiter("\\A").next();
                JSONObject levelJson = new JSONObject(jsonContent);


                int jsonInternalLevelNum = levelJson.optInt("levelNumber", levelNumberToLoad);
                if (jsonInternalLevelNum != levelNumberToLoad) {
                    System.out.println("LevelLoader Warning: Requested level " + levelNumberToLoad +
                            " but file " + fileName + " contains internal level number " + jsonInternalLevelNum +
                            ". Using requested number: " + levelNumberToLoad);
                }

                int packetsToSpawn = levelJson.optInt("packetsToSpawn", 50);
                double levelDuration = levelJson.optDouble("levelDurationSeconds", 180.0);

                GameState gameState = new GameState(levelNumberToLoad, packetsToSpawn, levelDuration);

                if (levelJson.has("initialWireLength")) {
                    gameState.remainingWireLengthProperty().set(levelJson.getDouble("initialWireLength"));
                } else {
                    gameState.remainingWireLengthProperty().set(Config.INITIAL_WIRE_LENGTH);
                }

                JSONArray systemsArray = levelJson.getJSONArray("systems");
                for (int i = 0; i < systemsArray.length(); i++) {
                    JSONObject systemJson = systemsArray.getJSONObject(i);
                    double x = systemJson.getDouble("x");
                    double y = systemJson.getDouble("y");
                    SystemRole role = SystemRole.valueOf(systemJson.getString("role").toUpperCase());

                    JSONObject portsJson = systemJson.getJSONObject("ports");
                    int sqIn = portsJson.optInt("squareInputs", 0);
                    int trIn = portsJson.optInt("triangleInputs", 0);
                    int sqOut = portsJson.optInt("squareOutputs", 0);
                    int trOut = portsJson.optInt("triangleOutputs", 0);

                    SystemNode systemNode = new SystemNode(x, y, role, sqIn, trIn, sqOut, trOut);
                    gameState.addSystem(systemNode);
                }
                System.out.println("LevelLoader: Level " + levelNumberToLoad + " loaded successfully with " +
                        gameState.getSystems().size() + " systems. Packets to spawn: " + packetsToSpawn +
                        ", Duration: " + levelDuration + "s, Wire: " + gameState.getRemainingWireLength());
                return gameState;

            }

        } catch (IOException e) {
            System.err.println("LevelLoader Error: Failed to process " + fileName);
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("LevelLoader Error: Processing level " + levelNumberToLoad + " from file: " + fileName);
            e.printStackTrace();
            System.err.println("LevelLoader: Returning a default GameState for level " + levelNumberToLoad + " due to processing error.");
            return new GameState(levelNumberToLoad, 50, 180.0);
        }
    }
}