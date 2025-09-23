package com.blueprinthell;

import com.api.service.GameService;
import com.model.GameState;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    public static final int SERVER_PORT = 8080;

    private static final Map<UUID, GameState> activeGames = new ConcurrentHashMap<>();
    private static final Map<Socket, UUID> clientGameMap = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        GameService gameService = new GameService(activeGames);

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server is running...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                Thread clientThread = new Thread(new ClientHandler(clientSocket, gameService, clientGameMap));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}