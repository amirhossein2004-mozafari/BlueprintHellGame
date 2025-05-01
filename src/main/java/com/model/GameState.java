package com.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.blueprinthell.Config;
import javafx.beans.property.*;

public class GameState {

    private final List<SystemNode> systems;
    private final List<Packet> packets;
    private final List<Connection> connections;
    private final DoubleProperty remainingWireLength;
    private final IntegerProperty coins;
    private final IntegerProperty packetLoss;

    public GameState() {
        this.systems = new ArrayList<>();
        this.packets = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.remainingWireLength = new SimpleDoubleProperty(Config.INITIAL_WIRE_LENGTH);
        this.coins = new SimpleIntegerProperty(0);
        this.packetLoss = new SimpleIntegerProperty(0);
    }

    public void addSystem(SystemNode system) {
        if (system != null) {
            this.systems.add(system);
        }
    }

    public List<SystemNode> getSystems() {
        return Collections.unmodifiableList(systems);
    }

    public void clearSystems() {
        this.systems.clear();
    }

    public void addPacket(Packet packet) {
        if (packet != null) {
            this.packets.add(packet);
        }
    }

    public List<Packet> getPackets() {
        return Collections.unmodifiableList(packets);
    }

    public void clearPackets() {
        this.packets.clear();
    }

    public void removePacket(Packet packet) {
        if (packet != null) {
            this.packets.remove(packet);
        }
    }

    public void addConnection(Connection connection) {
        if (connection != null) {
            this.connections.add(connection);
        }
    }
    public List<Connection> getConnections() {
        return Collections.unmodifiableList(connections);
    }

    public void clearConnections() {
        this.connections.clear();
        this.remainingWireLength.set(Config.INITIAL_WIRE_LENGTH);
    }

    public double getRemainingWireLength() {
        return remainingWireLength.get();
    }

    public DoubleProperty remainingWireLengthProperty() {
        return remainingWireLength;
    }

    public boolean useWire(double length) {
        if (length <= this.remainingWireLength.get() && length >= 0) {
            this.remainingWireLength.set(this.remainingWireLength.get() - length);
            return true;
        }
        return false;
    }

    public void addCoin(int amount) {
        if (amount > 0) {
            this.coins.set(this.coins.get() + amount);
        }
    }
    public int getCoins() { return coins.get(); }
    public IntegerProperty coinsProperty() { return coins; }

    public void incrementPacketLoss(int amount) {
        if (amount > 0) {
            this.packetLoss.set(this.packetLoss.get() + amount);
        }
    }
    public int getPacketLoss() {return packetLoss.get(); }
    public IntegerProperty packetLossProperty() { return packetLoss; }
}