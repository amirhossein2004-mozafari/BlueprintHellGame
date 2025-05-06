package com.controller;

import com.blueprinthell.Config;
import com.model.*;
import com.view.GameView;
import com.blueprinthell.MainApplication;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

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
    private final MainApplication mainApp;


    public GameController(GameState gameState, GameView gameView, MainApplication mainApp) {
        if(gameState == null || gameView == null || mainApp == null) {
            throw new IllegalArgumentException("GameState, GameView, and MainApplication cannot be null for GameController");
        }
        this.gameState = gameState;
        this.gameView = gameView;
        this.mainApp = mainApp;
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
        checkEndConditions();

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
                if (destinationPort != null && destinationPort.getParentSystem() != null) {
                    destinationPort.getParentSystem().receivePacket(packet, gameState);
                } else {
                    gameState.incrementPacketLoss(1);
                }
                packetsToRemove.add(packet);
            }
            else {
                if (hasPacketDeviated(packet)) {
                    gameState.incrementPacketLoss(1);
                    packetsToRemove.add(packet);
                    System.out.println("Packet lost - deviated too far.");
                }
            }
        }
        for (Packet packet : packetsToRemove) {
            gameState.removePacket(packet);
        }
    }

    private boolean hasPacketDeviated(Packet packet) {
        Connection conn = packet.getCurrentConnection();
        if (conn == null) return false;

        Port start = conn.getStartPort();
        Port end = conn.getEndPort();

        double x1 = start.getAbsoluteX();
        double y1 = start.getAbsoluteY();
        double x2 = end.getAbsoluteX();
        double y2 = end.getAbsoluteY();
        double xp = packet.getX();
        double yp = packet.getY();

        double A = y2 - y1;
        double B = x1 - x2;
        double C = x2 * y1 - y2 * x1;

        double distance = Math.abs(A * xp + B * yp + C) / Math.sqrt(A * A + B * B);

        if (Double.isNaN(distance)) return false;
        return distance > Config.COLLISION_DEVIATION_THRESHOLD;
    }

    private void updateSystemLogic(double deltaTime){
        for (SystemNode system : gameState.getSystems()) {
            system.update(deltaTime, gameState);
        }
    }

    private void checkEndConditions() {
        if (gameState == null || Config.MAX_PACKET_LOSS_PERCENTAGE <= 0) return;

        if (gameState.getTotalPacketsSpawned() > 0) {
            double lossPercentage = gameState.getPacketLossPercentage();

            if (lossPercentage >= Config.MAX_PACKET_LOSS_PERCENTAGE) {
                System.out.println("--- GAME OVER --- Packet Loss Reached Limit!");
                if(mainApp != null) {
                    mainApp.showGameOver(false, gameState.getCoins(), gameState.getPacketLossCount());
                } else {
                    System.err.println("Error: MainApplication reference is null in GameController. Cannot trigger Game Over screen.");
                }
            }
        }
    }


    public void handleMousePress(MouseEvent event) {
        if (isWiring) return;

        Point2D localClick = gameView.sceneToLocal(event.getSceneX(), event.getSceneY());
        System.out.println("Mouse Scene Coords: (" + event.getSceneX() + ", " + event.getSceneY() + ")");
        System.out.println("Mouse GameView Local Coords: (" + localClick.getX() + ", " + localClick.getY() + ")");

        Port clickedPort = findClickedPort(localClick.getX(), localClick.getY(), PortType.OUTPUT);

        if (clickedPort != null  ) {
            isWiring = true;
            wiringStartPort = clickedPort;

            tempWire = new Line(clickedPort.getAbsoluteX(), clickedPort.getAbsoluteY(),
                    localClick.getX(), localClick.getY());

            tempWire.setStrokeWidth(Config.WIRE_THICKNESS);
            tempWire.setStroke(Config.COLOR_WIRE_TEMP_INVALID);
            tempWire.setMouseTransparent(true);
            gameView.addNode(tempWire);
        } else {
            System.out.println("Click was not on a valid output port (using local coords).");
        }
    }

    public void handleMouseDrag(MouseEvent event) {
        if (isWiring && tempWire != null) {
            Point2D localDrag = gameView.sceneToLocal(event.getSceneX(), event.getSceneY());
            tempWire.setEndX(localDrag.getX());
            tempWire.setEndY(localDrag.getY());

            Port potentialTargetPort = findClickedPort(localDrag.getX(), localDrag.getY(), PortType.INPUT);
            double currentTempLength = new Point2D(wiringStartPort.getAbsoluteX(), wiringStartPort.getAbsoluteY())
                    .distance(localDrag.getX(), localDrag.getY());

            if (potentialTargetPort != null &&
                    potentialTargetPort.getType() == PortType.INPUT &&
                    isValidConnectionAttempt(wiringStartPort, potentialTargetPort) &&
                    gameState.getRemainingWireLength() >= currentTempLength) {
                tempWire.setStroke(Config.COLOR_WIRE_TEMP_VALID);
            } else {
                tempWire.setStroke(Config.COLOR_WIRE_TEMP_INVALID);
            }
        }
    }

    public void handleMouseRelease(MouseEvent event) {
        boolean connectionMadeSuccessfully = false;

        if (isWiring && wiringStartPort != null) {
            Point2D localReleaseCoords = gameView.sceneToLocal(event.getSceneX(), event.getSceneY());

            Port targetPort = findClickedPort(localReleaseCoords.getX(), localReleaseCoords.getY(), PortType.INPUT);

            System.out.println("MouseRelease - Attempting to connect from: " + wiringStartPort.getPacketType() +
                    " on system " + wiringStartPort.getParentSystem().hashCode()%1000);
            if (targetPort != null) {
                System.out.println("MouseRelease - Potential target port: " + targetPort.getPacketType() +
                        " on system " + targetPort.getParentSystem().hashCode()%1000 +
                        ", StorageFull: " + targetPort.getParentSystem().isStorageFull());
            } else {
                System.out.println("MouseRelease - No target port found at release point.");
            }


            if (isValidConnectionAttempt(wiringStartPort, targetPort)) {
                double requiredLength = new Point2D(wiringStartPort.getAbsoluteX(), wiringStartPort.getAbsoluteY())
                        .distance(targetPort.getAbsoluteX(), targetPort.getAbsoluteY());

                if (gameState.getRemainingWireLength() >= requiredLength) {
                    if (gameState.useWire(requiredLength)) {
                        Connection newConnection = new Connection(wiringStartPort, targetPort);
                        gameState.addConnection(newConnection);
                        connectionMadeSuccessfully = true;
                        System.out.println("Connection CREATED! Length: " + String.format("%.0f", requiredLength) +
                                ", Remaining Wire: " + String.format("%.0f", gameState.getRemainingWireLength()));
                    } else {
                        System.out.println("Wiring FAILED (in release): Could not use wire (error in useWire logic).");
                    }
                } else {
                    System.out.println("Wiring FAILED (in release): Not enough wire length. Required: " +
                            String.format("%.0f", requiredLength) +
                            ", Available: " + String.format("%.0f", gameState.getRemainingWireLength()));
                }
            } else {
                System.out.println("Wiring FAILED (in release): isValidConnectionAttempt returned false.");
                if (targetPort == null) {
                } else if (targetPort.getParentSystem().isStorageFull()) {
                    System.out.println("Reason (explicit check): Target system storage is full.");
                }
            }
        }

        isWiring = false;
        wiringStartPort = null;
        if (tempWire != null) {
            gameView.removeNode(tempWire);
            tempWire = null;
        }
        gameView.redrawAll(gameState);

        if (!connectionMadeSuccessfully && (event.getEventType() == MouseEvent.MOUSE_RELEASED && wiringStartPort == null  )) {

        }
    }

    public void handleKeyPress(KeyCode code) { System.out.println("Key Pressed: " + code); }


    private Port findClickedPort(double clickX, double clickY, PortType typeToFind) {
        for (SystemNode system : gameState.getSystems()) {
            List<Port> portsToCheck = (typeToFind == PortType.INPUT) ? system.getInputPorts() : system.getOutputPorts();
            for (Port port : portsToCheck) {
                if (port.getType() == typeToFind) {
                    double portCenterX = port.getAbsoluteX();
                    double portCenterY = port.getAbsoluteY();

                    double effectiveRadius;
                    if (port.getPacketType() == PacketType.SQUARE) {
                        effectiveRadius = Config.PORT_VISUAL_SIZE_SQUARE;
                    } else {
                        effectiveRadius = Config.PORT_VISUAL_SIZE_TRIANGLE;
                    }

                    double clickPadding = 1.5;
                    double hitRadius = effectiveRadius + clickPadding;

                    double distanceSquared = Math.pow(clickX - portCenterX, 2) + Math.pow(clickY - portCenterY, 2);

                    if (distanceSquared < hitRadius * hitRadius) {
                        System.out.println("Found Port via distance: " + port.getPacketType());
                        return port;
                    }
                }
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
        if (startPort == null || targetPort == null) {
            return false;
        }
        if (targetPort.getType() != PortType.INPUT) {
            return false;
        }
        if (startPort.getParentSystem() == targetPort.getParentSystem()) {
            return false;
        }
        if (startPort.getPacketType() != targetPort.getPacketType()) {
            return false;
        }

        if (targetPort.getParentSystem().isStorageFull()) {
            System.out.println("Validation fail: Target system storage is FULL.");
            return false;
        }

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