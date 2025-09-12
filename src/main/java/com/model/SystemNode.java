package com.model;

import com.blueprinthell.Config;
import javafx.geometry.Point2D;
import org.json.JSONObject;

import java.util.*;


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
                createSpecificPorts(0, 0, 1, 1);
                break;
            case SINK:
                createSpecificPorts(1, 1, 0, 0);
                break;
            case VPN:
            case SABOTEUR:
            case SPY:
                createSpecificPorts(1, 1, 1, 1);
                break;
            case ANTI_TROJAN:
                createSpecificPorts(0,0,0,0);
                break;
            case DISTRIBUTE:
            case MERGE:
                createSpecificPorts(2, 2, 2, 2);
                break;
            case INTERMEDIATE:
            default:
                createSpecificPorts(1, 1, 1, 1);
                break;
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


        if (packet.isLargePacket() && (this.role != SystemRole.SOURCE && this.role != SystemRole.DISTRIBUTE)) {
            this.wipeStoredPackets();
            this.randomizePort();
        }


        if (this.role == SystemRole.VPN) {
            Connection exit = findAnyFreeOutputConnection(this, gameState);
            if (exit != null) {
                Packet protectedPacket = new Packet(PacketType.PROTECTED, packet.getX(), packet.getY());
                gameState.addPacket(protectedPacket);
                protectedPacket.startMoving(exit);
            }
        }
        else if (this.role == SystemRole.DISTRIBUTE) {
            if (packet.isLargePacket()) {
                final int numberOfBits = packet.getCurrentSize();
                final String parentId = packet.getId();
                for (int i = 0; i < numberOfBits; i++) {
                    Connection exit = findAnyFreeOutputConnection(this, gameState);
                    if (exit != null) {
                        Packet bitPacket = new Packet(PacketType.BIT_PACKET, this.getX(), this.getY());
                        bitPacket.setParentLargePacketId(parentId);
                        bitPacket.setTotalBitsForParent(numberOfBits);
                        gameState.addPacket(bitPacket);
                        bitPacket.startMoving(exit);
                    }
                }
            } else {
                storedPackets.add(packet);
            }
        }
        if (this.role == SystemRole.MERGE) {
            if (packet.getType() == PacketType.BIT_PACKET) {
                String parentId = packet.getParentLargePacketId();
                if (parentId != null) {

                    List<Packet> bitsForParent = receivedBitPackets.computeIfAbsent(parentId, k -> new ArrayList<>());
                    bitsForParent.add(packet);

                    int totalBitsExpected = packet.getTotalBitsForParent();
                    System.out.println("Merge system received bit-packet for parent " + parentId.substring(0, 4) +
                            ". Total received: " + bitsForParent.size() + "/" + totalBitsExpected);

                    if (totalBitsExpected > 0 && bitsForParent.size() == totalBitsExpected) {
                        System.out.println("All bits for " + parentId.substring(0, 4) + " collected! Reconstructing LARGE packet.");

                        Packet reconstructedLargePacket = new Packet(
                                (totalBitsExpected == 8) ? PacketType.LARGE_8 : PacketType.LARGE_10,
                                this.getX(),
                                this.getY()
                        );


                        Connection exitConn = findAnyFreeOutputConnection(this, gameState);
                        if (exitConn != null) {
                            System.out.println("Found a free exit. Forwarding the new LARGE packet immediately.");

                            gameState.addPacket(reconstructedLargePacket);

                            SystemNode nextSystem = exitConn.getEndPort().getParentSystem();
                            nextSystem.wipeStoredPackets();
                            nextSystem.randomizePort();

                            reconstructedLargePacket.startMoving(exitConn);
                        } else {

                            System.out.println("No free exit found. Storing reconstructed packet for later.");
                            storedPackets.add(reconstructedLargePacket);
                        }


                        receivedBitPackets.remove(parentId);
                    }
                }
            } else {

                storedPackets.add(packet);
            }
        }

        else if (this.role == SystemRole.SABOTEUR) {
            if (packet.getNoiseLevel() == 0.0) packet.addNoise(0.1);
            if (random.nextDouble() < 0.20) {
                Packet trojan = new Packet(PacketType.TROJAN, packet.getX(), packet.getY());
                gameState.addPacket(trojan);
                storedPackets.add(trojan);
            } else {
                storedPackets.add(packet);
            }
        }
        else if (this.role == SystemRole.INTERMEDIATE || this.role == SystemRole.SPY) {
            if (storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY) {
                packet.getVelocity().set(0, 0);
                packet.setCurrentSegmentIndex(0);
                storedPackets.add(packet);
            } else {
                gameState.registerCompletelyLostPacket(packet);
            }
        }
        else if (this.role == SystemRole.SINK) {

        }
    }


    public void update(double deltaTime, GameState gameState) {
        if (gameState == null || storedPackets.isEmpty()) {
            return;
        }

        List<Connection> availableExits;



        if (this.role == SystemRole.SPY) {

            availableExits = new ArrayList<>();
            List<SystemNode> spyNetwork = gameState.getSystemsByRole(SystemRole.SPY);
            for (SystemNode spyNode : spyNetwork) {
                for (Port port : spyNode.getOutputPorts()) {
                    if (port.isConnected() && !isConnectionOccupied(port.getConnectedConnection(), gameState)) {
                        availableExits.add(port.getConnectedConnection());
                    }
                }
            }
        } else {

            availableExits = new ArrayList<>();
            for (Port port : this.getOutputPorts()) {
                if (port.isConnected() && !isConnectionOccupied(port.getConnectedConnection(), gameState)) {
                    availableExits.add(port.getConnectedConnection());
                }
            }
        }


        if (availableExits.isEmpty()) {
            return;
        }


        var packetIterator = storedPackets.iterator();
        while (packetIterator.hasNext()) {
            if (availableExits.isEmpty()) {
                break;
            }

            Packet packetToSend = packetIterator.next();
            Connection chosenExit = null;


            if (this.role == SystemRole.SABOTEUR) {

                List<Connection> incompatibleExits = availableExits.stream()
                        .filter(c -> c.getStartPort().getPacketType() != packetToSend.getType())
                        .toList();
                if (!incompatibleExits.isEmpty()) {
                    chosenExit = incompatibleExits.get(random.nextInt(incompatibleExits.size()));
                } else {
                    chosenExit = availableExits.get(random.nextInt(availableExits.size()));
                }
            } else {

                List<Connection> compatibleExits = availableExits.stream()
                        .filter(c -> c.getStartPort().getPacketType() == packetToSend.getType())
                        .toList();
                if (!compatibleExits.isEmpty()) {
                    chosenExit = compatibleExits.get(random.nextInt(compatibleExits.size()));
                } else {

                    if(this.role == SystemRole.INTERMEDIATE){
                        chosenExit = availableExits.get(random.nextInt(availableExits.size()));
                    }
                }
            }


            if (chosenExit != null) {

                if (chosenExit.getStartPort().getParentSystem() != this) {
                    Port exitPort = chosenExit.getStartPort();
                    packetToSend.setX(exitPort.getAbsoluteX());
                    packetToSend.setY(exitPort.getAbsoluteY());
                }

                packetToSend.startMoving(chosenExit);
                gameState.addPacket(packetToSend);

                packetIterator.remove();
                availableExits.remove(chosenExit);
            }
        }
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

        if (this.inputPorts.isEmpty()) {
            return;
        }

        Port portToChange = this.inputPorts.get(random.nextInt(this.inputPorts.size()));

        PacketType oldType = portToChange.getPacketType();
        PacketType newType = (oldType == PacketType.SQUARE) ? PacketType.TRIANGLE : PacketType.SQUARE;

        System.out.println("System " + (this.hashCode()%1000) + ": Port type randomized from " + oldType + " to " + newType);
        portToChange.setPacketType(newType);


    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;

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