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
import com.api.enums.AbilityType;
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

    private boolean isPlacingEffect = false;
    private EffectType effectToPlace;
    private int effectCost;
    private double effectDuration;
    private boolean isMovingSystem = false;
    private SystemNode systemToMove = null;
    private boolean testPacketsSpawned = false;
    private boolean collisionTestPacketsSpawned = false;

    public GameController(GameState gameState, GameView gameView, MainApplication mainApp) {
        if (gameState == null || gameView == null || mainApp == null) {
            throw new IllegalArgumentException("GameState, GameView, and MainApplication cannot be null for GameController");
        }
        this.gameState = gameState;
        this.gameView = gameView;
        this.mainApp = mainApp;
    }

    public void initializeGame() {
        this.testPacketsSpawned = false;
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

        gameState.incrementGameTime(scaledDeltaTime);

        if (gameState.isGameLogicPaused() || !gameState.isGameRunning()) {
            return;
        }

        gameState.updateActiveSkills(scaledDeltaTime);
        updateEffectPoints(scaledDeltaTime);

        if (gameState.canSpawnMorePackets() && !gameState.isLevelTimeUp()) {
            spawnNewPackets(scaledDeltaTime);
        }

        if (!gameState.isSkillActive(AbilityType.AIRYAMAN)) {
            CollisionDetector.checkPacketPacketCollisions(gameState, gameState.getPackets());
        }

        handlePacketMovementAndArrival(scaledDeltaTime);
        updateSystemLogic(scaledDeltaTime);
        updateAntiTrojanSystems(scaledDeltaTime);
        checkAndDestroyWires();

        if (gameView != null) {
            gameView.updatePacketPositions(gameState);
        }
        checkEndConditions();
    }

    private void spawnNewPackets(double scaledDeltaTime) {
        if (gameState == null) return;

        timeSinceLastPacket += scaledDeltaTime;
        if (timeSinceLastPacket < Config.PACKET_SPAWN_INTERVAL) {
            return;
        }
        timeSinceLastPacket = 0;

        if (!gameState.canSpawnMorePackets() || gameState.getPackets().size() >= Config.MAX_ACTIVE_PACKETS) {
            return;
        }

        List<SystemNode> sourceSystems = gameState.getSystemsByRole(SystemRole.SOURCE);
        if (sourceSystems.isEmpty()) {
            return;
        }
        SystemNode source = sourceSystems.get(random.nextInt(sourceSystems.size()));

        List<Port> availablePorts = new ArrayList<>();
        for (Port port : source.getOutputPorts()) {
            Connection conn = port.getConnectedConnection();
            if (conn != null && !isConnectionOccupied(conn, gameState)) {
                availablePorts.add(port);
            }
        }

        if (!availablePorts.isEmpty()) {
            Port chosenPort = availablePorts.get(random.nextInt(availablePorts.size()));


            PacketType[] spawnableTypes;

            if (gameState.getCurrentLevelNumber() >= 5) {
                spawnableTypes = new PacketType[] {
                        PacketType.SQUARE,
                        PacketType.TRIANGLE,
                        PacketType.MESSENGER_INFINITY,
                        PacketType.CONFIDENTIAL_NORMAL,
                        PacketType.LARGE_8,
                        PacketType.LARGE_10
                };
            } else {
                spawnableTypes = new PacketType[] {
                        PacketType.SQUARE,
                        PacketType.TRIANGLE,
                        PacketType.MESSENGER_INFINITY,
                        PacketType.CONFIDENTIAL_NORMAL
                };
            }

            PacketType typeToSpawn = spawnableTypes[random.nextInt(spawnableTypes.length)];


            Packet newPacket = new Packet(typeToSpawn, chosenPort.getAbsoluteX(), chosenPort.getAbsoluteY());

            gameState.addPacket(newPacket);
            gameState.registerSpawnedPacket(newPacket);

            newPacket.startMoving(chosenPort.getConnectedConnection());

            System.out.println("GC.Spawn: Spawned " + typeToSpawn + " for level " + gameState.getCurrentLevelNumber() + ". Total spawned: " + gameState.getTotalPacketsSuccessfullySpawnedCount());
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

        List<Packet> packetsToRemove = new ArrayList<>();
        List<Packet> arrivedPackets = new ArrayList<>();

        for (Packet packet : new ArrayList<>(gameState.getPackets())) {
            if (packet.getCurrentSize() <= 0) {
                packetsToRemove.add(packet);
                continue;
            }

            boolean reachedEndOnWire = packet.update(scaledDeltaTime, gameState);

            if (reachedEndOnWire) {
                packetsToRemove.add(packet);

                arrivedPackets.add(packet);
            }
            else if (packet.isOffWire()) {
                double boundsOffset = Config.OFF_WIRE_BOUNDS_OFFSET;
                boolean outOfBounds = packet.getX() < -boundsOffset ||
                        packet.getX() > Config.WINDOW_WIDTH + boundsOffset ||
                        packet.getY() < -Config.HUD_HEIGHT - boundsOffset ||
                        packet.getY() > Config.WINDOW_HEIGHT + boundsOffset;

                boolean hasStoppedForTooLong = packet.getVelocity().magnitudeSquared() < Config.MIN_VELOCITY_SQUARED_TO_STOP_OFF_WIRE &&
                        packet.getTimeSpentIdleOffWireSeconds() >= Config.MAX_IDLE_TIME_FOR_OFF_WIRE_PACKET_SECONDS;

                if (outOfBounds || hasStoppedForTooLong) {
                    gameState.registerCompletelyLostPacket(packet);
                    packetsToRemove.add(packet);
                }
            }
        }

        for (Packet arrivedPacket : arrivedPackets) {
            Port destPort = arrivedPacket.getTargetPort();
            if (destPort != null && destPort.getParentSystem() != null) {
                destPort.getParentSystem().receivePacket(arrivedPacket, gameState);
            } else {
                gameState.registerCompletelyLostPacket(arrivedPacket);
            }
        }

        if (!packetsToRemove.isEmpty()) {
            for (Packet p : packetsToRemove) {
                if (gameState.removePacket(p)) {
                    if (gameView != null) {
                        gameView.removePacketNode(p);
                    }
                }
            }
        }
    }

    private String getPacketIdSafe(Packet packet) {
        if (packet == null) {
            return "[NULL_PACKET_OBJECT]";
        }
        String id = packet.getId().toString();
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
        if (isPlacingEffect) {
            Point2D localClick = gameView.sceneToLocal(event.getSceneX(), event.getSceneY());

            if (effectToPlace == EffectType.AERGIA_NO_ACCELERATION) {
                Connection clickedConnection = findClickedConnection(localClick.getX(), localClick.getY());
                if (clickedConnection != null) {
                    System.out.println("Aergia effect placed on connection " + clickedConnection.getId());
                    gameState.decreaseCoins(effectCost);

                    EffectPoint newEffect = new EffectPoint(effectToPlace, clickedConnection, localClick, effectDuration);
                    gameState.addEffectPoint(newEffect);

                    gameView.redrawAll(gameState);
                } else {
                    System.out.println("Aergia placement cancelled: No wire was clicked.");
                }
            }
            else if (effectToPlace == EffectType.SISYPHUS_MOVE_SYSTEM) {
                SystemNode clickedSystem = findClickedSystem(localClick.getX(), localClick.getY());

                if (clickedSystem != null &&
                        clickedSystem.getRole() != SystemRole.SOURCE &&
                        clickedSystem.getRole() != SystemRole.SINK) {

                    System.out.println("System " + clickedSystem.getRole() + " selected to be moved.");
                    gameState.decreaseCoins(effectCost);

                    this.isMovingSystem = true;
                    this.systemToMove = clickedSystem;

                } else {
                    System.out.println("Sisyphus placement cancelled: A non-movable system (or nothing) was clicked.");
                }
            }

            isPlacingEffect = false;

            return;
        }

        if (gameView == null || gameState == null || isWiring || isDraggingBendPoint) {
            return;
        }

        Point2D localClick = gameView.sceneToLocal(event.getSceneX(), event.getSceneY());

        final double BEND_POINT_CLICK_RADIUS = 8.0;
        for (Connection conn : gameState.getConnections()) {
            List<Point2D> bendPoints = conn.getBendPoints();
            for (int i = 0; i < bendPoints.size(); i++) {
                if (bendPoints.get(i).distance(localClick) < BEND_POINT_CLICK_RADIUS) {

                    isDraggingBendPoint = true;
                    activeConnection = conn;
                    activeBendPointIndex = i;
                    System.out.println("Started dragging bend point " + i);
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

            if (gameState.getCoins() >= 1) {
                if (gameState.decreaseCoins(1)) {
                    clickedConnection.addBendPoint(localClick);
                    gameView.redrawAll(gameState);
                }
            } else {
                System.out.println("Not enough coins to add a bend point.");
            }
            return;
        }
    }

    public void handleMouseDrag(MouseEvent event) {
        if (gameView == null) return;

        Point2D localDrag = gameView.sceneToLocal(event.getSceneX(), event.getSceneY());

        if (isWiring && tempWire != null) {
            tempWire.setEndX(localDrag.getX());
            tempWire.setEndY(localDrag.getY());

            Port potentialTargetPort = findClickedPort(localDrag.getX(), localDrag.getY(), PortType.INPUT);
            double currentTempLength = new Point2D(wiringStartPort.getAbsoluteX(), wiringStartPort.getAbsoluteY())
                    .distance(localDrag.getX(), localDrag.getY());

            if (potentialTargetPort != null &&
                    !potentialTargetPort.isConnected() &&
                    isValidConnectionAttempt(wiringStartPort, potentialTargetPort) &&
                    gameState.getRemainingWireLength() >= currentTempLength)
            {
                tempWire.setStroke(Config.COLOR_WIRE_TEMP_VALID);
            } else {
                tempWire.setStroke(Config.COLOR_WIRE_TEMP_INVALID);
            }
        }
        else if (isDraggingBendPoint && activeConnection != null) {
            activeConnection.moveBendPoint(activeBendPointIndex, localDrag);
            gameView.redrawAll(gameState);
        }
        else if (isMovingSystem && systemToMove != null) {


            double newX = localDrag.getX() - (systemToMove.getWidth() / 2);
            double newY = localDrag.getY() - (systemToMove.getHeight() / 2);

            systemToMove.setPosition(newX, newY);

            gameView.redrawAll(gameState);
        }
    }



    public void handleMouseRelease(MouseEvent event) {

        if (isWiring && wiringStartPort != null) {
            Point2D localReleaseCoords = gameView.sceneToLocal(event.getSceneX(), event.getSceneY());
            Port targetPort = findClickedPort(localReleaseCoords.getX(), localReleaseCoords.getY(), PortType.INPUT);

            if (targetPort != null && !targetPort.isConnected() && isValidConnectionAttempt(wiringStartPort, targetPort)) {
                double requiredLength = new Point2D(wiringStartPort.getAbsoluteX(), wiringStartPort.getAbsoluteY())
                        .distance(targetPort.getAbsoluteX(), targetPort.getAbsoluteY());

                if (gameState.getRemainingWireLength() >= requiredLength) {
                    if (gameState.useWire(requiredLength)) {
                        Connection newConnection = new Connection(wiringStartPort, targetPort);
                        gameState.addConnection(newConnection);
                        AudioManager.playSoundEffect(AudioManager.SFX_CONNECT_SUCCESS);
                    }
                }
            }
        }

        else if (isDraggingBendPoint) {
            System.out.println("Finished dragging bend point.");
        }
        else if (isMovingSystem) {
            System.out.println("System move finished.");
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

        isMovingSystem = false;
        systemToMove = null;

        isPlacingEffect = false;


        if (gameView != null && gameState != null) {
            gameView.redrawAll(gameState);

            for (SystemNode system : gameState.getSystems()) {
                system.updateAllPortsActuallyConnectedState();
            }
            checkAndSetAllPortsConnected();
        }
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
        final double CLICK_THRESHOLD = 5.0;

        for (Connection conn : gameState.getConnections()) {
            List<Point2D> path = conn.getFullPath();
            for (int i = 0; i < path.size() - 1; i++) {
                Point2D p1 = path.get(i);
                Point2D p2 = path.get(i + 1);

                double segmentDx = p2.getX() - p1.getX();
                double segmentDy = p2.getY() - p1.getY();

                if (segmentDx == 0 && segmentDy == 0) {
                    continue;
                }

                double segmentLengthSq = segmentDx * segmentDx + segmentDy * segmentDy;

                double t = ((clickX - p1.getX()) * segmentDx + (clickY - p1.getY()) * segmentDy) / segmentLengthSq;

                double closestX, closestY;

                if (t < 0) {
                    closestX = p1.getX();
                    closestY = p1.getY();
                } else if (t > 1) {
                    closestX = p2.getX();
                    closestY = p2.getY();
                } else {
                    closestX = p1.getX() + t * segmentDx;
                    closestY = p1.getY() + t * segmentDy;
                }

                double dx = clickX - closestX;
                double dy = clickY - closestY;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < CLICK_THRESHOLD) {
                    return conn;
                }
            }
        }

        return null;
    }


    private void updateAntiTrojanSystems(double scaledDeltaTime) {
        for (SystemNode system : gameState.getSystems()) {
            if (system.getRole() == SystemRole.ANTI_TROJAN) {
                system.performAntiTrojanScan(gameState, scaledDeltaTime);
            }
        }
    }

    private void checkAndDestroyWires() {
        if (gameState == null) return;


        List<Connection> connectionsToRemove = new ArrayList<>();
        for (Connection conn : new ArrayList<>(gameState.getConnections())) {
            if (conn.getLargePacketPassages() >= 3) {
                System.out.println("Connection " + conn.getId() + " marked for destruction.");
                connectionsToRemove.add(conn);
            }
        }

        if (!connectionsToRemove.isEmpty()) {
            for (Connection connToRemove : connectionsToRemove) {
                for (Packet p : new ArrayList<>(gameState.getPackets())) {
                    if (p.getCurrentConnection() == connToRemove) {
                        gameState.registerCompletelyLostPacket(p);
                        gameView.removePacketNode(p);
                        gameState.removePacket(p);
                    }
                }
                if(connToRemove.getStartPort() != null) connToRemove.getStartPort().setConnectedConnection(null);
                if(connToRemove.getEndPort() != null) connToRemove.getEndPort().setConnectedConnection(null);
                gameState.removeConnection(connToRemove);
            }
            gameView.redrawAll(gameState);
        }
    }

    private SystemNode findClickedSystem(double x, double y) {
        for (SystemNode system : gameState.getSystems()) {
            if (x >= system.getX() && x <= system.getX() + system.getWidth() &&
                    y >= system.getY() && y <= system.getY() + system.getHeight()) {
                return system;
            }
        }
        return null;
    }

    public void startEffectPlacement(EffectType type, int cost, double duration) {
        this.isPlacingEffect = true;
        this.effectToPlace = type;
        this.effectCost = cost;
        this.effectDuration = duration;


        if (type == EffectType.AERGIA_NO_ACCELERATION) {
            System.out.println("Now click on a wire to place the Aergia effect.");
        } else if (type == EffectType.SISYPHUS_MOVE_SYSTEM) {
            System.out.println("Now click on an INTERMEDIATE system to move it.");
        } else if (type == EffectType.ELIPHAS_CENTERING_FORCE) {
            System.out.println("Now click on a wire to place the Eliphas effect.");
        }
    }

    private void updateEffectPoints(double scaledDeltaTime) {
        var iterator = gameState.getEffectPoints().iterator();
        boolean needsRedraw = false;
        while (iterator.hasNext()) {
            EffectPoint effect = iterator.next();
            if (effect.update(scaledDeltaTime)) {
                iterator.remove();
                needsRedraw = true;
                System.out.println("Effect " + effect.getType() + " has expired.");
            }
        }

        if (needsRedraw) {
            gameView.redrawAll(gameState);
        }
    }

    public void pauseGameLogic() {
        if (gameState != null) gameState.setGameLogicPaused(true);
    }

    public void resumeGameLogic() {
        if (gameState != null) gameState.setGameLogicPaused(false);
    }
}