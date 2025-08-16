package com.model;

import com.blueprinthell.Config;
import javafx.geometry.Point2D;
import org.json.JSONObject;

import java.util.*;


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

    private double antiTrojanCooldown = 0.0;
    private static final double ANTI_TROJAN_RADIUS = 150.0;
    private static final double ANTI_TROJAN_COOLDOWN_SECONDS = 10.0;
    private final Map<String, List<Packet>> receivedBitPackets = new HashMap<>();

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
        if (packet == null || gameState == null) {
            return;
        }


        if (this.role == SystemRole.VPN) {
            Connection outputConnection = findCompatibleFreeOutputConnection(this, gameState, packet.getType());

            if (outputConnection == null) {
                outputConnection = findAnyFreeOutputConnection(this, gameState);
            }

            if (outputConnection != null) {
                System.out.println("VPN System: Transforming packet to PROTECTED and forwarding.");
                Packet protectedPacket = new Packet(PacketType.PROTECTED, packet.getX(), packet.getY());


                protectedPacket.setVelocity(packet.getVelocity());

                protectedPacket.startMoving(outputConnection);
                gameState.addPacket(protectedPacket);
            } else {

                System.out.println("VPN System: No free output, packet is lost.");
                gameState.registerCompletelyLostPacket(packet);
            }

            gameState.removePacket(packet);
            return;
        }

        if (this.role == SystemRole.DISTRIBUTE) {
            if (packet.getType() == PacketType.LARGE) {
                final int numberOfBits = packet.getCurrentSize();
                final String parentId = packet.getId();

                System.out.println("Distribute system received LARGE packet (" + parentId.substring(0,4) + ") size " + numberOfBits);

                for (int i = 0; i < numberOfBits; i++) {
                    Connection outputConn = findAnyFreeOutputConnection(this, gameState);
                    if (outputConn != null) {
                        Packet bitPacket = new Packet(PacketType.BIT_PACKET, this.getX(), this.getY());


                        bitPacket.setParentLargePacketId(parentId);
                        bitPacket.setTotalBitsForParent(numberOfBits);

                        bitPacket.startMoving(outputConn);
                        gameState.addPacket(bitPacket);
                    }
                }
            } else {

                storedPackets.add(packet);
            }
            gameState.removePacket(packet);
            return;
        }

        if (this.role == SystemRole.MERGE) {
            if (packet.getType() == PacketType.BIT_PACKET) {
                String parentId = packet.getParentLargePacketId();
                if (parentId != null) {

                    List<Packet> bitsForParent = receivedBitPackets.computeIfAbsent(parentId, k -> new ArrayList<>());
                    bitsForParent.add(packet);

                    int totalBitsExpected = packet.getTotalBitsForParent();
                    System.out.println("Merge system received bit-packet for parent " + parentId.substring(0,4) +
                            ". Total received: " + bitsForParent.size() + "/" + totalBitsExpected);

                    if (totalBitsExpected > 0 && bitsForParent.size() == totalBitsExpected) {
                        System.out.println("All bits for " + parentId.substring(0,4) + " collected! Reconstructing LARGE packet.");

                        Packet reconstructedLargePacket = new Packet(PacketType.LARGE, this.getX(), this.getY());
                        reconstructedLargePacket.setCurrentSize(bitsForParent.size());

                        for (Packet bit : bitsForParent) {
                            gameState.removePacket(bit);
                        }

                        receivedBitPackets.remove(parentId);

                        storedPackets.add(reconstructedLargePacket);
                    }
                }
            } else {

                storedPackets.add(packet);
            }

            gameState.removePacket(packet);
            return;
        }


        if (this.role == SystemRole.SABOTEUR) {

            if (packet.getNoiseLevel() == 0.0) {
                packet.addNoise(0.1);
                System.out.println("Saboteur added noise. New level: " + packet.getNoiseLevel());
            }


            if (random.nextDouble() < 0.20) {
                System.out.println("Saboteur converting packet to TROJAN!");
                Packet trojanPacket = new Packet(PacketType.TROJAN, packet.getX(), packet.getY());
                gameState.removePacket(packet);
                packet = trojanPacket;
            }
        }

        boolean canAcceptPacket = (this.role == SystemRole.SINK) || (storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY);

        if (canAcceptPacket) {
            int coinsToAdd = (packet.getType() == PacketType.SQUARE) ?
                    Config.COINS_PER_SQUARE_PACKET_SYSTEM_ENTRY :
                    Config.COINS_PER_TRIANGLE_PACKET_SYSTEM_ENTRY;
            gameState.addCoin(coinsToAdd);

            if (this.role != SystemRole.SINK) {
                packet.setInheritedStabilityDamage(1.0 - packet.getStabilityOnWire());
                packet.getVelocity().set(0,0);
                packet.setCurrentConnection(null);
                storedPackets.add(packet);
            }
        } else {
            gameState.registerCompletelyLostPacket(packet);
        }

        gameState.removePacket(packet);
    }

    public void update(double deltaTime, GameState gameState) {
        if (gameState == null || storedPackets.isEmpty()) {
            return;
        }

        Packet packetToSend = null;
        Connection connToSendOn = null;
        int packetIndexInQueue = -1;

        for (int i = 0; i < storedPackets.size(); i++) {
            Packet candidatePacket = storedPackets.get(i);
            Connection availableConnection;

            if (this.role == SystemRole.SPY) {
                List<SystemNode> allSpySystems = new ArrayList<>();
                gameState.getSystems().stream().filter(s -> s.getRole() == SystemRole.SPY).forEach(allSpySystems::add);
                Collections.shuffle(allSpySystems);

                availableConnection = null;
                for (SystemNode spySystem : allSpySystems) {
                    availableConnection = findCompatibleFreeOutputConnection(spySystem, gameState, candidatePacket.getType());
                    if (availableConnection != null) break;
                }
            } else if (this.role == SystemRole.SABOTEUR) {
                availableConnection = findIncompatibleFreeOutputConnection(this, gameState, candidatePacket.getType());
                if (availableConnection == null) availableConnection = findAnyFreeOutputConnection(this, gameState);
            } else {
                availableConnection = findCompatibleFreeOutputConnection(this, gameState, candidatePacket.getType());
                if (availableConnection == null) availableConnection = findAnyFreeOutputConnection(this, gameState);
            }

            if (availableConnection != null) {
                packetToSend = candidatePacket;
                connToSendOn = availableConnection;
                packetIndexInQueue = i;
                break;
            }
        }

        if (packetToSend != null) {
            if(this.role == SystemRole.SPY) {
                Port exitPort = connToSendOn.getStartPort();
                packetToSend.setX(exitPort.getAbsoluteX());
                packetToSend.setY(exitPort.getAbsoluteY());
            }
            storedPackets.remove(packetIndexInQueue);
            packetToSend.startMoving(connToSendOn);
            gameState.addPacket(packetToSend);
        }
    }

    private Connection findCompatibleFreeOutputConnection(SystemNode system, GameState state, PacketType packetType) {
        if (system == null || state == null || packetType == null) return null;
        List<Connection> freeCompatibleConnections = new ArrayList<>();

        for (Port port : system.getOutputPorts()) {
            if (port.getPacketType() == packetType && port.isConnected()) {
                Connection conn = port.getConnectedConnection();
                if (conn != null && !isConnectionOccupied(conn, state)) {
                    freeCompatibleConnections.add(conn);
                }
            }
        }

        if (!freeCompatibleConnections.isEmpty()) {
            return freeCompatibleConnections.get(random.nextInt(freeCompatibleConnections.size()));
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
        List<Connection> freeConnections = new ArrayList<>();

        for (Port port : system.getOutputPorts()) {
            if (port.isConnected()) {
                Connection conn = port.getConnectedConnection();
                if (conn != null && !isConnectionOccupied(conn, state)) {
                    freeConnections.add(conn);
                }
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

    private Connection findIncompatibleFreeOutputConnection(SystemNode system, GameState state, PacketType packetType) {
        if (system == null || state == null || packetType == null) return null;
        List<Connection> freeIncompatibleConnections = new ArrayList<>();

        for (Port port : system.getOutputPorts()) {
            if (port.getPacketType() != packetType && port.isConnected()) {
                Connection conn = port.getConnectedConnection();
                if (conn != null && !isConnectionOccupied(conn, state)) {
                    freeIncompatibleConnections.add(conn);
                }
            }
        }

        if (!freeIncompatibleConnections.isEmpty()) {
            return freeIncompatibleConnections.get(random.nextInt(freeIncompatibleConnections.size()));
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