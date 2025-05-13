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


        double sw = Config.SYSTEM_WIDTH;
        double sh = Config.SYSTEM_HEIGHT;
        double margin = 100;
        double midX = Config.WINDOW_WIDTH / 2.0;
        double midY = Config.WINDOW_HEIGHT / 2.0;

        SystemNode sys0_Source = new SystemNode(margin, margin,
                List.of(),
                List.of(PacketType.SQUARE, PacketType.TRIANGLE, PacketType.SQUARE));
        gameState.addSystem(sys0_Source);
        SystemNode sys1_TR = new SystemNode(Config.WINDOW_WIDTH - margin - sw, margin,
                List.of(PacketType.SQUARE, PacketType.TRIANGLE),
                List.of(PacketType.SQUARE, PacketType.TRIANGLE));
        gameState.addSystem(sys1_TR);

        SystemNode sys2_MidTop = new SystemNode(midX - sw - 30, midY - sh / 2.0 - 30,
                List.of(PacketType.SQUARE, PacketType.TRIANGLE, PacketType.SQUARE),
                List.of(PacketType.TRIANGLE, PacketType.SQUARE, PacketType.TRIANGLE));
        gameState.addSystem(sys2_MidTop);

        SystemNode sys3_MidBottom = new SystemNode(midX + 30, midY + sh / 2.0 - 30,
                List.of(PacketType.TRIANGLE, PacketType.SQUARE, PacketType.TRIANGLE),
                List.of(PacketType.SQUARE, PacketType.SQUARE, PacketType.TRIANGLE));
        gameState.addSystem(sys3_MidBottom);


        SystemNode sys4_LeftMid = new SystemNode(margin + sw + 60, midY - sh / 2,
                List.of(PacketType.SQUARE, PacketType.SQUARE, PacketType.TRIANGLE),
                List.of(PacketType.TRIANGLE, PacketType.SQUARE));
        gameState.addSystem(sys4_LeftMid);

        SystemNode sys5_RightMid = new SystemNode(Config.WINDOW_WIDTH - margin - 2*sw - 60, midY - sh / 2,
                List.of(PacketType.TRIANGLE, PacketType.TRIANGLE, PacketType.SQUARE),
                List.of(PacketType.SQUARE, PacketType.TRIANGLE));
        gameState.addSystem(sys5_RightMid);


        SystemNode sys6_SinkLeft = new SystemNode(margin, Config.WINDOW_HEIGHT - margin - sh,
                List.of(PacketType.SQUARE, PacketType.TRIANGLE, PacketType.SQUARE),
                List.of());
        gameState.addSystem(sys6_SinkLeft);

        SystemNode sys7_SinkRight = new SystemNode(Config.WINDOW_WIDTH - margin - sw, Config.WINDOW_HEIGHT - margin - sh,
                List.of(PacketType.TRIANGLE, PacketType.SQUARE, PacketType.SQUARE),
                List.of());
        gameState.addSystem(sys7_SinkRight);

        gameView.redrawAll(gameState);
        System.out.println("Game Initialized with " + gameState.getSystems().size() + " systems (defined roles).");
    }

    public void updateGame(double scaledDeltaTime) {
        if (gameState == null || mainApp == null) {
            System.err.println("GameState or MainApplication is null in updateGame. Aborting update.");
            return;
        }

        if (gameState.isGameLogicPaused()) {
            return;
        }

        if (gameState.isLevelTimeUp()) {
            if (gameState.getPackets().isEmpty()) {
                System.out.println("Level Time Up! No more packets to process. Ending level via Game Over.");
                mainApp.showGameOver(false, gameState.getCoins(), gameState.getPacketLossCount());
                return;
            } else {
            }
        }

        gameState.incrementGameTime(scaledDeltaTime);

        gameState.updateActiveSkills(scaledDeltaTime);

        if (gameState.canSpawnMorePackets() && !gameState.isLevelTimeUp()) {
            spawnNewPackets(scaledDeltaTime);
        }

        if (gameState.getPackets() != null && !gameState.isSkillActive(ShopSkill.AIRYAMAN)) {
            CollisionDetector.checkPacketPacketCollisions(gameState, gameState.getPackets());
        }

        if (gameState.getPackets() != null) {
            handlePacketMovementAndArrival(scaledDeltaTime);
        }

        if (gameState.getSystems() != null) {
            updateSystemLogic(scaledDeltaTime);
        }

        if (gameView != null) {
            gameView.updatePacketPositions(gameState);
        }

        checkEndConditions();
    }

    private void spawnNewPackets(double scaledDeltaTime) {
        timeSinceLastPacket += scaledDeltaTime;
        if (timeSinceLastPacket >= PACKET_SPAWN_INTERVAL &&
                gameState.canSpawnMorePackets() &&
                !gameState.isLevelTimeUp()) {

            timeSinceLastPacket = 0.0;

            SystemNode sourceSystem = null;
            if (!gameState.getSystems().isEmpty()) {
                List<SystemNode> potentialSources = new ArrayList<>();
                for(SystemNode sys : gameState.getSystems()){
                    if(sys.getInputPorts().isEmpty() && !sys.getOutputPorts().isEmpty()){
                        potentialSources.add(sys);
                    }
                }
                if(potentialSources.isEmpty() && !gameState.getSystems().isEmpty()){
                    potentialSources.add(gameState.getSystems().get(0));
                }

                if(!potentialSources.isEmpty()){
                    sourceSystem = potentialSources.get(random.nextInt(potentialSources.size()));
                }
            }

            if (sourceSystem != null && !sourceSystem.getOutputPorts().isEmpty()) {
                Port sourcePort = sourceSystem.getOutputPorts().get(random.nextInt(sourceSystem.getOutputPorts().size()));

                Connection startConnection = findConnectionFromPort(sourcePort, gameState);

                if (startConnection != null && !isConnectionOccupied(startConnection, gameState)) {
                    PacketType typeToSpawn = sourcePort.getPacketType();

                    Packet newPacket = new Packet(typeToSpawn, startConnection.getStartPort().getAbsoluteX(), startConnection.getStartPort().getAbsoluteY());

                    if (gameState.getPackets().size() < 50) {
                        newPacket.startMoving(startConnection);
                        gameState.addPacket(newPacket);
                    }
                } else {

                }
            } else {

            }
        }
    }



    private void handlePacketMovementAndArrival(double scaledDeltaTime) {
        if (gameState == null || gameState.getPackets() == null) return;

        List<Packet> packetsToRemoveFromGameState = new ArrayList<>();

        for (Packet packet : new ArrayList<>(gameState.getPackets())) {
            if (packet.getCurrentSize() <= 0) {
                System.out.println("Packet " + packet.getType() + " (ID: " + packet.hashCode() % 1000 + ") size became 0 - Considered Lost.");
                gameState.incrementPacketLoss(1);
                packetsToRemoveFromGameState.add(packet);
                continue;
            }

            boolean reachedTargetPortOnWire = packet.update(scaledDeltaTime);

            if (reachedTargetPortOnWire) {
                Port destinationPort = packet.getTargetPort();
                if (destinationPort != null && destinationPort.getParentSystem() != null) {
                    SystemNode destinationSystem = destinationPort.getParentSystem();
                    destinationSystem.receivePacket(packet, gameState);
                } else {
                    gameState.incrementPacketLoss(1);
                    System.out.println("Packet " + packet.getType() + " (ID: " + packet.hashCode() % 1000 + ") lost - reached end of an invalid/disconnected wire segment.");
                }
                packetsToRemoveFromGameState.add(packet);
            } else if (packet.isOffWire()) {
                double boundsCheckOffset = Math.max(Config.PORT_VISUAL_SIZE_SQUARE, Config.PORT_VISUAL_SIZE_TRIANGLE) * 2.0;

                if (packet.getX() < -boundsCheckOffset || packet.getX() > Config.WINDOW_WIDTH + boundsCheckOffset ||
                        packet.getY() < -boundsCheckOffset || packet.getY() > Config.WINDOW_HEIGHT + boundsCheckOffset) {
                    System.out.println("Off-wire packet " + packet.getType() + " (ID: " + packet.hashCode() % 1000 + ") went OUT OF BOUNDS. Lost.");
                    gameState.incrementPacketLoss(1);
                    packetsToRemoveFromGameState.add(packet);
                }
            }
        }

        for (Packet packetToRemove : packetsToRemoveFromGameState) {
            gameState.removePacket(packetToRemove);
        }
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
            double lossPercentage = gameState.getPacketLossPercentage();
            if (lossPercentage >= Config.MAX_PACKET_LOSS_PERCENTAGE) {
                System.out.println("--- GAME OVER --- Packet Loss Reached Limit!");
                if (mainApp != null) {
                    mainApp.showGameOver(false, gameState.getCoins(), gameState.getPacketLossCount());
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

    public void pauseGame() { System.out.println("Game Paused"); }
    public void resumeGame() { System.out.println("Game Resumed"); }
}