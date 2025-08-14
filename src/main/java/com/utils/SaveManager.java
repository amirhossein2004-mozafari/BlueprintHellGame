package com.utils;

import com.model.*;
import javafx.geometry.Point2D;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;


public class SaveManager {

    private static final String SAVE_FILE_PATH = "gameState.json";


    public static JSONObject gameStateToJson(GameState gameState) {
        JSONObject saveJson = new JSONObject();


        saveJson.put("levelNumber", gameState.getCurrentLevelNumber());
        saveJson.put("gameTimeElapsed", gameState.getGameTimeElapsedSeconds());


        saveJson.put("coins", gameState.getCoins());
        saveJson.put("remainingWireLength", gameState.getRemainingWireLength());

        JSONArray systemsArray = new JSONArray();
        for (int i = 0; i < gameState.getSystems().size(); i++) {
            SystemNode system = gameState.getSystems().get(i);
            JSONObject systemJson = new JSONObject();
            systemJson.put("id", i);
            systemJson.put("role", system.getRole().toString());
            systemJson.put("x", system.getX());
            systemJson.put("y", system.getY());
            systemsArray.put(systemJson);
        }
        saveJson.put("systems", systemsArray);


        JSONArray connectionsArray = new JSONArray();
        for (Connection conn : gameState.getConnections()) {
            JSONObject connJson = new JSONObject();

            connJson.put("startSystemIndex", gameState.getSystems().indexOf(conn.getStartPort().getParentSystem()));
            connJson.put("startPortIndex", conn.getStartPort().getParentSystem().getOutputPorts().indexOf(conn.getStartPort()));
            connJson.put("endSystemIndex", gameState.getSystems().indexOf(conn.getEndPort().getParentSystem()));
            connJson.put("endPortIndex", conn.getEndPort().getParentSystem().getInputPorts().indexOf(conn.getEndPort()));

            JSONArray bendPointsArray = new JSONArray();
            for (Point2D point : conn.getBendPoints()) {
                JSONObject pointJson = new JSONObject();
                pointJson.put("x", point.getX());
                pointJson.put("y", point.getY());
                bendPointsArray.put(pointJson);
            }
            connJson.put("bendPoints", bendPointsArray);
            connectionsArray.put(connJson);
        }
        saveJson.put("connections", connectionsArray);

        JSONArray packetsArray = new JSONArray();
        for (Packet packet : gameState.getPackets()) {
            JSONObject packetJson = new JSONObject();
            packetJson.put("type", packet.getType().toString());
            packetJson.put("x", packet.getX());
            packetJson.put("y", packet.getY());
            packetJson.put("velocityX", packet.getVelocity().getX());
            packetJson.put("velocityY", packet.getVelocity().getY());
            packetJson.put("currentSize", packet.getCurrentSize());
            packetJson.put("noiseLevel", packet.getNoiseLevel());
            packetsArray.put(packetJson);
        }
        saveJson.put("packets", packetsArray);

        return saveJson;
    }


    public static void saveGame(GameState gameState) {
        if (gameState == null) {
            System.err.println("SaveManager: GameState is null, cannot save.");
            return;
        }

        JSONObject saveJson = gameStateToJson(gameState);

        try (FileWriter file = new FileWriter(SAVE_FILE_PATH)) {
            file.write(saveJson.toString(4));
            System.out.println("Game state saved successfully to " + SAVE_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Error saving game state to file: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static GameState loadGame() {
        try {
            if (!Files.exists(Paths.get(SAVE_FILE_PATH))) {
                System.out.println("SaveManager: No save file found.");
                return null;
            }

            String jsonContent = new String(Files.readAllBytes(Paths.get(SAVE_FILE_PATH)));
            JSONObject saveJson = new JSONObject(jsonContent);

            return jsonToGameState(saveJson);

        } catch (IOException e) {
            System.err.println("Error reading save file: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing save file JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static GameState jsonToGameState(JSONObject saveJson) {

        int levelNumber = saveJson.getInt("levelNumber");
        GameState gameState = new GameState(levelNumber, 0, 0);


        gameState.setGameTimeElapsed(saveJson.getDouble("gameTimeElapsed"));
        gameState.coinsProperty().set(saveJson.getInt("coins"));
        gameState.remainingWireLengthProperty().set(saveJson.getDouble("remainingWireLength"));


        JSONArray systemsArray = saveJson.getJSONArray("systems");
        List<SystemNode> loadedSystems = new ArrayList<>();
        for (int i = 0; i < systemsArray.length(); i++) {
            JSONObject systemJson = systemsArray.getJSONObject(i);
            double x = systemJson.getDouble("x");
            double y = systemJson.getDouble("y");
            SystemRole role = SystemRole.valueOf(systemJson.getString("role"));

            SystemNode system = new SystemNode(x, y, role);
            loadedSystems.add(system);
            gameState.addSystem(system);
        }


        JSONArray connectionsArray = saveJson.getJSONArray("connections");
        for (int i = 0; i < connectionsArray.length(); i++) {
            JSONObject connJson = connectionsArray.getJSONObject(i);
            SystemNode startSystem = loadedSystems.get(connJson.getInt("startSystemIndex"));
            Port startPort = startSystem.getOutputPorts().get(connJson.getInt("startPortIndex"));

            SystemNode endSystem = loadedSystems.get(connJson.getInt("endSystemIndex"));
            Port endPort = endSystem.getInputPorts().get(connJson.getInt("endPortIndex"));

            Connection newConn = new Connection(startPort, endPort);

            JSONArray bendPointsArray = connJson.getJSONArray("bendPoints");
            for (int j = 0; j < bendPointsArray.length(); j++) {
                JSONObject pointJson = bendPointsArray.getJSONObject(j);
                newConn.addBendPoint(new Point2D(pointJson.getDouble("x"), pointJson.getDouble("y")));
            }
            gameState.addConnection(newConn);
        }


        JSONArray packetsArray = saveJson.getJSONArray("packets");
        for (int i = 0; i < packetsArray.length(); i++) {
            JSONObject packetJson = packetsArray.getJSONObject(i);
            PacketType type = PacketType.valueOf(packetJson.getString("type"));
            double x = packetJson.getDouble("x");
            double y = packetJson.getDouble("y");

            Packet newPacket = new Packet(type, x, y);
            newPacket.getVelocity().set(packetJson.getDouble("velocityX"), packetJson.getDouble("velocityY"));
            newPacket.setCurrentSize(packetJson.getInt("currentSize"));
            newPacket.setNoiseLevel(packetJson.getDouble("noiseLevel"));

            gameState.addPacket(newPacket);
        }

        System.out.println("Game state loaded successfully!");
        return gameState;
    }

}