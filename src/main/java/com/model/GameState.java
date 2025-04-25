package com.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameState {

    private final List<SystemNode> systems;

    public GameState() {
        this.systems = new ArrayList<>();
    }

    public void addSystem(SystemNode system) {
        if (system != null) {
            this.systems.add(system);
        }
    }

    public List<SystemNode> getSystems() {
        return Collections.unmodifiableList(systems);
    }

}