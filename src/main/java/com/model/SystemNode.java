package com.model;

import com.blueprinthell.Config;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;


public class SystemNode {

    private double x;
    private double y;
    private final double width;
    private final double height;
    private final SystemRole role;
    private final List<Port> inputPorts;
    private final List<Port> outputPorts;
    private final List<Packet> storedPackets;
    private boolean indicatorOn = false;
    private static final Random random = new Random();
    private boolean allPortsActuallyConnected = false;

    private double antiTrojanCooldown = 0.0;
    private static final double ANTI_TROJAN_RADIUS = 150.0;
    private static final double ANTI_TROJAN_COOLDOWN_SECONDS = 10.0;
    private final Map<String, List<Packet>> receivedBitPackets = new HashMap<>();
    private final List<Packet> packetsToSend = new ArrayList<>();
    private UUID id;

    public SystemNode(double x, double y, SystemRole role,
                      int numSqIn, int numTriIn, int numLgIn, int numMsgIn,
                      int numSqOut, int numTriOut, int numLgOut, int numMsgOut) {
        this.x = x; this.y = y; this.role = role;
        this.width = Config.SYSTEM_WIDTH; this.height = Config.SYSTEM_HEIGHT;
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        this.storedPackets = new ArrayList<>();
        createSpecificPorts(numSqIn, numTriIn, numLgIn, numMsgIn, numSqOut, numTriOut, numLgOut, numMsgOut);
        validatePortsBasedOnRole();
        this.id = UUID.randomUUID();
    }


