package com.controller;

import com.blueprinthell.Config;
import com.blueprinthell.MainApplication;
import com.model.*;
import com.utils.AudioManager;
import com.view.GameView;
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
    private final MainApplication mainApp;
    private final Random random = new Random();

    private boolean isWiring = false;
    private Port wiringStartPort = null;
    private Line tempWire = null;
    private double timeSinceLastPacket = 0.0;

    private boolean isDraggingBendPoint = false;
    private Connection activeConnection = null;
    private int activeBendPointIndex = -1;


    public GameController(GameState gameState, GameView gameView, MainApplication mainApp) {
        if (gameState == null || gameView == null || mainApp == null) {
            throw new IllegalArgumentException("GameState, GameView, and MainApplication cannot be null for GameController");
        }
        this.gameState = gameState;
        this.gameView = gameView;
        this.mainApp = mainApp;
    }

    public void initializeGame() {
        gameState.clearPackets();
        gameState.clearConnections();

        if (gameState != null) {
            for (SystemNode system : gameState.getSystems()) {
                system.updateAllPortsActuallyConnectedState();
            }
        }

        if (gameView != null && gameState != null) {
            gameView.redrawAll(gameState);
        }
        checkAndSetAllPortsConnected();
        System.out.println("GameController.initializeGame: Game Initialized. Overall HUD ports connected: " + gameState.areAllPortsConnected());
    }

    public void updateGame(double scaledDeltaTime) {
        if (gameState == null || mainApp == null) {
            return;
        }
        if (gameState.isGameLogicPaused() || !gameState.isGameRunning()) {
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

        if (gameView != null) {
            gameView.updatePacketPositions(gameState);
        }
        checkEndConditions();
    }

    private void spawnNewPackets(double scaledDeltaTime) {
        if (gameState == null) return;

        timeSinceLastPacket += scaledDeltaTime;

        if (timeSinceLastPacket >= Config.PACKET_SPAWN_INTERVAL) {
            timeSinceLastPacket = 0.0;

            if (gameState.canSpawnMorePackets() && !gameState.isLevelTimeUp()) {
                if (gameState.getPackets().size() < Config.MAX_ACTIVE_PACKETS) {
                    SystemNode sourceSystem = findRandomSourceSystem(gameState);
                    System.out.println("GC.Spawn: MAX_ACTIVE_PACKETS (" + Config.MAX_ACTIVE_PACKETS + ") reached. Cannot spawn.");
                    if (sourceSystem != null) {
                        Connection startConnection = findAnyAvailableConnectionFromSystem(sourceSystem, gameState);
                        if (startConnection != null) {
                            PacketType typeToSpawn = random.nextBoolean() ? PacketType.SQUARE : PacketType.TRIANGLE;
                            Packet newPacket = new Packet(
                                    typeToSpawn,
                                    startConnection.getStartPort().getAbsoluteX(),
                                    startConnection.getStartPort().getAbsoluteY()
                            );

                            gameState.registerSpawnedPacket(newPacket);
                            newPacket.startMoving(startConnection);
                            gameState.addPacket(newPacket);
                            System.out.println("GC.Spawn: Packet " + typeToSpawn + " spawned from Source " + sourceSystem.hashCode() % 1000 + ". Total successfully spawned: " + gameState.getTotalPacketsSuccessfullySpawnedCount());

                        } else {System.out.println("GC.Spawn: No available connection from Source System " + sourceSystem.hashCode() % 1000);
                        }
                    } else {
                    }
                } else {
                }
            } else {
                if (!gameState.canSpawnMorePackets()) {
                }
                if (gameState.isLevelTimeUp()) {
                }
            }
        }
    }

    private SystemNode findRandomSourceSystem(GameState state) {
        if (state == null) return null;
        List<SystemNode> sourceSystems = state.getSystems().stream()
                .filter(s -> s.getRole() == SystemRole.SOURCE && !s.getOutputPorts().isEmpty())
                .collect(Collectors.toList());
        if (!sourceSystems.isEmpty()) {
            return sourceSystems.get(random.nextInt(sourceSystems.size()));
        }
        for (SystemNode sys : state.getSystems()) {
            if (!sys.getOutputPorts().isEmpty() && sys.getRole() == SystemRole.SOURCE) return sys;
        }
        return null;
    }

    private Connection findAnyAvailableConnectionFromSystem(SystemNode system, GameState state) {
        if (system == null || state == null) return null;
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

    private void handlePacketMovementAndArrival(double scaledDeltaTime) {
        if (gameState == null) {
            return;
        }

        List<Packet> packetsToRemoveFromActiveList = new ArrayList<>();

        for (Packet packet : new ArrayList<>(gameState.getPackets())) {
            if (packet == null) {
                continue;
            }

            if (packet.getCurrentSize() <= 0) {
                packetsToRemoveFromActiveList.add(packet);
                continue;
            }

            boolean reachedEndOnWire = packet.update(scaledDeltaTime);

            if (reachedEndOnWire) {
                Port destinationPort = packet.getTargetPort();
                if (destinationPort != null && destinationPort.getParentSystem() != null) {
                    SystemNode destinationSystem = destinationPort.getParentSystem();
                    destinationSystem.receivePacket(packet, gameState);
                    if (packet.getCurrentConnection() == null) {
                        packetsToRemoveFromActiveList.add(packet);
                    }
                } else {
                    gameState.registerCompletelyLostPacket(packet);
                    packetsToRemoveFromActiveList.add(packet);
                }
            } else if (packet.isOffWire()) {
                double boundsOffset = Config.OFF_WIRE_BOUNDS_OFFSET;
                boolean outOfBounds = packet.getX() < -boundsOffset || packet.getX() > Config.WINDOW_WIDTH + boundsOffset ||
                        packet.getY() < -Config.HUD_HEIGHT - boundsOffset ||
                        packet.getY() > Config.WINDOW_HEIGHT + boundsOffset;

                if (outOfBounds) {
                    gameState.registerCompletelyLostPacket(packet);
                    packetsToRemoveFromActiveList.add(packet);
                } else {
                    if (packet.getVelocity().magnitudeSquared() < Config.MIN_VELOCITY_SQUARED_TO_STOP_OFF_WIRE &&
                            packet.getTimeSpentIdleOffWireSeconds() >= Config.MAX_IDLE_TIME_FOR_OFF_WIRE_PACKET_SECONDS) {

                        gameState.registerCompletelyLostPacket(packet);
                        packetsToRemoveFromActiveList.add(packet);
                    }
                }
            }
        }

        for (Packet p : packetsToRemoveFromActiveList) {
            if (p == null) continue;

            boolean removedFromState = gameState.removePacket(p);
            String packetIdForLog = getPacketIdSafe(p);

            if (removedFromState) {
                if (gameView != null) {
                    gameView.removePacketNode(p);
                }
                System.out.println("GC.PacketMovement: Packet " + packetIdForLog + " removed from active game and view.");
            } else {
                System.err.println("GC.PacketMovement WARNING: Failed to remove packet " + packetIdForLog + " from gameState's active list (was it already removed or not found?).");
            }
        }
    }

    private String getPacketIdSafe(Packet packet) {
        if (packet == null) {
            return "[NULL_PACKET_OBJECT]";
        }
        String id = packet.getId();
        if (id == null || id.isEmpty()) {
            return "[EMPTY_OR_NULL_ID]";
        }
        return (id.length() < 4) ? id : id.substring(0, 4);
    }

    private void updateSystemLogic(double scaledDeltaTime) {
        if (gameState == null) return;
        for (SystemNode system : gameState.getSystems()) {
            system.update(scaledDeltaTime, gameState);
        }
    }

    private void checkEndConditions() {
        if (gameState == null || mainApp == null || !gameState.isGameRunning()) {
            return;
        }

        if (gameState.getTotalInitialSizeOfSpawnedPackets() > 0 &&
                gameState.getPacketLossPercentageBySize() >= Config.MAX_PACKET_LOSS_PERCENTAGE) {

            System.out.println("GC.EndConditions: GAME OVER - Packet Loss limit reached! Loss: " +
                    String.format("%.1f%%", gameState.getPacketLossPercentageBySize()));
            gameState.setGameRunning(false);
            mainApp.showGameOver(false, gameState.getCoins(), (int) gameState.getPacketLossPercentageBySize());
            return;
        }

        boolean noMorePacketsToSpawn = !gameState.canSpawnMorePackets() || gameState.isLevelTimeUp();
        boolean allSpawnedPacketsAccountedFor = gameState.getPackets().isEmpty();

        boolean allSystemsProcessedTheirPackets = true;
        for (SystemNode system : gameState.getSystems()) {
            if (system.getRole() == SystemRole.INTERMEDIATE && system.getStoredPacketCount() > 0) {
                allSystemsProcessedTheirPackets = false;
                break;
            }
        }

        if (noMorePacketsToSpawn && allSpawnedPacketsAccountedFor && allSystemsProcessedTheirPackets) {

            gameState.setGameRunning(false);

            double finalLossPercentage = gameState.getPacketLossPercentageBySize();
            String finalLossString = String.format("%.1f%%", finalLossPercentage);

            int requiredPackets = gameState.getPacketsToSpawnInLevel();



            if (gameState.getTotalInitialSizeOfSpawnedPackets() == 0 && requiredPackets > 0) {
                mainApp.showGameOver(false, gameState.getCoins(), 100);
            } else if (finalLossPercentage < Config.MAX_PACKET_LOSS_PERCENTAGE) {
                mainApp.showGameOver(true, gameState.getCoins(), (int) finalLossPercentage);
            } else {
                mainApp.showGameOver(false, gameState.getCoins(), (int) finalLossPercentage);
            }
        } else if (gameState.isLevelTimeUp() && !allSpawnedPacketsAccountedFor && !allSystemsProcessedTheirPackets) {

            if (gameState.getTotalPacketsSuccessfullySpawnedCount() == 0 && gameState.getPacketsToSpawnInLevel() > 0) {
                gameState.setGameRunning(false);
                mainApp.showGameOver(false, gameState.getCoins(), 100);
                return;
            }
        }
    }

    public void handleMousePress(MouseEvent event) {
        if (gameView == null || isWiring || isDraggingBendPoint) return;

        Point2D localClick = gameView.sceneToLocal(event.getSceneX(), event.getSceneY());

        final double BEND_POINT_CLICK_RADIUS = 6.0;
        for (Connection conn : gameState.getConnections()) {
            List<Point2D> bendPoints = conn.getBendPoints();
            for (int i = 0; i < bendPoints.size(); i++) {
                if (bendPoints.get(i).distance(localClick) < BEND_POINT_CLICK_RADIUS) {

                    isDraggingBendPoint = true;
                    activeConnection = conn;
                    activeBendPointIndex = i;
                    System.out.println("Started dragging bend point " + i + " of connection " + conn.getId());
                    return;
                }
            }
        }


        Port clickedPort = findClickedPort(localClick.getX(), localClick.getY(), PortType.OUTPUT);
        if (clickedPort != null && !clickedPort.isConnected()) {

            isWiring = true;
            wiringStartPort = clickedPort;
            tempWire = new Line(clickedPort.getAbsoluteX(), clickedPort.getAbsoluteY(), localClick.getX(), localClick.getY());
            tempWire.setStrokeWidth(Config.WIRE_THICKNESS);
            tempWire.setStroke(Config.COLOR_WIRE_TEMP_INVALID);
            tempWire.setMouseTransparent(true);
            gameView.addNode(tempWire);
            return;
        }


        Connection clickedConnection = findClickedConnection(localClick.getX(), localClick.getY());
        if (clickedConnection != null) {
            System.out.println("Wire clicked! Attempting to add a bend point.");
            if (gameState.getCoins() >= 1) {
                if (gameState.decreaseCoins(1)) {
                    clickedConnection.addBendPoint(localClick);
                    System.out.println("Bend point added to connection " + clickedConnection.getId());

                    gameView.redrawAll(gameState);
                }
            } else {
                System.out.println("Not enough coins to add a bend point!");
            }
        }
    }

    public void handleMouseDrag(MouseEvent event) {
        if (gameView == null) return;
        Point2D localDrag = gameView.sceneToLocal(event.getSceneX(), event.getSceneY());


        if (isWiring && tempWire != null && wiringStartPort != null) {
            tempWire.setEndX(localDrag.getX());
            tempWire.setEndY(localDrag.getY());

            Port potentialTargetPort = findClickedPort(localDrag.getX(), localDrag.getY(), PortType.INPUT);
            double currentTempLength = new Point2D(wiringStartPort.getAbsoluteX(), wiringStartPort.getAbsoluteY())
                    .distance(localDrag.getX(), localDrag.getY());

            if (potentialTargetPort != null && !potentialTargetPort.isConnected() &&
                    isValidConnectionAttempt(wiringStartPort, potentialTargetPort) &&
                    gameState.getRemainingWireLength() >= currentTempLength) {
                tempWire.setStroke(Config.COLOR_WIRE_TEMP_VALID);
            } else {
                tempWire.setStroke(Config.COLOR_WIRE_TEMP_INVALID);
            }
        }

        else if (isDraggingBendPoint && activeConnection != null) {
            activeConnection.moveBendPoint(activeBendPointIndex, localDrag);

            gameView.redrawAll(gameState);
        }
    }

    public void handleMouseRelease(MouseEvent event) {

        if (isWiring && wiringStartPort != null && tempWire != null) {
            boolean connectionMadeSuccessfully = false;
            Point2D localReleaseCoords = gameView.sceneToLocal(event.getSceneX(), event.getSceneY());
            Port targetPort = findClickedPort(localReleaseCoords.getX(), localReleaseCoords.getY(), PortType.INPUT);

            if (targetPort != null && !targetPort.isConnected() && isValidConnectionAttempt(wiringStartPort, targetPort)) {
                double requiredLength = new Point2D(wiringStartPort.getAbsoluteX(), wiringStartPort.getAbsoluteY())
                        .distance(targetPort.getAbsoluteX(), targetPort.getAbsoluteY());

                if (gameState.getRemainingWireLength() >= requiredLength) {
                    if (gameState.useWire(requiredLength)) {
                        Connection newConnection = new Connection(wiringStartPort, targetPort);
                        gameState.addConnection(newConnection);
                        connectionMadeSuccessfully = true;
                        AudioManager.playSoundEffect(AudioManager.SFX_CONNECT_SUCCESS);
                    }
                }
            }

            gameView.redrawAll(gameState);
            checkAndSetAllPortsConnected();
        }

        else if (isDraggingBendPoint) {
            System.out.println("Finished dragging bend point.");
        }


        isWiring = false;
        wiringStartPort = null;
        if (tempWire != null) {
            gameView.removeNode(tempWire);
            tempWire = null;
        }

        isDraggingBendPoint = false;
        activeConnection = null;
        activeBendPointIndex = -1;
    }

    private void checkAndSetAllPortsConnected() {
        if (gameState == null) return;
        if (gameState.getSystems().isEmpty()) {
            gameState.updateAllPortsConnectedStatus(false);
            return;
        }
        boolean allRequiredPortsConnected = true;
        for (SystemNode system : gameState.getSystems()) {

            if (!system.areAllNodePortsConnected()) {
                allRequiredPortsConnected = false;
                break;
            }
        }
        gameState.updateAllPortsConnectedStatus(allRequiredPortsConnected);
    }

    public void handleKeyPress(KeyCode code) {
        if (gameState == null) return;
        if (code == Config.KEY_TIME_FORWARD) {
            gameState.setCurrentTimeScale(gameState.getCurrentTimeScale() + Config.TIME_SCALE_INCREMENT);
            System.out.println("Time scale: " + String.format("%.1f", gameState.getCurrentTimeScale()));
        } else if (code == Config.KEY_TIME_BACKWARD) {
            gameState.setCurrentTimeScale(gameState.getCurrentTimeScale() - Config.TIME_SCALE_INCREMENT);
            System.out.println("Time scale: " + String.format("%.1f", gameState.getCurrentTimeScale()));
        }
    }

    private Port findClickedPort(double clickX, double clickY, PortType typeToFind) {
        if (gameState == null) return null;
        for (SystemNode system : gameState.getSystems()) {
            List<Port> portsToCheck = (typeToFind == PortType.INPUT) ? system.getInputPorts() : system.getOutputPorts();
            for (Port port : portsToCheck) {
                if (port.getType() == typeToFind) {
                    double portCenterX = port.getAbsoluteX();
                    double portCenterY = port.getAbsoluteY();
                    double size = (port.getPacketType() == PacketType.SQUARE) ? Config.PORT_VISUAL_SIZE_SQUARE : Config.PORT_VISUAL_SIZE_TRIANGLE;
                    double hitRadius = size + Config.PORT_CLICK_PADDING;

                    if (Math.pow(clickX - portCenterX, 2) + Math.pow(clickY - portCenterY, 2) < hitRadius * hitRadius) {
                        return port;
                    }
                }
            }
        }
        return null;
    }

    private Connection findConnectionFromPort(Port port, GameState state) {
        if (port == null || state == null) return null;
        for(Connection conn : state.getConnections()){
            if(conn.getStartPort() == port || conn.getEndPort() == port){
                return conn;
            }
        }
        return null;
    }

    private boolean isConnectionOccupied(Connection connectionToCheck, GameState gameState) {
        if (connectionToCheck == null || gameState == null) return true;
        for (Packet packet : gameState.getPackets()) {
            if (packet.getCurrentConnection() == connectionToCheck) {
                if (packet.getProgressOnConnection() < Config.ARRIVAL_PROGRESS_THRESHOLD - 0.01) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isValidConnectionAttempt(Port startPort, Port targetPort) {
        if (startPort == null || targetPort == null) return false;
        if (targetPort.getType() != PortType.INPUT || startPort.getType() != PortType.OUTPUT) return false;
        if (startPort.getParentSystem() == targetPort.getParentSystem()) return false;
        if (startPort.getPacketType() != targetPort.getPacketType()) return false;
        if (targetPort.isConnected()) return false;
        if (startPort.isConnected()) return false;
        if (targetPort.getParentSystem().getRole() != SystemRole.SINK && targetPort.getParentSystem().isStorageFull()) {
            System.out.println("GC.Validation: Target system storage is FULL (and not SINK).");
            return false;
        }
        return true;
    }

    private Connection findClickedConnection(double clickX, double clickY) {
        if (gameState == null) return null;
        final double clickThreshold = 5.0;

        for (Connection conn : gameState.getConnections()) {
            List<Point2D> path = conn.getFullPath();
            for (int i = 0; i < path.size() - 1; i++) {
                Point2D p1 = path.get(i);
                Point2D p2 = path.get(i + 1);


                double left = Math.min(p1.getX(), p2.getX()) - clickThreshold;
                double right = Math.max(p1.getX(), p2.getX()) + clickThreshold;
                double top = Math.min(p1.getY(), p2.getY()) - clickThreshold;
                double bottom = Math.max(p1.getY(), p2.getY()) + clickThreshold;

                if (clickX >= left && clickX <= right && clickY >= top && clickY <= bottom) {

                    return conn;
                }
            }
        }
        return null;
    }

    public void pauseGameLogic() {
        if (gameState != null) gameState.setGameLogicPaused(true);
    }

    public void resumeGameLogic() {
        if (gameState != null) gameState.setGameLogicPaused(false);
    }
}