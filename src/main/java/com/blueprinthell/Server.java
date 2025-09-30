
package com.blueprinthell;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.model.GameState;
import com.blueprinthell.db.DatabaseManager;

public class Server {
    public static final int SERVER_PORT = 8080;

    private static Socket waitingPlayer = null;

    public static void main(String[] args) {
        DatabaseManager.initialize();

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Multiplayer Server is running and waiting for clients to pair up...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                handleClientConnection(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            DatabaseManager.shutdown();
        }
    }

    private static synchronized void handleClientConnection(Socket clientSocket) {
        if (waitingPlayer == null) {
            waitingPlayer = clientSocket;
            System.out.println(clientSocket.getInetAddress() + " is now waiting for an opponent.");
        } else {
            System.out.println(clientSocket.getInetAddress() + " has been paired with " + waitingPlayer.getInetAddress());

            GameSession newSession = new GameSession(waitingPlayer, clientSocket);
            Thread sessionThread = new Thread(newSession);
            sessionThread.start();

            waitingPlayer = null;
        }
    }
}
