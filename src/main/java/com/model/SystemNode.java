package com.model;

import com.blueprinthell.Config;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Iterator;

public class SystemNode {

    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final List<Port> inputPorts;
    private final List<Port> outputPorts;
    private boolean indicatorOn = false;

    private final List<Packet> storedPackets;
    private static final Random random = new Random();

    public SystemNode(double x, double y) {
        this.x = x;
        this.y = y;
        this.width = Config.SYSTEM_WIDTH;
        this.height = Config.SYSTEM_HEIGHT;
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        this.storedPackets = new ArrayList<>();
        createRandomPorts();
    }

    private void createRandomPorts() {
        int numInputs = random.nextInt(3) + 1;
        double inputSpacing = height / (numInputs + 1.0);
        for (int i = 1; i <= numInputs; i++) {
            PacketType randomType = random.nextBoolean() ? PacketType.SQUARE : PacketType.TRIANGLE;
            double yPos = i * inputSpacing;
            inputPorts.add(new Port(this, PortType.INPUT, randomType, new Point2D(Config.PORT_OFFSET_X, yPos)));
        }

        int numOutputs = random.nextInt(3) + 1;
        double outputSpacing = height / (numOutputs + 1.0);
        double outputX = width - Config.PORT_OFFSET_X;
        for (int i = 1; i <= numOutputs; i++) {
            PacketType randomType = random.nextBoolean() ? PacketType.SQUARE : PacketType.TRIANGLE;
            double yPos = i * outputSpacing;
            outputPorts.add(new Port(this, PortType.OUTPUT, randomType, new Point2D(outputX, yPos)));
        }
    }

    public void receivePacket(Packet packet, GameState gameState) {
        if (packet != null) {
            packet.resetMovementState();

            if (storedPackets.size() < Config.SYSTEM_PACKET_STORAGE_CAPACITY) {
                storedPackets.add(packet);
                System.out.println("Packet received and stored: " + packet.getType());
                gameState.addCoin(packet.getType() == PacketType.SQUARE ? 1 : 2);
            } else {
                System.out.println("Packet discarded, storage full: " + packet.getType());
                gameState.incrementPacketLoss(1);
            }
        }
    }


    public Packet updateTrySend(double deltaTime, GameState gameState) {
        if (!storedPackets.isEmpty()) {
            Iterator<Packet> iterator = storedPackets.iterator();
            while (iterator.hasNext()) {
                Packet packetToSend = iterator.next();
                Port outputPort = findAvailableOutputPort(packetToSend.getType(), gameState);
                if (outputPort != null) {
                    Connection startConnection = findConnectionFromPort(outputPort, gameState);
                    if (startConnection != null) {
                        iterator.remove();
                        System.out.println("System sending stored packet: " + packetToSend.getType());
                        packetToSend.startMoving(startConnection);
                        return packetToSend;
                    } else {
                        System.err.println("System Error: Found port " + outputPort + " but no outgoing connection.");
                    }
                    break;
                }
            }
        }
        return null;
    }



    private Port findAvailableOutputPort(PacketType type, GameState gameState) {
        for (Port port : this.outputPorts) {
            Connection connectedWire = findConnectionFromPort(port, gameState);
            if (connectedWire != null ) {
                return port;
            }
        }
        return null;
    }


    private Connection findConnectionFromPort(Port port, GameState gameState) {
        for (Connection conn : gameState.getConnections()) {
            if (conn.getStartPort() == port) {
                return conn;
            }
        }
        return null;
    }


    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public List<Port> getInputPorts() { return Collections.unmodifiableList(inputPorts); }
    public List<Port> getOutputPorts() { return Collections.unmodifiableList(outputPorts); }
    public boolean isIndicatorOn() { return indicatorOn; }
    public void setIndicatorOn(boolean indicatorOn) { this.indicatorOn = indicatorOn; }
}