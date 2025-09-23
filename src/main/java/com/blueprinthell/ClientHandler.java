package com.blueprinthell;

import com.api.dto.StatusDTO;
import com.api.dto.SystemDTO;
import com.api.service.GameService;
import com.model.SystemNode;
import com.utils.SerializationManager;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final GameService gameService;
    private final Map<Socket, UUID> clientGameMap;

    public ClientHandler(Socket socket, GameService service, Map<Socket, UUID> clientGameMap) {
        this.clientSocket = socket;
        this.gameService = service;
        this.clientGameMap = clientGameMap;
    }

    @Override
    public void run() {
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            StatusDTO initResponse = gameService.initializeGame();
            if (initResponse.getStatusCode() != 200) {
                out.println("ERROR: Could not initialize game.");
                return;
            }
            UUID gameId = (UUID) initResponse.getMessage();
            clientGameMap.put(clientSocket, gameId);

            StatusDTO systemsResponse = gameService.getAllSystems(gameId);
            if (systemsResponse.getStatusCode() != 200) {
                out.println("ERROR: Could not retrieve game systems.");
                return;
            }

            @SuppressWarnings("unchecked")
            List<SystemDTO> systemDTOs = (List<SystemDTO>) systemsResponse.getMessage();

            String jsonResponse = SerializationManager.toJson(systemDTOs);
            out.println(jsonResponse);
            System.out.println("Server sent initial game state to client " + clientSocket.getInetAddress());


        } catch (Exception e) {
            System.err.println("An error occurred with client " + clientSocket.getInetAddress());
            e.printStackTrace();
        } finally {
            clientGameMap.remove(clientSocket);
        }
    }
}