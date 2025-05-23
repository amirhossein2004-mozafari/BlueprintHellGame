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
    private boolean allPortsActuallyConnected = false;


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
        updateAllPortsActuallyConnectedState();
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

    public void updateAllPortsActuallyConnectedState() {
        boolean allInConnected = true;
        if (!inputPorts.isEmpty()) {
            for (Port port : inputPorts) {
                if (!port.isConnected()) {
                    allInConnected = false;
                    break;
                }
            }
        } else {
            allInConnected = true;
        }

        boolean allOutConnected = true;
        if (!outputPorts.isEmpty()) {
            for (Port port : outputPorts) {
                if (!port.isConnected()) {
                    allOutConnected = false;
                    break;
                }
            }
        } else {
            allOutConnected = true;
        }

        this.allPortsActuallyConnected = allInConnected && allOutConnected;
    }

    public boolean isAllPortsActuallyConnected() {
        return allPortsActuallyConnected;
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

        boolean canAcceptPacket = (this.role == SystemRole.SINK) || (storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY);

        if (canAcceptPacket) {
            int coinsToAdd = (packet.getType() == PacketType.SQUARE) ? Config.COINS_PER_SQUARE_PACKET_SYSTEM_ENTRY : Config.COINS_PER_TRIANGLE_PACKET_SYSTEM_ENTRY;
            gameState.addCoin(coinsToAdd);

            if (this.role != SystemRole.SINK) {
                packet.setInheritedStabilityDamage(1.0 - packet.getStabilityOnWire());
                packet.getVelocity().set(0,0);
                packet.setAcceleration(0);
                packet.setCurrentConnection(null);
                packet.setTargetPort(null);
                packet.setStartPort(null);
                packet.setProgressOnConnection(0);
                storedPackets.add(packet);
            } else {
                packet.setCurrentConnection(null);
                packet.setTargetPort(null);
                packet.setStartPort(null);
                packet.setProgressOnConnection(1.0);

            }
        } else {
            gameState.registerCompletelyLostPacket(packet);
        }
    }
    public void update(double deltaTime, GameState gameState) {
        if (gameState == null || role == SystemRole.SINK || role == SystemRole.SOURCE || storedPackets.isEmpty()) {
            return;
        }

        Packet packetToSend = null;
        Connection connToSendOn = null;
        int packetIndexInQueue = -1;


        for (int i = 0; i < storedPackets.size(); i++) {
            Packet candidatePacket = storedPackets.get(i);
            if (candidatePacket == null) continue;


            Connection availableConnection = findAnyFreeOutputConnection(this, gameState);
            if (availableConnection != null) {
                packetToSend = candidatePacket;
                connToSendOn = availableConnection;
                packetIndexInQueue = i;
                break;
            }
        }

        if (packetToSend != null && connToSendOn != null && packetIndexInQueue != -1) {
            storedPackets.remove(packetIndexInQueue);

            packetToSend.startMoving(connToSendOn);
            gameState.addPacket(packetToSend);

            System.out.println("SystemNode " + (this.hashCode() % 1000) + " (Role: " + this.role +
                    ") sent packet: " + packetToSend.getType() + " (from index " + packetIndexInQueue + ")" +
                    " via output port of type: " + connToSendOn.getStartPort().getPacketType() +
                    ". Packet Stability on exit: " + String.format("%.2f", packetToSend.getStabilityOnWire()));
        } else if (!storedPackets.isEmpty()){

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

    private Connection findAnyFreeOutputConnection(SystemNode system, GameState state) {
        if (system == null || state == null) return null;
        List<Connection> freeConnections = new ArrayList<>();

        for (Port port : system.getOutputPorts()) {
            if (port.isConnected()) {
                Connection conn = port.getConnectedConnection();
                if (conn != null) {
                    boolean occupied = isConnectionOccupied(conn, state);
                    if (!occupied) {
                        freeConnections.add(conn);
                    }
                } else {
                }
            }
        }

        if (!freeConnections.isEmpty()) {
            Connection chosen = freeConnections.get(random.nextInt(freeConnections.size()));
            return chosen;
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

    private boolean isConnectionOccupied(Connection connectionToCheck, GameState gameState) {

        if (connectionToCheck == null || gameState == null) return true;
        for (Packet packet : gameState.getPackets()) {
            if (packet.getCurrentConnection() == connectionToCheck && packet.getProgressOnConnection() > 0.01 ) {
                return true;
            }
        }
        return false;
    }

    private void updateIndicatorState() {
        this.indicatorOn = !storedPackets.isEmpty();
    }

    public boolean areAllNodePortsConnected() {
        for (Port port : inputPorts) {
            if (!port.isConnected()) {
                return false;
            }
        }
        for (Port port : outputPorts) {
            if (!port.isConnected()) {
                return false;
            }
        }
        return true;
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