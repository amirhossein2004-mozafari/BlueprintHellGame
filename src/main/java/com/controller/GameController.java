package com.controller;

import com.blueprinthell.Config;
import com.model.*;
import com.view.GameView;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameController {

    private final GameState gameState;
    private final GameView gameView;
    private final Random random = new Random();

    private boolean isWiring = false;
    private Port wiringStartPort = null;
    private Line tempWire = null;
    private double timeSinceLastPacket = 0.0;
    private final double PACKET_SPAWN_INTERVAL = 2.0;

    public GameController(GameState gameState, GameView gameView) {
        this.gameState = gameState;
        this.gameView = gameView;
    }

    public void initializeGame() {
        gameState.clearSystems();
        gameState.clearPackets();
        gameState.clearConnections();
        double[][] positions = { {150, 100}, {700, 100}, {400, 300}, {150, 500}, {700, 500} };
        for (int i = 0; i < 5; i++) {
            if (i < positions.length) {
                SystemNode sys = new SystemNode(positions[i][0], positions[i][1]);
                gameState.addSystem(sys);
            }
        }
        gameView.redrawAll(gameState);
        System.out.println("Game Initialized with " + gameState.getSystems().size() + " systems.");
    }

    public void updateGame(double deltaTime) {
        spawnNewPackets(deltaTime);
        handlePacketMovementAndArrival(deltaTime);
        updateSystemLogic(deltaTime);

        gameView.updatePacketPositions(gameState);

    }

    private void spawnNewPackets(double deltaTime) {
        timeSinceLastPacket += deltaTime;
        if (timeSinceLastPacket >= PACKET_SPAWN_INTERVAL && !gameState.getSystems().isEmpty()) {
            timeSinceLastPacket -= PACKET_SPAWN_INTERVAL;
            SystemNode sourceSystem = gameState.getSystems().get(0);

            PacketType typeToSpawn = random.nextBoolean() ? PacketType.SQUARE : PacketType.TRIANGLE;

            Connection startConnection = findAvailableConnectionFromSystem(sourceSystem, gameState);

            if (startConnection != null) {
                Packet newPacket = new Packet(typeToSpawn, startConnection.getStartPort().getAbsoluteX(), startConnection.getStartPort().getAbsoluteY());
                if (gameState.getPackets().size() < 50) {
                    newPacket.startMoving(startConnection);
                    gameState.addPacket(newPacket);
                    System.out.println("Spawned packet: " + typeToSpawn + " on port type: " + startConnection.getStartPort().getPacketType());
                }
            }
        }
    }

    private void handlePacketMovementAndArrival(double deltaTime) {
        List<Packet> packetsToRemove = new ArrayList<>();
        for (Packet packet : new ArrayList<>(gameState.getPackets())) {
            boolean reachedEnd = packet.update(deltaTime);
            if (reachedEnd) {
                Port destinationPort = packet.getTargetPort();
                if (destinationPort != null) {
                    SystemNode destinationSystem = destinationPort.getParentSystem();
                    destinationSystem.receivePacket(packet, gameState);
                } else {
                    gameState.incrementPacketLoss(1);
                }
                packetsToRemove.add(packet);
            }
        }
        for (Packet packet : packetsToRemove) {
            gameState.removePacket(packet);
            System.out.println("Packet reached end/lost: " + packet.getType());
        }
    }

    private void updateSystemLogic(double deltaTime){
        for (SystemNode system : gameState.getSystems()) {
            system.update(deltaTime, gameState);
        }
    }


    public void handleMousePress(MouseEvent event) {
        if (isWiring) return;
        Port clickedPort = findClickedPort(event.getX(), event.getY(), PortType.OUTPUT);
        if (clickedPort != null) {
            isWiring = true;
            wiringStartPort = clickedPort;
            tempWire = new Line(clickedPort.getAbsoluteX(), clickedPort.getAbsoluteY(), event.getX(), event.getY());
            tempWire.setStrokeWidth(Config.WIRE_THICKNESS);
            tempWire.setStroke(Config.COLOR_WIRE_TEMP_VALID);
            tempWire.setMouseTransparent(true);
            gameView.addNode(tempWire);
        }
    }

    public void handleMouseDrag(MouseEvent event) {
        if (isWiring && tempWire != null) {
            tempWire.setEndX(event.getX());
            tempWire.setEndY(event.getY());
        }
    }

    public void handleMouseRelease(MouseEvent event) {
        boolean connectionMade = false;
        if (isWiring) {
            Port targetPort = findClickedPort(event.getX(), event.getY(), PortType.INPUT);
            if (isValidConnectionAttempt(wiringStartPort, targetPort)) {
                double requiredLength = new Point2D(wiringStartPort.getAbsoluteX(), wiringStartPort.getAbsoluteY())
                        .distance(targetPort.getAbsoluteX(), targetPort.getAbsoluteY());
                if (gameState.getRemainingWireLength() >= requiredLength) {
                    Connection newConnection = new Connection(wiringStartPort, targetPort);
                    if (gameState.useWire(requiredLength)) {
                        gameState.addConnection(newConnection);
                        connectionMade = true;
                        System.out.println("Connection created! L=" + String.format("%.0f", requiredLength) + ", Rem=" + String.format("%.0f", gameState.getRemainingWireLength()));
                    }
                } else { System.out.println("Wire Error: Not enough wire."); }
            } else { System.out.println("Wire Error: Invalid target."); }
        }
        isWiring = false;
        wiringStartPort = null;
        if (tempWire != null) { gameView.removeNode(tempWire); tempWire = null; }
        if (!connectionMade && tempWire != null ) { System.out.println("Wiring cancelled."); }
        gameView.redrawAll(gameState);
    }

    public void handleKeyPress(KeyCode code) { System.out.println("Key Pressed: " + code); }


    private Port findClickedPort(double clickX, double clickY, PortType typeToFind) {
        for (SystemNode system : gameState.getSystems()) {
            List<Port> portsToCheck = (typeToFind == PortType.INPUT) ? system.getInputPorts() : system.getOutputPorts();
            for (Port port : portsToCheck) {
                double portX = port.getAbsoluteX(); double portY = port.getAbsoluteY();
                double clickRadiusMultiplier = 1.8;
                double portHitRadius = (port.getPacketType() == PacketType.SQUARE ? Config.PORT_VISUAL_SIZE_SQUARE : Config.PORT_VISUAL_SIZE_TRIANGLE) * clickRadiusMultiplier;
                double distSq = Math.pow(clickX - portX, 2) + Math.pow(clickY - portY, 2);
                if (distSq < portHitRadius * portHitRadius) { return port; }
            }
        }
        return null;
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

    private boolean isValidConnectionAttempt(Port startPort, Port targetPort) {
        if (targetPort == null || startPort == null) return false;
        if (targetPort.getParentSystem() == startPort.getParentSystem()) return false;
        if (targetPort.getPacketType() != startPort.getPacketType()) return false;

        return true;
    }

    private Connection findRandomAvailableConnectionFromSystem(SystemNode system, GameState state) {
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

    private Connection findConnectionFromPort(Port port, GameState state) {
        for(Connection conn : state.getConnections()){ if(conn.getStartPort() == port){ return conn; }} return null;
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

    public void pauseGameLogic() { System.out.println("Game Logic Paused"); }
    public void resumeGameLogic() { System.out.println("Game Logic Resumed"); }
    public void pauseGame() { System.out.println("Game Paused"); }
    public void resumeGame() { System.out.println("Game Resumed"); }
}