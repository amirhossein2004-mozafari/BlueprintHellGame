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

public class SaveManager {

    private static final String SAVE_FILE_PATH = "gameState.json";


    public static void saveGame(GameState gameState) {
        if (gameState == null || !gameState.isGameRunning()) {
            System.out.println("SaveManager: Save condition not met (game not running or state is null).");
            return;
        }
        JSONObject saveJson = gameStateToJson(gameState);
        try (FileWriter file = new FileWriter(SAVE_FILE_PATH)) {
            file.write(saveJson.toString(2));
            System.out.println("Game state saved successfully to " + SAVE_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Error saving game state to file: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static JSONObject gameStateToJson(GameState gameState) {
        JSONObject json = new JSONObject();
        json.put("levelNumber", gameState.getCurrentLevelNumber());
        json.put("gameTimeElapsed", gameState.getGameTimeElapsedSeconds());
        json.put("coins", gameState.getCoins());
        json.put("remainingWireLength", gameState.getRemainingWireLength());

        JSONArray systemsArray = new JSONArray();
        gameState.getSystems().forEach(system -> {
            JSONObject sysJson = new JSONObject();
            sysJson.put("role", system.getRole().toString());
            sysJson.put("x", system.getX());
            sysJson.put("y", system.getY());
            systemsArray.put(sysJson);
        });
        json.put("systems", systemsArray);

        JSONArray connsArray = new JSONArray();
        for (Connection conn : gameState.getConnections()) {
            JSONObject connJson = new JSONObject();
            connJson.put("startSysIdx", gameState.getSystems().indexOf(conn.getStartPort().getParentSystem()));
            connJson.put("startPortIdx", conn.getStartPort().getParentSystem().getOutputPorts().indexOf(conn.getStartPort()));
            connJson.put("endSysIdx", gameState.getSystems().indexOf(conn.getEndPort().getParentSystem()));
            connJson.put("endPortIdx", conn.getEndPort().getParentSystem().getInputPorts().indexOf(conn.getEndPort()));
            connJson.put("largePassages", conn.getLargePacketPassages());

            JSONArray bends = new JSONArray();
            conn.getBendPoints().forEach(p -> bends.put(new JSONObject().put("x", p.getX()).put("y", p.getY())));
            connJson.put("bendPoints", bends);
            connsArray.put(connJson);
        }
        json.put("connections", connsArray);

        JSONArray packetsArray = new JSONArray();
        gameState.getPackets().forEach(packet -> {
            JSONObject pJson = new JSONObject();
            pJson.put("type", packet.getType().toString());
            pJson.put("x", packet.getX());
            pJson.put("y", packet.getY());
            pJson.put("velX", packet.getVelocity().getX());
            pJson.put("velY", packet.getVelocity().getY());
            pJson.put("currentSize", packet.getCurrentSize());
            packetsArray.put(pJson);
        });
        json.put("packets", packetsArray);

        return json;
    }


    public static GameState loadGame() {
        if (!Files.exists(Paths.get(SAVE_FILE_PATH))) {
            return null;
        }
        try {
            String content = new String(Files.readAllBytes(Paths.get(SAVE_FILE_PATH)));
            if (content.trim().isEmpty()) return null;

            JSONObject json = new JSONObject(content);
            return jsonToGameState(json);
        } catch (Exception e) {
            System.err.println("Failed to load game from file.");
            e.printStackTrace();
            return null;
        }
    }

    private static GameState jsonToGameState(JSONObject json) {
        int levelNumber = json.getInt("levelNumber");
        GameState gameState = LevelLoader.loadLevel(levelNumber);

        gameState.clearConnections();
        gameState.clearPackets();

        gameState.setGameTimeElapsed(json.getDouble("gameTimeElapsed"));
        gameState.coinsProperty().set(json.getInt("coins"));
        gameState.remainingWireLengthProperty().set(json.getDouble("remainingWireLength"));

        List<SystemNode> systems = gameState.getSystems();
        JSONArray connsArray = json.getJSONArray("connections");
        for (int i = 0; i < connsArray.length(); i++) {
            JSONObject cJson = connsArray.getJSONObject(i);
            Port startPort = systems.get(cJson.getInt("startSysIdx")).getOutputPorts().get(cJson.getInt("startPortIdx"));
            Port endPort = systems.get(cJson.getInt("endSysIdx")).getInputPorts().get(cJson.getInt("endPortIdx"));
            Connection conn = new Connection(startPort, endPort);

            cJson.getJSONArray("bendPoints").forEach(p -> {
                JSONObject pJson = (JSONObject) p;
                conn.addBendPoint(new Point2D(pJson.getDouble("x"), pJson.getDouble("y")));
            });

            for(int k=0; k < cJson.optInt("largePassages", 0); k++) conn.registerLargePacketPassage();

            gameState.addConnection(conn);
        }

        JSONArray packetsArray = json.getJSONArray("packets");
        packetsArray.forEach(p -> {
            JSONObject pJson = (JSONObject) p;
            Packet packet = new Packet(PacketType.valueOf(pJson.getString("type")), pJson.getDouble("x"), pJson.getDouble("y"));
            packet.getVelocity().set(pJson.getDouble("velX"), pJson.getDouble("velY"));
            packet.setCurrentSize(pJson.getInt("currentSize"));
            gameState.addPacket(packet);
        });

        System.out.println("GameState successfully reconstructed from file.");
        return gameState;
    }


    public static void deleteSaveFile() {
        try {
            Files.deleteIfExists(Paths.get(SAVE_FILE_PATH));
            System.out.println("Save file successfully deleted.");
        } catch (IOException e) {
            System.err.println("Could not delete save file.");
            e.printStackTrace();
        }
    }
}