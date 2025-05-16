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
    private final SystemRole role;
    private final List<Port> inputPorts;
    private final List<Port> outputPorts;
    private final List<Packet> storedPackets;
    private boolean indicatorOn = false;
    private static final Random random = new Random();


    public SystemNode(double x, double y, SystemRole role,
                      int numSquareInputs, int numTriangleInputs,
                      int numSquareOutputs, int numTriangleOutputs) {
        this.x = x;
        this.y = y;
        this.role = role;
        this.width = Config.SYSTEM_WIDTH;
        this.height = Config.SYSTEM_HEIGHT;
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        this.storedPackets = new ArrayList<>();

        createSpecificPorts(numSquareInputs, numTriangleInputs, numSquareOutputs, numTriangleOutputs);
        validatePortsBasedOnRole();
    }


    public SystemNode(double x, double y, SystemRole role) {
        this.x = x;
        this.y = y;
        this.role = role;
        this.width = Config.SYSTEM_WIDTH;
        this.height = Config.SYSTEM_HEIGHT;
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        this.storedPackets = new ArrayList<>();

        switch (role) {
            case SOURCE:
                createSpecificPorts(0, 0, 2, 1);
                break;
            case SINK:
                createSpecificPorts(1, 2, 0, 0);
                break;
            case INTERMEDIATE:
            default:
                createSpecificPorts(1, 1, 1, 1);
                break;
        }
    }

    private void createSpecificPorts(int numSquareIn, int numTriangleIn, int numSquareOut, int numTriangleOut) {
        List<PacketType> inputTypes = new ArrayList<>();
        for (int i = 0; i < numSquareIn; i++) inputTypes.add(PacketType.SQUARE);
        for (int i = 0; i < numTriangleIn; i++) inputTypes.add(PacketType.TRIANGLE);

        if (!inputTypes.isEmpty()) {
            double inputSpacing = height / (double) (inputTypes.size() + 1);
            for (int i = 0; i < inputTypes.size(); i++) {
                inputPorts.add(new Port(this, PortType.INPUT, inputTypes.get(i),
                        new Point2D(Config.PORT_OFFSET_X, (i + 1) * inputSpacing)));
            }
        }

        List<PacketType> outputTypes = new ArrayList<>();
        for (int i = 0; i < numSquareOut; i++) outputTypes.add(PacketType.SQUARE);
        for (int i = 0; i < numTriangleOut; i++) outputTypes.add(PacketType.TRIANGLE);

        if (!outputTypes.isEmpty()) {
            double outputSpacing = height / (double) (outputTypes.size() + 1);
            double outputX = width - Config.PORT_OFFSET_X;
            for (int i = 0; i < outputTypes.size(); i++) {
                outputPorts.add(new Port(this, PortType.OUTPUT, outputTypes.get(i),
                        new Point2D(outputX, (i + 1) * outputSpacing)));
            }
        }
    }

    private void validatePortsBasedOnRole() {
        if (this.role == SystemRole.SOURCE && !inputPorts.isEmpty()) {
            System.err.println("Warning: Source system ID " + (hashCode() % 1000) + " was created with input ports. Clearing them.");
            inputPorts.clear();
        }
        if (this.role == SystemRole.SINK && !outputPorts.isEmpty()) {
            System.err.println("Warning: Sink system ID " + (hashCode() % 1000) + " was created with output ports. Clearing them.");
            outputPorts.clear();
        }
    }


    public void receivePacket(Packet packet, GameState gameState) {
        if (packet == null || gameState == null) return;

        packet.setCurrentConnection(null);
        packet.setTargetPort(null);
        packet.setStartPort(null);
        packet.setProgressOnConnection(0.0);
        packet.getVelocity().set(0, 0);
        packet.setAcceleration(0);

        if (this.role == SystemRole.SINK || storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY) {
            if(this.role != SystemRole.SINK) {
                storedPackets.add(packet);
            }
            gameState.addCoin(packet.getType() == PacketType.SQUARE ? 1 : 2);
            System.out.println("Packet " + packet.getType() + " received by system " + (hashCode() % 1000) + " (Role: " + role + ")");
            if (this.role == SystemRole.SINK) {
            }
        } else {
            gameState.incrementPacketLoss(1);
            System.out.println("Packet " + packet.getType() + " discarded by system " + (hashCode() % 1000) + " (INTERMEDIATE - storage full)");
        }
        updateIndicatorState();
    }

    public void update(double deltaTime, GameState gameState) {
        updateIndicatorState();
        if (role == SystemRole.SINK || role == SystemRole.SOURCE || storedPackets.isEmpty()) {
            return;
        }

        Packet packetToSend = storedPackets.get(0);

        Connection connToSendOn = findAnyAvailableConnectionFromSystem(this, gameState);

        if (connToSendOn != null) {
            storedPackets.remove(0);
            packetToSend.startMoving(connToSendOn);
            gameState.addPacket(packetToSend);
            System.out.println("System " + (this.hashCode() % 1000) + " sent packet: " + packetToSend.getType() +
                    " via its port type: " + connToSendOn.getStartPort().getPacketType());
            updateIndicatorState();
        }
    }


    private Connection findAnyAvailableConnectionFromSystem(SystemNode system, GameState state) {
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


    private Connection findAvailableConnectionFromSystem(SystemNode system, GameState state, PacketType packetTypeToMatch) {
        List<Connection> suitableConnections = new ArrayList<>();
        for (Port port : system.getOutputPorts()) {
            if (port.getPacketType() == packetTypeToMatch) {
                Connection conn = findConnectionFromPort(port, state);
                if (conn != null && !isConnectionOccupied(conn, state)) {
                    suitableConnections.add(conn);
                }
            }
        }
        if (!suitableConnections.isEmpty()) {
            return suitableConnections.get(random.nextInt(suitableConnections.size()));
        }
        return null;
    }


    private Connection findConnectionFromPort(Port port, GameState gameState) {
        if (port == null || gameState == null) return null;
        for (Connection conn : gameState.getConnections()) {
            if (conn.getStartPort() == port) {
                return conn;
            }
        }
        return null;
    }

    private boolean isConnectionOccupied(Connection connection, GameState gameState) {
        if (connection == null || gameState == null) return true;
        for (Packet packet : gameState.getPackets()) {
            if (packet.getCurrentConnection() == connection && packet.getProgressOnConnection() > 0.0001) {
                return true;
            }
        }
        return false;
    }

    private void updateIndicatorState() {
        this.indicatorOn = !storedPackets.isEmpty();
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public SystemRole getRole() { return role; }
    public List<Port> getInputPorts() { return Collections.unmodifiableList(inputPorts); }
    public List<Port> getOutputPorts() { return Collections.unmodifiableList(outputPorts); }
    public boolean isIndicatorOn() { return indicatorOn; }
    public int getStoredPacketCount() { return storedPackets.size(); }
    public boolean isStorageFull() { return storedPackets.size() >= Config.SYSTEM_PACKET_STORAGE_CAPACITY; }

    public void setIndicatorOn(boolean indicatorOn) { this.indicatorOn = indicatorOn; }
}