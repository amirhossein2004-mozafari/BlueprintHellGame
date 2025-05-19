package com.model;

import com.blueprinthell.Config;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GameState {

    private final List<SystemNode> systems;
    private final List<Packet> packets;
    private final List<Connection> connections;

    private final DoubleProperty remainingWireLength;
    private final IntegerProperty coins;
    private final IntegerProperty packetLossCount;
    private final IntegerProperty totalPacketsSpawned;
    private final DoubleProperty packetLossPercentage;
    private final DoubleProperty currentTimeScale;

    private final int packetsToSpawnInLevel;
    private final double levelDurationSeconds;

    private final SimpleIntegerProperty packetsSpawnedThisLevelProperty;
    private double gameTimeElapsedSeconds;
    private final SimpleDoubleProperty gameTimeElapsedSecondsProperty;

    private final Map<ShopSkill, DoubleProperty> activeSkillsDuration;
    private final BooleanProperty isGameLogicPaused;
    private final BooleanProperty isGameRunning;
    private final BooleanProperty allPortsConnectedProperty;
    private final IntegerProperty currentLevelNumberProperty;


    public GameState(int levelNumber, int packetsToSpawnInLevel, double levelDurationSeconds) {
        this.currentLevelNumberProperty = new SimpleIntegerProperty(levelNumber);
        this.packetsToSpawnInLevel = packetsToSpawnInLevel;
        this.levelDurationSeconds = levelDurationSeconds;

        this.systems = new ArrayList<>();
        this.packets = new ArrayList<>();
        this.connections = new ArrayList<>();

        this.remainingWireLength = new SimpleDoubleProperty(Config.INITIAL_WIRE_LENGTH);
        this.coins = new SimpleIntegerProperty(0);
        this.packetLossCount = new SimpleIntegerProperty(0);
        this.totalPacketsSpawned = new SimpleIntegerProperty(0);
        this.currentTimeScale = new SimpleDoubleProperty(Config.DEFAULT_TIME_SCALE);

        this.packetLossPercentage = new SimpleDoubleProperty(0.0);
        this.packetLossPercentage.bind(
                Bindings.when(this.totalPacketsSpawned.greaterThan(0))
                        .then(this.packetLossCount.multiply(100.0).divide(this.totalPacketsSpawned))
                        .otherwise(0.0)
        );

        this.packetsSpawnedThisLevelProperty = new SimpleIntegerProperty(0);
        this.gameTimeElapsedSeconds = 0.0;
        this.gameTimeElapsedSecondsProperty = new SimpleDoubleProperty(0.0);

        this.activeSkillsDuration = new EnumMap<>(ShopSkill.class);
        for (ShopSkill skill : ShopSkill.values()) {
            if (skill == ShopSkill.ATAR || skill == ShopSkill.AIRYAMAN) {
                activeSkillsDuration.put(skill, new SimpleDoubleProperty(0.0));
            }
        }

        this.isGameLogicPaused = new SimpleBooleanProperty(false);
        this.isGameRunning = new SimpleBooleanProperty(false);
        this.allPortsConnectedProperty = new SimpleBooleanProperty(false);


        System.out.println("GameState created for level: " + this.currentLevelNumberProperty.get() +
                " with packetsToSpawn: " + this.packetsToSpawnInLevel +
                ", duration: " + this.levelDurationSeconds);
    }

    public GameState() {
        this(1, 30, 90.0);
        System.out.println("GameState created with default parameters (via no-arg constructor).");
    }


    public void addSystem(SystemNode system) { if (system != null) this.systems.add(system); }
    public List<SystemNode> getSystems() { return Collections.unmodifiableList(systems); }
    public void clearSystems() {
        this.systems.clear();
        System.out.println("GameState: Systems cleared.");
    }

    public void addPacket(Packet packet) {
        if (packet != null && !this.packets.contains(packet)) {
            this.packets.add(packet);
            this.totalPacketsSpawned.set(this.totalPacketsSpawned.get() + 1);
            this.packetsSpawnedThisLevelProperty.set(this.packetsSpawnedThisLevelProperty.get() + 1);
        }
    }
    public void removePacket(Packet packet) { if (packet != null) this.packets.remove(packet); }
    public List<Packet> getPackets() { return Collections.unmodifiableList(packets); }

    public void clearPackets() {
        this.packets.clear();
        this.totalPacketsSpawned.set(0);
        this.packetLossCount.set(0);
        this.packetsSpawnedThisLevelProperty.set(0);
        this.gameTimeElapsedSeconds = 0.0;
        this.gameTimeElapsedSecondsProperty.set(0.0);
        System.out.println("GameState: Packets cleared and related stats reset.");
    }

    public void addConnection(Connection connection) { if (connection != null) this.connections.add(connection); }
    public List<Connection> getConnections() { return Collections.unmodifiableList(connections); }
    public void clearConnections() {
        this.connections.clear();
        this.remainingWireLength.set(Config.INITIAL_WIRE_LENGTH);
        this.updateAllPortsConnectedStatus(false);
        System.out.println("GameState: Connections cleared and wire length reset.");
    }

    public void incrementGameTime(double deltaTime) {
        this.gameTimeElapsedSeconds += deltaTime;
        this.gameTimeElapsedSecondsProperty.set(this.gameTimeElapsedSeconds);
    }

    public boolean canSpawnMorePackets() { return packetsSpawnedThisLevelProperty.get() < packetsToSpawnInLevel; }
    public boolean isLevelTimeUp() { return gameTimeElapsedSeconds >= levelDurationSeconds; }

    public double getGameTimeElapsedSeconds() { return gameTimeElapsedSeconds; }
    public SimpleDoubleProperty gameTimeElapsedSecondsProperty() { return gameTimeElapsedSecondsProperty; }

    public int getPacketsSpawnedThisLevel() { return packetsSpawnedThisLevelProperty.get(); }
    public SimpleIntegerProperty packetsSpawnedThisLevelProperty() { return packetsSpawnedThisLevelProperty; }

    public int getPacketsToSpawnInLevel() { return packetsToSpawnInLevel; }
    public double getLevelDurationSeconds() { return levelDurationSeconds; }


    public double getRemainingWireLength() { return remainingWireLength.get(); }
    public DoubleProperty remainingWireLengthProperty() { return remainingWireLength; }
    public boolean useWire(double length) {
        if (length >= 0 && length <= this.remainingWireLength.get()) {
            this.remainingWireLength.set(this.remainingWireLength.get() - length);
            return true;
        } return false;
    }

    public void addCoin(int amount) { if (amount > 0) this.coins.set(this.coins.get() + amount); }
    public int getCoins() { return coins.get(); }
    public IntegerProperty coinsProperty() { return coins; }

    public void incrementPacketLoss(int amount) {
        if (amount > 0) {
            this.packetLossCount.set(this.packetLossCount.get() + amount);
        }
    }
    public int getPacketLossCount() { return packetLossCount.get(); }
    public IntegerProperty packetLossProperty() { return packetLossCount; }
    public double getPacketLossPercentage() { return packetLossPercentage.get(); }
    public DoubleProperty packetLossPercentageProperty() { return packetLossPercentage; }
    public int getTotalPacketsSpawned() { return totalPacketsSpawned.get(); }
    public IntegerProperty totalPacketsSpawnedProperty() { return totalPacketsSpawned; }

    public double getCurrentTimeScale() { return currentTimeScale.get(); }
    public void setCurrentTimeScale(double scale) {
        double newScale = Math.max(Config.MIN_TIME_SCALE, Math.min(Config.MAX_TIME_SCALE, scale));
        this.currentTimeScale.set(newScale);
    }
    public DoubleProperty currentTimeScaleProperty() { return currentTimeScale; }

    public boolean isGameRunning() { return isGameRunning.get(); }
    public void setGameRunning(boolean running) { isGameRunning.set(running); }
    public BooleanProperty gameRunningProperty() { return isGameRunning; }

    public boolean areAllPortsConnected() { return allPortsConnectedProperty.get(); }
    public void updateAllPortsConnectedStatus(boolean status) { allPortsConnectedProperty.set(status); }
    public BooleanProperty allPortsConnectedProperty() { return allPortsConnectedProperty; }


    private final BooleanProperty atarSkillActiveProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty airyamanSkillActiveProperty = new SimpleBooleanProperty(false);

    public void activateSkill(ShopSkill skill, double durationSeconds) {
        if (skill == null) return;
        if (skill == ShopSkill.ANAHITA) {
            applyAnahitaEffect();
            System.out.println("Anahita skill activated! Noise cleared.");
            return;
        }
        DoubleProperty currentDurationProp = activeSkillsDuration.computeIfAbsent(skill, k -> new SimpleDoubleProperty(0.0));
        currentDurationProp.set(durationSeconds);
        if (skill == ShopSkill.ATAR) atarSkillActiveProperty.set(true);
        if (skill == ShopSkill.AIRYAMAN) airyamanSkillActiveProperty.set(true);
        System.out.println(skill.name() + " skill activated. Duration: " + currentDurationProp.get());
    }

    public boolean isSkillActive(ShopSkill skill) {
        if (skill == null || !activeSkillsDuration.containsKey(skill)) return false;
        return activeSkillsDuration.get(skill).get() > 0;
    }

    public void updateActiveSkills(double deltaTime) {
        if (deltaTime <= 0) return;
        Iterator<Map.Entry<ShopSkill, DoubleProperty>> iterator = activeSkillsDuration.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ShopSkill, DoubleProperty> entry = iterator.next();
            ShopSkill skill = entry.getKey();
            DoubleProperty durationProp = entry.getValue();
            if (durationProp.get() > 0) {
                double newDuration = durationProp.get() - deltaTime;
                if (newDuration <= 0) {
                    durationProp.set(0);
                    if (skill == ShopSkill.ATAR) atarSkillActiveProperty.set(false);
                    if (skill == ShopSkill.AIRYAMAN) airyamanSkillActiveProperty.set(false);
                    System.out.println(skill.name() + " skill expired.");
                } else {
                    durationProp.set(newDuration);
                }
            }
        }
    }

    private void applyAnahitaEffect() {
        for (Packet packet : new ArrayList<>(this.packets)) {
            if (packet != null) packet.setNoiseLevel(0.0);
        }
    }

    public boolean decreaseCoins(int amount) {
        if (amount > 0 && this.coins.get() >= amount) {
            this.coins.set(this.coins.get() - amount);
            return true;
        }
        return false;
    }

    public boolean isGameLogicPaused() { return isGameLogicPaused.get(); }
    public void setGameLogicPaused(boolean paused) { this.isGameLogicPaused.set(paused); }
    public BooleanProperty gameLogicPausedProperty() { return isGameLogicPaused; }

    public BooleanProperty atarSkillActiveProperty() { return atarSkillActiveProperty; }
    public BooleanProperty airyamanSkillActiveProperty() { return airyamanSkillActiveProperty; }

    public IntegerProperty currentLevelNumberProperty() { return currentLevelNumberProperty; }
    public int getCurrentLevelNumber() { return currentLevelNumberProperty.get(); }
}