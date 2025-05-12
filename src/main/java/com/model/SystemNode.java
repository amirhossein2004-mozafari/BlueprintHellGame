package com.model;

import com.blueprinthell.Config;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.Collections;
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

    public SystemNode(double x, double y, List<PacketType> inputPortTypes, List<PacketType> outputPortTypes) {
        this.x = x;
        this.y = y;
        this.width = Config.SYSTEM_WIDTH;
        this.height = Config.SYSTEM_HEIGHT;
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        this.storedPackets = new ArrayList<>();
        createSpecificPorts(inputPortTypes, outputPortTypes);
    }

    public SystemNode(double x, double y) {
        this(x, y, List.of(PacketType.SQUARE, PacketType.TRIANGLE), List.of(PacketType.SQUARE, PacketType.TRIANGLE)); // مثال پورت‌های پیش‌فرض
    }

    private void createSpecificPorts(List<PacketType> inputTypes, List<PacketType> outputTypes) {
        if (inputTypes != null && !inputTypes.isEmpty()) {
            double inputSpacing = height / (inputTypes.size() + 1.0);
            for (int i = 0; i < inputTypes.size(); i++) {
                double yPos = (i + 1) * inputSpacing;
                inputPorts.add(new Port(this, PortType.INPUT, inputTypes.get(i), new Point2D(Config.PORT_OFFSET_X, yPos)));
            }
        }
        if (outputTypes != null && !outputTypes.isEmpty()) {
            double outputSpacing = height / (outputTypes.size() + 1.0);
            double outputX = width - Config.PORT_OFFSET_X;
            for (int i = 0; i < outputTypes.size(); i++) {
                double yPos = (i + 1) * outputSpacing;
                outputPorts.add(new Port(this, PortType.OUTPUT, outputTypes.get(i), new Point2D(outputX, yPos)));
            }
        }
    }

    public void receivePacket(Packet packet, GameState gameState) {
        if (packet == null || gameState == null) return;
        packet.setCurrentConnection(null); packet.setTargetPort(null);
        packet.setProgressOnConnection(0.0); packet.getVelocity().set(0, 0); packet.setAcceleration(0);

        if (storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY) {
            storedPackets.add(packet);
            gameState.addCoin(packet.getType() == PacketType.SQUARE ? 1 : 2);
        } else {
            gameState.incrementPacketLoss(1);
            System.out.println("Packet discarded, storage full: " + packet.getType());
        }
        this.setIndicatorOn(!storedPackets.isEmpty());
    }

    public void update(double deltaTime, GameState gameState) {
        this.setIndicatorOn(!storedPackets.isEmpty());
        if (!storedPackets.isEmpty()) {
            Packet packetToSend = storedPackets.get(0);
            Connection connToSendOn = findAvailableConnectionFromSystem(this, gameState);
            if (connToSendOn != null && !isConnectionOccupied(connToSendOn, gameState)) {
                storedPackets.remove(0);
                packetToSend.startMoving(connToSendOn);
                gameState.addPacket(packetToSend);
                this.setIndicatorOn(!storedPackets.isEmpty());
                System.out.println("System " + this.hashCode() % 1000 + " sent packet: " + packetToSend.getType());
                return;
            }
        }
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

    public boolean isStorageFull() {
        return storedPackets.size() >= Config.SYSTEM_PACKET_STORAGE_CAPACITY;
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