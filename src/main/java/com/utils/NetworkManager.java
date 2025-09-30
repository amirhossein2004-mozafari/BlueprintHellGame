package com.utils;

import com.blueprinthell.Server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import org.json.JSONObject;

public class NetworkManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static NetworkManager instance;

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public boolean connect() {
        try {
            socket = new Socket("localhost", Server.SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Client: Successfully connected to the server!");
            return true;
        } catch (IOException e) {
            System.err.println("Client: Connection failed. Playing in offline mode.");
            return false;
        }
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public String readMessage() {
        try {
            if (in != null) {
                return in.readLine();
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAction(String action, JSONObject payload) {
        if (out != null) {
            JSONObject command = new JSONObject();
            command.put("action", action);
            command.put("payload", payload);
            sendMessage(command.toString());
        }
    }

}