package com.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.blueprinthell.Config;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class GameState {

    private final List<SystemNode> systems;
    private final List<Packet> packets;
    private final List<Connection> connections;
    private final DoubleProperty remainingWireLength;
    public GameState() {
        this.systems = new ArrayList<>();
        this.packets = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.remainingWireLength = new SimpleDoubleProperty(Config.INITIAL_WIRE_LENGTH);
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
}