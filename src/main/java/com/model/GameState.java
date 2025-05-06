package com.model;

import com.blueprinthell.Config;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameState {

    private final List<SystemNode> systems;
    private final List<Packet> packets;
    private final List<Connection> connections;

    private final DoubleProperty remainingWireLength;
    private final IntegerProperty coins;
    private final IntegerProperty packetLossCount;
    private final IntegerProperty totalPacketsSpawned;
    private final DoubleProperty packetLossPercentage;

    public GameState() {
        this.systems = new ArrayList<>();
        this.packets = new ArrayList<>();
        this.connections = new ArrayList<>();

        this.remainingWireLength = new SimpleDoubleProperty(Config.INITIAL_WIRE_LENGTH);
        this.coins = new SimpleIntegerProperty(0);
        this.packetLossCount = new SimpleIntegerProperty(0);
        this.totalPacketsSpawned = new SimpleIntegerProperty(0);

        this.packetLossPercentage = new SimpleDoubleProperty(0.0);
        this.packetLossPercentage.bind(
                Bindings.when(totalPacketsSpawned.greaterThan(0))
                        .then(packetLossCount.multiply(100.0).divide(totalPacketsSpawned))
                        .otherwise(0.0)
        );
    }

    public void addSystem(SystemNode system) {
        if (system != null) this.systems.add(system);
    }
    public List<SystemNode> getSystems() {
        return Collections.unmodifiableList(systems);
    }
    public void clearSystems() {
        this.systems.clear();
    }

    public void addPacket(Packet packet) {
        if (packet != null && !this.packets.contains(packet)) {
            this.packets.add(packet);
            this.totalPacketsSpawned.set(this.totalPacketsSpawned.get() + 1);
        }
    }
    public List<Packet> getPackets() {
        return Collections.unmodifiableList(packets);
    }
    public void clearPackets() {
        this.packets.clear();
        this.totalPacketsSpawned.set(0);
        this.packetLossCount.set(0);
    }
    public void removePacket(Packet packet) {
        if (packet != null) this.packets.remove(packet);
    }

    public void addConnection(Connection connection) {
        if (connection != null) this.connections.add(connection);
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
        if (length >= 0 && length <= this.remainingWireLength.get()) {
            this.remainingWireLength.set(this.remainingWireLength.get() - length);
            return true;
        }
        return false;
    }

    public void addCoin(int amount) {
        if (amount > 0) this.coins.set(this.coins.get() + amount);
    }
    public int getCoins() {
        return coins.get();
    }
    public IntegerProperty coinsProperty() {
        return coins;
    }

    public void incrementPacketLoss(int amount) {
        if (amount > 0) {
            this.packetLossCount.set(this.packetLossCount.get() + amount);
            System.out.println("Packet Lost! Total Loss: " + this.packetLossCount.get());
        }
    }
    public int getPacketLossCount() {
        return packetLossCount.get();
    }
    public IntegerProperty packetLossProperty() {
        return packetLossCount;
    }
    public double getPacketLossPercentage() {
        return packetLossPercentage.get();
    }
    public DoubleProperty packetLossPercentageProperty() {
        return packetLossPercentage;
    }
    public int getTotalPacketsSpawned() {
        return totalPacketsSpawned.get();
    }
    public IntegerProperty totalPacketsSpawnedProperty() {
        return totalPacketsSpawned;
    }

}