    public SystemNode(double x, double y, SystemRole role) {
        this(x, y, role, 0, 0, 0, 0, 0, 0, 0, 0);

        switch (role) {
            case SOURCE: createSpecificPorts(0, 0, 0, 0, 1, 1, 1, 1); break;
            case SINK: createSpecificPorts(1, 1, 1, 1, 0, 0, 0, 0); break;
            case VPN:
            case SPY:
            case INTERMEDIATE:
            case SABOTEUR:
                createSpecificPorts(1, 1, 0, 1, 1, 1, 0, 1); break;
            case ANTI_TROJAN: createSpecificPorts(0,0,0,0,0,0,0,0); break;
            case DISTRIBUTE: createSpecificPorts(0,0,1,0,2,2,0,0); break;
            case MERGE: createSpecificPorts(2,2,0,0,0,0,1,0); break;
        }
        validatePortsBasedOnRole();
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

    private void createSpecificPorts(int numSquareIn, int numTriangleIn, int numLargeIn, int numMessengerIn,
                                     int numSquareOut, int numTriangleOut, int numLargeOut, int numMessengerOut) {
        List<PacketType> inputTypes = new ArrayList<>();
        for (int i = 0; i < numSquareIn; i++) inputTypes.add(PacketType.SQUARE);
        for (int i = 0; i < numTriangleIn; i++) inputTypes.add(PacketType.TRIANGLE);
        for (int i = 0; i < numLargeIn; i++) inputTypes.add(PacketType.LARGE);
        for (int i = 0; i < numMessengerIn; i++) inputTypes.add(PacketType.MESSENGER_INFINITY);

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
        for (int i = 0; i < numLargeOut; i++) outputTypes.add(PacketType.LARGE);
        for (int i = 0; i < numMessengerOut; i++) {
            outputTypes.add(PacketType.MESSENGER_INFINITY);
        }

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

        if (this.role != SystemRole.SINK) {
            int coinsToAdd = 0;
            switch (packet.getType()) {
                case SQUARE: coinsToAdd = 2; break;
                case TRIANGLE: coinsToAdd = 3; break;
                case MESSENGER_INFINITY: coinsToAdd = 1; break;
                case CONFIDENTIAL_NORMAL: coinsToAdd = 3; break;
                case CONFIDENTIAL_DISTANCING: coinsToAdd = 4; break;
                case PROTECTED: coinsToAdd = Config.COINS_PER_PROTECTED_PACKET_SYSTEM_ENTRY; break;
            }
            if (coinsToAdd > 0) gameState.addCoin(coinsToAdd);
        }

        packet.setCurrentConnection(null);
        packet.setLastVisitedSystem(this);

        if (packet.isLargePacket() && (this.role != SystemRole.SOURCE && this.role != SystemRole.DISTRIBUTE)) {
            this.wipeStoredPackets();
            this.randomizePort();
        }

        if ((this.role == SystemRole.SABOTEUR || this.role == SystemRole.SPY) && packet.getType() == PacketType.PROTECTED) {
            Packet revertedPacket = new Packet(packet.getOriginalType(), packet.getX(), packet.getY());
            if(storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY) {
                storedPackets.add(revertedPacket);
                System.out.println("Protected packet has been reverted to " + revertedPacket.getType());
            } else {
                gameState.registerCompletelyLostPacket(revertedPacket);
            }
            return;
        }


        if (this.role == SystemRole.SINK) {
            return;
        }
        else if (this.role == SystemRole.DISTRIBUTE) {
            if (packet.isLargePacket()) {
                final int numberOfBits = packet.getInitialSize();
                final String parentId = packet.getId().toString();
                Color batchColor = Color.rgb(random.nextInt(156) + 100, random.nextInt(156) + 100, random.nextInt(156) + 100);
                for (int i = 0; i < numberOfBits; i++) {
                    Connection exit = findAnyFreeOutputConnection(this, gameState);
                    if (exit != null) {
                        Packet bitPacket = new Packet(PacketType.BIT_PACKET, this.getX(), this.getY());
                        bitPacket.setParentLargePacketId(parentId);
                        bitPacket.setTotalBitsForParent(numberOfBits);
                        bitPacket.setVisualColor(batchColor);
                        gameState.addPacket(bitPacket);
                        bitPacket.startMoving(exit);
                    }
                }
            } else {
                if (storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY) {
                    storedPackets.add(packet);
                } else {
                    gameState.registerCompletelyLostPacket(packet);
                }
            }
        }
        else if (this.role == SystemRole.MERGE) {
            if (packet.getType() == PacketType.BIT_PACKET) {
                String parentId = packet.getParentLargePacketId();
                if (parentId != null) {
                    List<Packet> bitsForParent = receivedBitPackets.computeIfAbsent(parentId, k -> new ArrayList<>());
                    bitsForParent.add(packet);
                    int totalBitsExpected = packet.getTotalBitsForParent();
                    if (totalBitsExpected > 0 && bitsForParent.size() == totalBitsExpected) {
                        Packet reconstructed = new Packet(
                                (totalBitsExpected == 8) ? PacketType.LARGE_8 : PacketType.LARGE_10, this.getX(), this.getY());
                        if (storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY) {
                            storedPackets.add(reconstructed);
                        } else {
                            gameState.registerCompletelyLostPacket(reconstructed);
                        }
                        receivedBitPackets.remove(parentId);
                    }
                }
            } else {
                if (storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY) {
                    storedPackets.add(packet);
                } else {
                    gameState.registerCompletelyLostPacket(packet);
                }
            }
        }
        else if (this.role == SystemRole.VPN || this.role == SystemRole.SPY || this.role == SystemRole.INTERMEDIATE || this.role == SystemRole.SABOTEUR) {
            if (packet.getType() == PacketType.BIT_PACKET) {
                storedPackets.add(packet);
            }
            else if (storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY) {
                storedPackets.add(packet);
            } else {
                gameState.registerCompletelyLostPacket(packet);
            }
        }
    }

    public void update(double deltaTime, GameState gameState) {
        if (gameState == null || storedPackets.isEmpty()) {
            return;
        }

        var packetIterator = storedPackets.iterator();
        while (packetIterator.hasNext()) {

            Packet originalPacket = packetIterator.next();
            Packet packetToSend = originalPacket;

            if (this.role == SystemRole.VPN) {
                if (originalPacket.getType() == PacketType.CONFIDENTIAL_NORMAL) {
                    packetToSend = new Packet(PacketType.CONFIDENTIAL_DISTANCING, originalPacket.getX(), originalPacket.getY());
                }
                else if (originalPacket.getType() != PacketType.PROTECTED && originalPacket.getType() != PacketType.CONFIDENTIAL_DISTANCING) {
                    packetToSend = new Packet(PacketType.PROTECTED, originalPacket.getX(), originalPacket.getY());
                    packetToSend.setOriginalType(originalPacket.getType());
                }
            } else if (this.role == SystemRole.SABOTEUR) {
                if (originalPacket.getNoiseLevel() == 0.0) originalPacket.addNoise(0.1);
                if (random.nextDouble() < 0.20 && originalPacket.getType() != PacketType.PROTECTED) {
                    packetToSend = new Packet(PacketType.TROJAN, originalPacket.getX(), originalPacket.getY());
                }
            }

            List<Connection> availableExits = new ArrayList<>();
            if (this.role == SystemRole.SPY) {
                for (SystemNode spyNode : gameState.getSystemsByRole(SystemRole.SPY)) {
                    if (spyNode == this || spyNode == originalPacket.getLastVisitedSystem()) continue;
                    for (Port p : spyNode.getOutputPorts()) {
                        if (p.isConnected() && !isConnectionOccupied(p.getConnectedConnection(), gameState)) {
                            availableExits.add(p.getConnectedConnection());
                        }
                    }
                }
            } else {
                for (Port port : this.getOutputPorts()) {
                    if (port.isConnected() && !isConnectionOccupied(port.getConnectedConnection(), gameState)) {
                        availableExits.add(port.getConnectedConnection());
                    }
                }
            }

            if (availableExits.isEmpty()) {
                break;
            }

            Connection chosenExit = null;
            if (this.role == SystemRole.SABOTEUR) {
                List<Connection> incompatible = new ArrayList<>();
                for (Connection c : availableExits) {
                    if (c.getStartPort().getPacketType() != packetToSend.getType()) {
                        incompatible.add(c);
                    }
                }
                chosenExit = !incompatible.isEmpty() ? incompatible.get(random.nextInt(incompatible.size())) : availableExits.get(random.nextInt(availableExits.size()));
            } else {
                List<Connection> compatible = new ArrayList<>();
                for (Connection c : availableExits) {
                    if (c.getStartPort().getPacketType() == packetToSend.getType()) {
                        compatible.add(c);
                    }
                }
                chosenExit = !compatible.isEmpty() ? compatible.get(random.nextInt(compatible.size())) : availableExits.get(random.nextInt(availableExits.size()));
            }

            if (chosenExit != null) {

                gameState.addPacket(packetToSend);

                if (this.role == SystemRole.SPY && chosenExit.getStartPort().getParentSystem() != this) {
                    packetToSend.setX(chosenExit.getStartPort().getAbsoluteX());
                    packetToSend.setY(chosenExit.getStartPort().getAbsoluteY());
                }

                packetToSend.startMoving(chosenExit);
                packetIterator.remove();
            } else {
                break;
            }
        }
    }

    private Connection findBestExit(Packet packet, GameState gameState) {
        List<Connection> availableExits = new ArrayList<>();
        if (this.role == SystemRole.SPY) {
            List<SystemNode> spyNetwork = gameState.getSystemsByRole(SystemRole.SPY);
            for(SystemNode spyNode : spyNetwork){
                if(spyNode == this || spyNode == packet.getLastVisitedSystem()) continue;
                for(Port p : spyNode.getOutputPorts()){
                    if(p.isConnected() && !isConnectionOccupied(p.getConnectedConnection(), gameState))
                        availableExits.add(p.getConnectedConnection());
                }
            }
        } else {
            for (Port port : this.getOutputPorts()) {
                if (port.isConnected() && !isConnectionOccupied(port.getConnectedConnection(), gameState)) {
                    availableExits.add(port.getConnectedConnection());
                }
            }
        }

        if (availableExits.isEmpty()) return null;

        List<Connection> compatibleExits = availableExits.stream()
                .filter(c -> c.getStartPort().getPacketType() == packet.getType())
                .collect(Collectors.toList());

        if (!compatibleExits.isEmpty()) {
            return compatibleExits.get(random.nextInt(compatibleExits.size()));
        }

        if(this.role == SystemRole.INTERMEDIATE){
            return availableExits.get(random.nextInt(availableExits.size()));
        }

        return null;
    }


    private Connection findCompatibleFreeOutputConnection(SystemNode system, GameState state, PacketType packetType) {
        if (system == null || state == null || packetType == null) return null;

        List<Connection> candidates = new ArrayList<>();

        for (Port port : system.getOutputPorts()) {
            if (port.getPacketType() == packetType && port.isConnected()) {
                candidates.add(port.getConnectedConnection());
            }
        }

        if (candidates.isEmpty()) return null;


        List<Connection> freeConnections = new ArrayList<>();
        for (Connection conn : candidates) {
            if (!isConnectionOccupied(conn, state)) {
                freeConnections.add(conn);
            }
        }

        if (!freeConnections.isEmpty()) {
            return freeConnections.get(random.nextInt(freeConnections.size()));
        }

        return null;
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

        List<Connection> candidates = new ArrayList<>();
        for (Port port : system.getOutputPorts()) {
            if (port.isConnected()) {
                candidates.add(port.getConnectedConnection());
            }
        }

        if (candidates.isEmpty()) return null;

        List<Connection> freeConnections = new ArrayList<>();
        for (Connection conn : candidates) {
            if (!isConnectionOccupied(conn, state)) {
                freeConnections.add(conn);
            }
        }

        if (!freeConnections.isEmpty()) {
            return freeConnections.get(random.nextInt(freeConnections.size()));
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
        if (connectionToCheck == null || gameState == null) {
            return true;
        }
        for (Packet packet : gameState.getPackets()) {

            if (packet.getCurrentConnection() == connectionToCheck) {
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

            if (this.role == SystemRole.SPY) {
                continue;
            }

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

    private Connection findIncompatibleFreeOutputConnection(SystemNode system, GameState state, PacketType packetType) {
        if (system == null || state == null || packetType == null) return null;

        List<Connection> candidates = new ArrayList<>();
        for (Port port : system.getOutputPorts()) {
            if (port.getPacketType() != packetType && port.isConnected()) {
                candidates.add(port.getConnectedConnection());
            }
        }

        if (candidates.isEmpty()) return null;

        List<Connection> freeConnections = new ArrayList<>();
        for (Connection conn : candidates) {
            if (!isConnectionOccupied(conn, state)) {
                freeConnections.add(conn);
            }
        }

        if (!freeConnections.isEmpty()) {
            return freeConnections.get(random.nextInt(freeConnections.size()));
        }

        return null;
    }


    public void performAntiTrojanScan(GameState gameState, double deltaTime) {
        if (antiTrojanCooldown > 0) {
            antiTrojanCooldown = Math.max(0, antiTrojanCooldown - deltaTime);
            return;
        }
        Vector2D antiTrojanPos = new Vector2D(this.x + this.width/2, this.y + this.height/2);
        for (Packet packet : new ArrayList<>(gameState.getPackets())) {
            if (packet.getType() == PacketType.TROJAN) {
                if (antiTrojanPos.distance(new Vector2D(packet.getX(), packet.getY())) <= ANTI_TROJAN_RADIUS) {
                    Packet messengerPacket = new Packet(PacketType.MESSENGER_INFINITY, packet.getX(), packet.getY());
                    messengerPacket.setVelocity(packet.getVelocity());
                    messengerPacket.setCurrentConnection(packet.getCurrentConnection());
                    gameState.removePacket(packet);
                    gameState.addPacket(messengerPacket);
                    this.antiTrojanCooldown = ANTI_TROJAN_COOLDOWN_SECONDS;
                    return;
                }
            }
        }
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("x", this.x);
        json.put("y", this.y);
        json.put("role", this.role.toString());

        return json;
    }

    public List<Packet> getPacketsToSend() {
        return packetsToSend;
    }



    public void wipeStoredPackets() {
        if (!storedPackets.isEmpty()) {
            System.out.println("System " + (this.hashCode() % 1000) + " storage wiped by LARGE packet!");
            storedPackets.clear();
        }
    }


    public void randomizePort() {
        List<Port> allPorts = new ArrayList<>();
        allPorts.addAll(inputPorts);
        allPorts.addAll(outputPorts);
        if (allPorts.isEmpty()) return;

        Port portToChange = allPorts.get(random.nextInt(allPorts.size()));
        PacketType oldType = portToChange.getPacketType();
        PacketType newType;
        if (oldType == PacketType.SQUARE) newType = PacketType.TRIANGLE;
        else if (oldType == PacketType.TRIANGLE) newType = PacketType.SQUARE;
        else return;

        portToChange.setPacketType(newType);
        System.out.println("System " + (this.hashCode()%1000) + ": Port type randomized from " + oldType + " to " + newType);
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;

    }

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
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