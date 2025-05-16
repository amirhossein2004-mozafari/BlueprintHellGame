package com.controller;

import com.blueprinthell.Config;
import com.model.*;
import com.view.GameView;
import com.blueprinthell.MainApplication;

import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GameController {

    private final GameState gameState;
    private final GameView gameView;
    private final Random random = new Random();

    private boolean isWiring = false;
    private Port wiringStartPort = null;
    private Line tempWire = null;
    private double timeSinceLastPacket = 0.0;
    private final double PACKET_SPAWN_INTERVAL = 0.5;
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

        gameState.addSystem(new SystemNode(100, 150, SystemRole.SOURCE, 0, 0, 2, 2));

        gameState.addSystem(new SystemNode(400, 100, SystemRole.INTERMEDIATE, 1, 1, 1, 0));

        gameState.addSystem(new SystemNode(700, 200, SystemRole.INTERMEDIATE, 1, 2, 2, 1));

        gameState.addSystem(new SystemNode(170, 400, SystemRole.INTERMEDIATE, 2, 0, 1, 1));

        gameState.addSystem(new SystemNode(380, 300, SystemRole.INTERMEDIATE, 1, 1, 1, 2));

        gameState.addSystem(new SystemNode(680, 430, SystemRole.INTERMEDIATE, 1, 1, 1, 0));

        gameState.addSystem(new SystemNode(300, 550, SystemRole.SINK, 1, 1, 0, 0));

        gameState.addSystem(new SystemNode(650, 520, SystemRole.SINK, 1, 0, 0, 0));

        gameView.redrawAll(gameState);
        checkAndSetAllPortsConnected();
        System.out.println("Game Initialized. All ports connected: " + gameState.areAllPortsConnected());
    }

    public void updateGame(double scaledDeltaTime) {
        if (gameState == null || mainApp == null || gameState.isGameLogicPaused()) return;

        if (!gameState.isGameRunning()) {
            return;
        }
        if (gameState.isLevelTimeUp() && gameState.getPackets().isEmpty()) {
            System.out.println("Level Time Up & No Packets! Ending level.");
            mainApp.showGameOver(false, gameState.getCoins(), gameState.getPacketLossCount());
            return;
        }
        gameState.incrementGameTime(scaledDeltaTime);
        gameState.updateActiveSkills(scaledDeltaTime);

        if (gameState.canSpawnMorePackets() && !gameState.isLevelTimeUp()) {
            spawnNewPackets(scaledDeltaTime);
        }

        if (!gameState.isSkillActive(ShopSkill.AIRYAMAN)) {
            CollisionDetector.checkPacketPacketCollisions(gameState, gameState.getPackets());
        }
        handlePacketMovementAndArrival(scaledDeltaTime);
        updateSystemLogic(scaledDeltaTime);
        gameView.updatePacketPositions(gameState);
        checkEndConditions();
    }

    private void spawnNewPackets(double scaledDeltaTime) {
        timeSinceLastPacket += scaledDeltaTime;
        if (timeSinceLastPacket >= PACKET_SPAWN_INTERVAL &&
                gameState.canSpawnMorePackets() &&
                !gameState.isLevelTimeUp()) {

            timeSinceLastPacket = 0.0;

            SystemNode sourceSystem = findRandomSourceSystem(gameState);

            if (sourceSystem != null && !sourceSystem.getOutputPorts().isEmpty()) {
                Connection startConnection = findAnyAvailableConnectionFromSystem(sourceSystem, gameState);

                if (startConnection != null) {
                    PacketType typeToSpawn = random.nextBoolean() ? PacketType.SQUARE : PacketType.TRIANGLE;

                    Packet newPacket = new Packet(typeToSpawn, startConnection.getStartPort().getAbsoluteX(), startConnection.getStartPort().getAbsoluteY());

                    if (gameState.getPackets().size() < 50) {
                        newPacket.startMoving(startConnection);
                        gameState.addPacket(newPacket);
                        System.out.println("Spawned packet: " + typeToSpawn + " from System " + sourceSystem.hashCode()%1000 +
                                " on PortType: " + startConnection.getStartPort().getPacketType());
                    }
                }
            }
        }
    }

    private SystemNode findRandomSourceSystem(GameState state) {
        List<SystemNode> sourceSystems = state.getSystems().stream()
                .filter(s -> s.getRole() == SystemRole.SOURCE && !s.getOutputPorts().isEmpty())
                .collect(Collectors.toList());
        if (!sourceSystems.isEmpty()) {
            return sourceSystems.get(random.nextInt(sourceSystems.size()));
        }
        for (SystemNode sys : state.getSystems()) {
            if (!sys.getOutputPorts().isEmpty()) return sys;
        }
        return null;
    }

    private Connection findAnyAvailableConnectionFromSystem(SystemNode system, GameState state) {
        List<Connection> availableConnections = new ArrayList<>();
        if (system == null) return null;
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

    private void handlePacketMovementAndArrival(double scaledDeltaTime) {
        List<Packet> packetsToRemove = new ArrayList<>();
        for (Packet packet : new ArrayList<>(gameState.getPackets())) {
            if (packet.getCurrentSize() <= 0) {
                gameState.incrementPacketLoss(1);
                packetsToRemove.add(packet);
                System.out.println("Packet depleted and lost.");
                continue;
            }

            boolean reachedEnd = packet.update(scaledDeltaTime);

            if (reachedEnd && !packet.isOffWire()) {
                Port destinationPort = packet.getTargetPort();
                if (destinationPort != null && destinationPort.getParentSystem() != null) {
                    SystemNode destinationSystem = destinationPort.getParentSystem();
                    destinationSystem.receivePacket(packet, gameState);
                    if (destinationSystem.getRole() == SystemRole.SINK) {
                        System.out.println("Packet " + packet.getType() + " reached a SINK system!");
                    }
                } else {
                    gameState.incrementPacketLoss(1);
                    System.out.println("Packet lost - wire leads to invalid target.");
                }
                packetsToRemove.add(packet);
            } else if (packet.isOffWire()) {
                double boundsOffset = Config.SYSTEM_WIDTH;
                if (packet.getX() < -boundsOffset || packet.getX() > Config.WINDOW_WIDTH + boundsOffset ||
                        packet.getY() < -boundsOffset || packet.getY() > Config.WINDOW_HEIGHT + boundsOffset) {
                    gameState.incrementPacketLoss(1);
                    packetsToRemove.add(packet);
                    System.out.println("Off-wire packet went OUT OF BOUNDS and lost.");
                }
            }
        }
        for (Packet p : packetsToRemove) gameState.removePacket(p);
    }


    private boolean hasPacketDeviated(Packet packet) {
        if (packet == null) return false;

        Connection conn = packet.getCurrentConnection();

        if (conn == null) {
            double boundsOffset = Math.max(Config.PORT_VISUAL_SIZE_SQUARE, Config.PORT_VISUAL_SIZE_TRIANGLE) * 1.5;
            boolean outOfBounds = packet.getX() < -boundsOffset ||
                    packet.getX() > Config.WINDOW_WIDTH + boundsOffset ||
                    packet.getY() < -boundsOffset ||
                    packet.getY() > Config.WINDOW_HEIGHT + boundsOffset;
            if (outOfBounds) {

            }
            return outOfBounds;
        }

        Port startPort = conn.getStartPort();
        Port endPort = conn.getEndPort();

        if (startPort == null || endPort == null) {
            System.err.println("Warning: Connection has null start or end port in hasPacketDeviated.");
            return true;
        }

        double p1x = startPort.getAbsoluteX();
        double p1y = startPort.getAbsoluteY();
        double p2x = endPort.getAbsoluteX();
        double p2y = endPort.getAbsoluteY();
        double px = packet.getX();
        double py = packet.getY();

        double l2 = (p1x - p2x) * (p1x - p2x) + (p1y - p2y) * (p1y - p2y);

        if (l2 < 0.0001) {
            return new Point2D(px, py).distance(p1x, p1y) > Config.COLLISION_DEVIATION_THRESHOLD;
        }

        double t = ((px - p1x) * (p2x - p1x) + (py - p1y) * (p2y - p1y)) / l2;

        double distanceToLineSegment;
        if (t < 0.0) {
            distanceToLineSegment = new Point2D(px, py).distance(p1x, p1y);
        } else if (t > 1.0) {
            distanceToLineSegment = new Point2D(px, py).distance(p2x, p2y);
        } else {
            double closestX = p1x + t * (p2x - p1x);
            double closestY = p1y + t * (p2y - p1y);
            distanceToLineSegment = new Point2D(px, py).distance(closestX, closestY);
        }

        if (Double.isNaN(distanceToLineSegment)) {
            System.err.println("Warning: Calculated distance is NaN in hasPacketDeviated for packet " + packet.getType());
            return false;
        }

        boolean deviated = distanceToLineSegment > Config.COLLISION_DEVIATION_THRESHOLD;
        return deviated;
    }

    private void updateSystemLogic(double scaledDeltaTime) {
        for (SystemNode system : gameState.getSystems()) {
            system.update(scaledDeltaTime, gameState);
        }
    }

    private void checkEndConditions() {
        if (gameState.getTotalPacketsSpawned() > 0) {
            if (gameState.getPacketLossPercentage() >= Config.MAX_PACKET_LOSS_PERCENTAGE) {
                System.out.println("GAME OVER - Packet Loss limit reached!");
                mainApp.showGameOver(false, gameState.getCoins(), gameState.getPacketLossCount());
            }
        }

        if (gameState.isLevelTimeUp() && !gameState.canSpawnMorePackets() && gameState.getPackets().isEmpty()) {
            System.out.println("Level time up and all spawned packets processed. Calculating result...");
            if(gameState.getPacketLossPercentage() < Config.MAX_PACKET_LOSS_PERCENTAGE) {
                System.out.println("Level WON (Time up, loss within limits). Implement proper win condition logic.");
            } else {
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
        checkAndSetAllPortsConnected();

        System.out.println("Mouse Release. All ports connected: " + gameState.areAllPortsConnected());
    }

    private void checkAndSetAllPortsConnected() {
        if (gameState == null || gameState.getSystems().isEmpty()) {
            gameState.updateAllPortsConnectedStatus(false);
            return;
        }
        for (SystemNode system : gameState.getSystems()) {
            for (Port port : system.getInputPorts()) {
                if (!port.isConnected()) {
                    gameState.updateAllPortsConnectedStatus(false);
                    return;
                }
            }
            for (Port port : system.getOutputPorts()) {
                if (!port.isConnected()) {
                    gameState.updateAllPortsConnectedStatus(false);
                    return;
                }
            }
        }
        gameState.updateAllPortsConnectedStatus(true);
    }

    public void handleKeyPress(KeyCode code) {
        if (gameState == null) return;
        if (code == Config.KEY_TIME_FORWARD) {
            double newScale = gameState.getCurrentTimeScale() + Config.TIME_SCALE_INCREMENT;
            gameState.setCurrentTimeScale(newScale);
            System.out.println("Time scale increased to: " + String.format("%.1f", gameState.getCurrentTimeScale()));
        } else if (code == Config.KEY_TIME_BACKWARD) {
            double newScale = gameState.getCurrentTimeScale() - Config.TIME_SCALE_INCREMENT;
            gameState.setCurrentTimeScale(newScale);
            System.out.println("Time scale decreased to: " + String.format("%.1f", gameState.getCurrentTimeScale()));
        }
    }


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
        if (startPort.getType() != PortType.OUTPUT) {
            return false;
        }
        if (startPort.getParentSystem() == targetPort.getParentSystem()) {
            return false;
        }
        if (startPort.getPacketType() != targetPort.getPacketType()) {
            return false;
        }

        if (targetPort.getParentSystem().isStorageFull() && targetPort.getParentSystem().getRole() != SystemRole.SINK) {
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

    private Connection findConnectionFromPort(Port port, GameState gameState) {
        if (port == null || gameState == null) return null;
        for(Connection conn : gameState.getConnections()){
            if(conn.getStartPort() == port){ return conn; }
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

    public void pauseGameLogic() {
        if (gameState != null) {
            gameState.setGameLogicPaused(true);
            System.out.println("Game Logic Paused (by GameController call)");
        }
    }

    public void resumeGameLogic() {
        if (gameState != null) {
            gameState.setGameLogicPaused(false);
            System.out.println("Game Logic Resumed by Controller");
        }
    }

    public void pauseGame() {
        System.out.println("GameController: Requesting Full Game Pause.");
        pauseGameLogic();
    }

    public void resumeGame() {
        System.out.println("GameController: Requesting Full Game Resume.");
        resumeGameLogic();
    }

}