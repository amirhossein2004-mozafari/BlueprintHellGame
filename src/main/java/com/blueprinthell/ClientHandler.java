package com.blueprinthell;

import com.api.dto.PacketDTO;
import com.api.dto.SystemDTO;
import com.api.dto.WireDTO;
import com.model.*;
import com.utils.LevelLoader;
import com.utils.SerializationManager;
import com.controller.GameController;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import javafx.geometry.Point2D;
import org.json.JSONObject;
import com.api.enums.AbilityType;
import com.blueprinthell.Config;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Map<UUID, GameState> activeGames;
    private UUID gameId;
    private GameController gameController;

    public ClientHandler(Socket socket, Map<UUID, GameState> activeGames) {
        this.clientSocket = socket;
        this.activeGames = activeGames;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {

            GameState gameState = LevelLoader.loadLevel(1);
            if (gameState == null) {
                out.println("ERROR: Could not load level on server.");
                return;
            }
            this.gameController = new GameController(gameState, null, null, true);
            gameState.setGameRunning(true);

            this.gameId = UUID.randomUUID();
            activeGames.put(this.gameId, gameState);

            out.println("GAME_ID:" + this.gameId.toString());
            System.out.println("Server: Started game " + this.gameId + " for client " + clientSocket.getInetAddress());


            Thread gameLoopThread = new Thread(() -> {
                try {
                    long lastTime = System.nanoTime();
                    double nsPerUpdate = 1000000000.0 / 60.0;
                    double delta = 0;

                    while (!clientSocket.isClosed()) {
                        long now = System.nanoTime();
                        delta += (now - lastTime) / nsPerUpdate;
                        lastTime = now;

                        while(delta >= 1){
                            this.gameController.updateGame(1.0 / 60.0);
                            delta--;
                        }

                        List<SystemDTO> systemDTOs = gameState.getSystems().stream()
                                .map(SerializationManager::toDTO).collect(Collectors.toList());

                        List<PacketDTO> packetDTOs = gameState.getPackets().stream()
                                .map(SerializationManager::toDTO).collect(Collectors.toList());

                        List<WireDTO> wireDTOs = gameState.getConnections().stream()
                                .map(SerializationManager::toDTO).collect(Collectors.toList());

                        out.println("UPDATE_SYSTEMS:" + SerializationManager.toJson(systemDTOs));
                        out.println("UPDATE_PACKETS:" + SerializationManager.toJsonPackets(packetDTOs));
                        out.println("UPDATE_WIRES:" + SerializationManager.toJsonWires(wireDTOs));

                        Thread.sleep(16);
                    }
                } catch (InterruptedException e) {
                    System.out.println("Game loop for client " + clientSocket.getInetAddress() + " interrupted.");
                }
            });

            gameLoopThread.start();

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("Server: Received from " + this.gameId.toString().substring(0,4) + ": " + clientMessage);

                try {
                    JSONObject command = new JSONObject(clientMessage);
                    String action = command.getString("action");
                    JSONObject payload = command.getJSONObject("payload");

                    switch (action) {
                        case "CREATE_WIRE":
                            handleCreateWire(gameState, payload);
                            break;

                        case "BUY_SKILL":
                            handleBuySkill(gameState, payload);
                            break;

                    }
                } catch (Exception e) {
                    System.err.println("Server: Error processing command from client: " + clientMessage);
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.err.println("Client handler error for " + clientSocket.getInetAddress() + ": " + e.getMessage());
        } finally {
            System.out.println("Client " + clientSocket.getInetAddress() + " disconnected.");
            if (this.gameId != null) {
                activeGames.remove(this.gameId);
                System.out.println("Server: Removed game " + this.gameId);
            }
        }
    }

    private void handleCreateWire(GameState gameState, JSONObject payload) {
        UUID startPortId = UUID.fromString(payload.getString("startPortId"));
        UUID endPortId = UUID.fromString(payload.getString("endPortId"));

        Port startPort = gameState.findPortById(startPortId);
        Port endPort = gameState.findPortById(endPortId);

        if (startPort != null && endPort != null) {
            System.out.println("Server: Processing CREATE_WIRE between " + startPort.getId().toString().substring(0,4) +
                    " and " + endPort.getId().toString().substring(0,4));

            double requiredLength = new Point2D(startPort.getAbsoluteX(), startPort.getAbsoluteY())
                    .distance(endPort.getAbsoluteX(), endPort.getAbsoluteY());

            if (gameState.useWire(requiredLength)) {
                Connection newConnection = new Connection(startPort, endPort);
                gameState.addConnection(newConnection);
                System.out.println("Server: Connection created successfully.");
            } else {
                System.err.println("Server: Not enough wire to create connection.");
            }
        } else {
            System.err.println("Server: Could not find one or both ports for CREATE_WIRE. Start: " + startPortId + ", End: " + endPortId);
        }
    }

    public static com.api.dto.PacketDTO toDTO(Packet packet) {
        if (packet == null) return null;
        PacketDTO dto = new PacketDTO();
        dto.setId(packet.getId());
        dto.setPacketType(mapGamePacketTypeToApiPacketType(packet.getType()));
        dto.setPosition(new Point2D(packet.getX(), packet.getY()));
        return dto;
    }

    public static com.api.enums.PacketType mapGamePacketTypeToApiPacketType(com.model.PacketType modelType) {
        try {
            return com.api.enums.PacketType.valueOf(modelType.name());
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: No matching API PacketType for " + modelType.name());
            return com.api.enums.PacketType.SQUARE;
        }
    }


    private void handleBuySkill(GameState gameState, JSONObject payload) {
        try {
            String abilityName = payload.getString("abilityType");
            AbilityType abilityType = AbilityType.valueOf(abilityName);

            int cost = 0;
            double duration = 0;

            switch (abilityType) {
                case ATAR:
                    cost = Config.ATAR_SKILL_COST;
                    duration = Config.ATAR_SKILL_DURATION_SECONDS;
                    break;
                case AIRYAMAN:
                    cost = Config.AIRYAMAN_SKILL_COST;
                    duration = Config.AIRYAMAN_SKILL_DURATION_SECONDS;
                    break;
                case ANAHITA:
                    cost = Config.ANAHITA_SKILL_COST;
                    duration = 0;
                    break;
            }

            if (cost == 0) {
                System.err.println("Server: Player tried to buy an unsupported skill: " + abilityName);
                return;
            }

            if (gameState.getCoins() >= cost) {
                gameState.decreaseCoins(cost);
                gameState.activateSkill(abilityType, duration);

                System.out.println("Server: Player " + gameId.toString().substring(0,4) +
                        " successfully bought skill: " + abilityName);


            } else {
                System.out.println("Server: Player " + gameId.toString().substring(0,4) +
                        " failed to buy skill " + abilityName + ". Not enough coins.");
            }

        } catch (Exception e) {
            System.err.println("Server: Error processing BUY_SKILL action.");
            e.printStackTrace();
        }
    }



}