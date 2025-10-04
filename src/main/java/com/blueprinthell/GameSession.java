package com.blueprinthell;

import com.api.dto.PacketDTO;
import com.api.dto.SystemDTO;
import com.api.dto.WireDTO;
import com.blueprinthell.db.entities.GameResultEntity;
import com.controller.GameController;
import com.model.*;
import com.utils.LevelLoader;
import com.utils.SerializationManager;
import javafx.geometry.Point2D;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.blueprinthell.db.DatabaseManager;
import com.api.dto.LeaderboardEntryDTO;

public class GameSession implements Runnable {
    private final Socket p1Socket;
    private final Socket p2Socket;
    private PrintWriter p1Out;
    private PrintWriter p2Out;

    private GameState p1GameState;
    private GameState p2GameState;
    private GameController p1GameController;
    private GameController p2GameController;
    private static final String HASH_SECRET_KEY = "Blueprint_Hell_Is_Not_A_Real_Place_Or_Is_It?";

    public GameSession(Socket p1Socket, Socket p2Socket) {
        this.p1Socket = p1Socket;
        this.p2Socket = p2Socket;
    }

    @Override
    public void run() {
        try {
            p1Out = new PrintWriter(p1Socket.getOutputStream(), true);
            BufferedReader p1In = new BufferedReader(new InputStreamReader(p1Socket.getInputStream()));
            p2Out = new PrintWriter(p2Socket.getOutputStream(), true);
            BufferedReader p2In = new BufferedReader(new InputStreamReader(p2Socket.getInputStream()));
            p1Out.println("PLAYER_ID:1");
            p2Out.println("PLAYER_ID:2");
            p1GameState = LevelLoader.loadLevel(1);
            p2GameState = LevelLoader.loadLevel(1);
            p1GameController = new GameController(p1GameState, null, null);
            p2GameController = new GameController(p2GameState, null, null);
            p1GameState.setGameRunning(true);
            p2GameState.setGameRunning(true);

            System.out.println("Game Session Started for " + p1Socket.getInetAddress() + " and " + p2Socket.getInetAddress());

            new Thread(() -> playerListener(p1In, p1Out, "Player 1")).start();
            new Thread(() -> playerListener(p2In, p2Out, "Player 2")).start();

            long lastTime = System.nanoTime();
            double nsPerUpdate = 1000000000.0 / 60.0;
            double delta = 0;

            while (!p1Socket.isClosed() && !p2Socket.isClosed()) {
                long now = System.nanoTime();
                delta += (now - lastTime) / nsPerUpdate;
                lastTime = now;

                while(delta >= 1){
                    p1GameController.updateGame(1.0 / 60.0, 1);
                    p2GameController.updateGame(1.0 / 60.0, 2);
                    delta--;
                }

                broadcastStateToPlayer(p1Out, p1GameState, p2GameState);
                broadcastStateToPlayer(p2Out, p2GameState, p1GameState);

                Thread.sleep(16);
            }

        } catch (Exception e) {
            System.err.println("Game Session crashed: " + e.getMessage());
        } finally {
            System.out.println("Game Session finished. Saving results...");

            String p1Username = p1Socket.getInetAddress().toString();
            String p2Username = p2Socket.getInetAddress().toString();

            DatabaseManager.saveGameResult(p1Username, p1GameState.getCoins(), p1GameState.getPacketLossPercentageBySize());
            DatabaseManager.saveGameResult(p2Username, p2GameState.getCoins(), p2GameState.getPacketLossPercentageBySize());

            try { p1Socket.close(); } catch (Exception e) {}
            try { p2Socket.close(); } catch (Exception e) {}
        }
    }

