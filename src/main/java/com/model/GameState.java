package com.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameState {

    private final List<SystemNode> systems;
    private final List<Packet> packets;

    public GameState() {
        this.systems = new ArrayList<>();
        this.packets = new ArrayList<>();
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
}