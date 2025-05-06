package com.model;

import com.blueprinthell.Config;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SystemNode {

    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final List<Port> inputPorts;
    private final List<Port> outputPorts;
    private final List<Packet> storedPackets;
    private boolean indicatorOn = false;
    private static final Random random = new Random();

    public SystemNode(double x, double y) {
        this.x = x;
        this.y = y;
        this.width = Config.SYSTEM_WIDTH;
        this.height = Config.SYSTEM_HEIGHT;
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        this.storedPackets = new ArrayList<>();
        createRandomPorts();
    }

    private void createRandomPorts() {
        int numInputs = random.nextInt(3) + 1;
        double inputSpacing = height / (numInputs + 1.0);
        for (int i = 1; i <= numInputs; i++) {
            PacketType randomType = random.nextBoolean() ? PacketType.SQUARE : PacketType.TRIANGLE;
            inputPorts.add(new Port(this, PortType.INPUT, randomType, new Point2D(Config.PORT_OFFSET_X, i * inputSpacing)));
        }
        int numOutputs = random.nextInt(3) + 1;
        double outputSpacing = height / (numOutputs + 1.0);
        double outputX = width - Config.PORT_OFFSET_X;
        for (int i = 1; i <= numOutputs; i++) {
            PacketType randomType = random.nextBoolean() ? PacketType.SQUARE : PacketType.TRIANGLE;
            outputPorts.add(new Port(this, PortType.OUTPUT, randomType, new Point2D(outputX, i * outputSpacing)));
        }
    }

    public void receivePacket(Packet packet, GameState gameState) {
        if (packet == null || gameState == null) return;
        packet.setCurrentConnection(null);
        packet.setTargetPort(null);
        packet.setProgressOnConnection(0.0);
        packet.getVelocity().set(0, 0);
        packet.setAcceleration(0);

        if (storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY) {
            storedPackets.add(packet);
            int coinsToAdd = (packet.getType() == PacketType.SQUARE) ? 1 : 2;
            gameState.addCoin(coinsToAdd);
            System.out.println("Packet received & stored: " + packet.getType() + ". Added " + coinsToAdd + " coins. Total: " + gameState.getCoins());
        } else {
            gameState.incrementPacketLoss(1);
            System.out.println("Packet discarded, storage full: " + packet.getType());
        }
        this.setIndicatorOn(!storedPackets.isEmpty());
    }

    public void requeuePacket(Packet packet) {
        if (packet != null && !storedPackets.contains(packet)) {
            if (storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY) {
                storedPackets.add(0, packet);
            }
        }
    }

    public void update(double deltaTime, GameState gameState) {
        this.setIndicatorOn(!storedPackets.isEmpty());

        if (!storedPackets.isEmpty()) {
            Packet packetToSend = storedPackets.get(0);

            Connection connToSendOn = findAvailableConnectionFromSystem(this, gameState);

            if (connToSendOn != null) {
                storedPackets.remove(0);
                packetToSend.startMoving(connToSendOn);
                gameState.addPacket(packetToSend);
                this.setIndicatorOn(!storedPackets.isEmpty());
                System.out.println("System " + this.hashCode() % 1000 + " sending packet: " + packetToSend.getType() +
                        " via port type: " + connToSendOn.getStartPort().getPacketType());
            }
        }
    }
    public boolean isStorageFull() {
        return storedPackets.size() >= Config.SYSTEM_PACKET_STORAGE_CAPACITY;
    }

    private Connection findAvailableConnectionFromSystem(SystemNode system, GameState state) {
        List<Connection> availableConnections = new ArrayList<>();
        for (Port port : system.getOutputPorts()) {
            Connection conn = findConnectionFromPort(port, state);
            if (conn != null && !isConnectionOccupied(conn, state)) {
                availableConnections.add(conn);
            }
        }
        if (!availableConnections.isEmpty()) {
            return availableConnections.get(random.nextInt(availableConnections.size()));
        }
        return null;
    }


    private Connection findConnectionFromPort(Port port, GameState gameState) {
        if (port == null || port.getType() != PortType.OUTPUT || gameState == null) return null;
        for (Connection conn : gameState.getConnections()) {
            if (conn.getStartPort() == port) {
                return conn;
            }
        }
        return null;
    }

    private boolean isConnectionOccupied(Connection connection, GameState gameState) {
        if (connection == null) return true;
        for (Packet packet : gameState.getPackets()) {
            if (packet.getCurrentConnection() == connection && packet.getProgressOnConnection() > 0.001) {
                return true;
            }
        }
        return false;
    }

    public int getStoredPacketCount() {
        return storedPackets.size();
    }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public List<Port> getInputPorts() { return Collections.unmodifiableList(inputPorts); }
    public List<Port> getOutputPorts() { return Collections.unmodifiableList(outputPorts); }
    public boolean isIndicatorOn() { return indicatorOn; }

    public void setIndicatorOn(boolean indicatorOn) { this.indicatorOn = indicatorOn; }
}