    private void broadcastStateToPlayer(PrintWriter out, GameState myState, GameState opponentState) {
        List<SystemDTO> mySystemDTOs = myState.getSystems().stream().map(SerializationManager::toDTO).collect(Collectors.toList());
        List<PacketDTO> myPacketDTOs = myState.getPackets().stream().map(SerializationManager::toDTO).collect(Collectors.toList());
        List<WireDTO> myWireDTOs = myState.getConnections().stream().map(SerializationManager::toDTO).collect(Collectors.toList());

        List<SystemDTO> opponentSystemDTOs = opponentState.getSystems().stream().map(SerializationManager::toDTO).collect(Collectors.toList());

        out.println("UPDATE_SYSTEMS:" + SerializationManager.toJson(mySystemDTOs));
        out.println("UPDATE_PACKETS:" + SerializationManager.toJsonPackets(myPacketDTOs));
        out.println("UPDATE_WIRES:" + SerializationManager.toJsonWires(myWireDTOs));
        out.println("UPDATE_OPPONENT_SYSTEMS:" + SerializationManager.toJson(opponentSystemDTOs));
    }


    private void playerListener(BufferedReader in, PrintWriter out, String playerName) {
        String clientMessage;
        GameState myGameState = playerName.equals("Player 1") ? p1GameState : p2GameState;
        GameState opponentGameState = playerName.equals("Player 1") ? p2GameState : p1GameState;

        try {
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("Session Received from " + playerName + ": " + clientMessage);

                JSONObject command = new JSONObject(clientMessage);
                String action = command.getString("action");
                JSONObject payload = command.getJSONObject("payload");

                if ("CREATE_WIRE".equals(action)) {
                    handleCreateWire(myGameState, payload);
                }
                else if ("BUY_SKILL".equals(action)) {
                    handleBuySkill(myGameState, payload);

                }
                else if ("USE_CONTROLLABLE_SYSTEM".equals(action)) {
                    handleUseControllableSystem(myGameState, opponentGameState, payload);
                }
                else if ("ADD_BEND_POINT".equals(action)) {
                    handleAddBendPoint(myGameState, payload);
                }

                else if ("GET_LEADERBOARD".equals(action)) {
                    handleGetLeaderboard(out);
                }

                else if ("SUBMIT_OFFLINE_RESULT".equals(action)) {
                    handleSubmitOfflineResult(payload);
                }

            }
        } catch (Exception e) {
            System.out.println("Connection lost for " + playerName);
        }
    }


    private void handleCreateWire(GameState gameState, JSONObject payload) {
        Port startPort = gameState.findPortById(UUID.fromString(payload.getString("startPortId")));
        Port endPort = gameState.findPortById(UUID.fromString(payload.getString("endPortId")));

        if (startPort != null && endPort != null) {
            double requiredLength = new Point2D(startPort.getAbsoluteX(), startPort.getAbsoluteY())
                    .distance(endPort.getAbsoluteX(), endPort.getAbsoluteY());

            if (gameState.useWire(requiredLength)) {
                Connection newConnection = new Connection(startPort, endPort);
                gameState.addConnection(newConnection);
            }
        }
    }

    private void handleBuySkill(GameState gameState, JSONObject payload) {
        try {
            String abilityName = payload.getString("abilityType");
            com.api.enums.AbilityType abilityType = com.api.enums.AbilityType.valueOf(abilityName);
            int cost = 0; double duration = 0;
            switch (abilityType) {
                case ATAR: cost = Config.ATAR_SKILL_COST; duration = Config.ATAR_SKILL_DURATION_SECONDS; break;
                case AIRYAMAN: cost = Config.AIRYAMAN_SKILL_COST; duration = Config.AIRYAMAN_SKILL_DURATION_SECONDS; break;
                case ANAHITA: cost = Config.ANAHITA_SKILL_COST; duration = 0; break;
            }
            if (cost > 0 && gameState.getCoins() >= cost) {
                gameState.decreaseCoins(cost);
                gameState.activateSkill(abilityType, duration);
                System.out.println("Server: Skill " + abilityName + " bought successfully for player.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleUseControllableSystem(GameState myGameState, GameState opponentGameState, JSONObject payload) {
        UUID systemId = UUID.fromString(payload.getString("systemId"));
        SystemNode mySystem = myGameState.getSystems().stream()
                .filter(s -> s.getId().equals(systemId))
                .findFirst().orElse(null);

        int myPlayerId = (myGameState == p1GameState) ? 1 : 2;
        if (mySystem != null && mySystem.isControllable() && !mySystem.isOnCooldown()) {
            mySystem.startCooldown();

            List<SystemNode> opponentSources = opponentGameState.getSystemsByRole(SystemRole.SOURCE);
            if (!opponentSources.isEmpty()) {
                SystemNode opponentSourceSystem = opponentSources.get(new java.util.Random().nextInt(opponentSources.size()));

                Packet newPacket = new Packet(PacketType.SQUARE,
                        opponentSourceSystem.getX(), opponentSourceSystem.getY());
                newPacket.setPlayerId(myPlayerId);
                opponentGameState.addPacket(newPacket);

                System.out.println("Server: Packet injected into opponent's network successfully.");

            } else {
                System.out.println("Server: Opponent has no SOURCE systems to spawn a packet from.");
            }
        } else {
            System.out.println("Server: Invalid USE_CONTROLLABLE_SYSTEM request (already on cooldown or not controllable).");
        }
    }

    private void handleAddBendPoint(GameState gameState, JSONObject payload) {
        UUID connectionId = UUID.fromString(payload.getString("connectionId"));
        double x = payload.getDouble("x");
        double y = payload.getDouble("y");

        if (gameState.getCoins() >= 1) {
            Connection targetConnection = gameState.getConnections().stream()
                    .filter(c -> c.getId().equals(connectionId))
                    .findFirst().orElse(null);

            if (targetConnection != null) {
                gameState.decreaseCoins(1);
                targetConnection.addBendPoint(new Point2D(x, y));
                System.out.println("Server: Added bend point to connection " + connectionId.toString().substring(0, 4));
            }
        } else {
            System.out.println("Server: Player tried to add bend point but has insufficient coins.");
        }
    }

    private void handleGetLeaderboard(PrintWriter out) {
        List<GameResultEntity> topScores = DatabaseManager.getLeaderboardTopScores(10);
        List<LeaderboardEntryDTO> dtoList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (GameResultEntity entity : topScores) {
            dtoList.add(new LeaderboardEntryDTO(
                    entity.getPlayer().getUsername(),
                    entity.getFinalCoins(),
                    entity.getPacketLossPercentage(),
                    sdf.format(entity.getPlayDate())
            ));
        }

        String jsonPayload = SerializationManager.toJsonLeaderboard(dtoList);
        out.println("UPDATE_LEADERBOARD:" + jsonPayload);
        System.out.println("Server: Sent leaderboard data to a client.");
    }


    private void handleSubmitOfflineResult(JSONObject payload) {
        try {
            String username = payload.getString("username");
            int coins = payload.getInt("coins");
            int level = payload.getInt("levelNumber");
            double time = payload.getDouble("gameTimeElapsed");
            String clientHash = payload.getString("integrityHash");

            String dataToHash = coins + ":" + level + ":" + (int)time;
            String serverHash = generateHash(dataToHash);

            if (serverHash.equals(clientHash)) {
                System.out.println("Server: Offline result for " + username + " is VALID. Saving to DB.");
                DatabaseManager.saveGameResult(username, coins, 0.0);
            } else {
                System.err.println("Server: OFFLINE RESULT REJECTED. HASH MISMATCH.");
                System.err.println(" > Client sent: " + clientHash);
                System.err.println(" > Server calculated: " + serverHash);
            }
        } catch (Exception e) {
            System.err.println("Server: Error processing offline result.");
            e.printStackTrace();
        }
    }

    private String generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest((data + HASH_SECRET_KEY).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) { e.printStackTrace(); return ""; }
    }

}