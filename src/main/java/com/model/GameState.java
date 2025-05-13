package com.model;

import com.blueprinthell.Config;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

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

    private double gameTimeElapsedSeconds = 0.0;
    private int packetsSpawnedThisLevel = 0;
    private final int packetsToSpawnInLevel;
    private final double levelDurationSeconds;

    private final SimpleIntegerProperty packetsSpawnedThisLevelProperty = new SimpleIntegerProperty(0);
    private final SimpleDoubleProperty gameTimeElapsedSecondsProperty = new SimpleDoubleProperty(0.0);

    private final Map<ShopSkill, DoubleProperty> activeSkillsDuration;
    private final BooleanProperty isGameLogicPaused;

    public GameState(int packetsToSpawnInLevel, double levelDurationSeconds) {
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
                Bindings.when(totalPacketsSpawned.greaterThan(0))
                        .then(packetLossCount.multiply(100.0).divide(totalPacketsSpawned))
                        .otherwise(0.0)
        );
        this.gameTimeElapsedSecondsProperty.set(0);
        this.packetsSpawnedThisLevelProperty.set(0);
        this.activeSkillsDuration = new EnumMap<>(ShopSkill.class);
        this.isGameLogicPaused = new SimpleBooleanProperty(false);
        this.packetsToSpawnInLevel = packetsToSpawnInLevel;
        this.levelDurationSeconds = levelDurationSeconds;
    }

    public GameState() {
        this(30, 90.0);
    }

    public void addSystem(SystemNode system) { if (system != null) this.systems.add(system); }
    public List<SystemNode> getSystems() { return Collections.unmodifiableList(systems); }
    public void clearSystems() { this.systems.clear(); }

    public void addPacket(Packet packet) {
        if (packet != null && !this.packets.contains(packet)) {
            this.packets.add(packet);
            this.totalPacketsSpawned.set(this.totalPacketsSpawned.get() + 1);
            this.packetsSpawnedThisLevel++;
            this.packetsSpawnedThisLevelProperty.set(this.packetsSpawnedThisLevel);
        }
    }
    public void removePacket(Packet packet) { if (packet != null) this.packets.remove(packet); }
    public List<Packet> getPackets() { return Collections.unmodifiableList(packets); }
    public void clearPackets() {
        this.packets.clear();
        this.totalPacketsSpawned.set(0);
        this.packetLossCount.set(0);
        this.packetsSpawnedThisLevel = 0;
        this.packetsSpawnedThisLevelProperty.set(0);
        this.gameTimeElapsedSeconds = 0.0;
        this.gameTimeElapsedSecondsProperty.set(0.0);
    }

    public void addConnection(Connection connection) { if (connection != null) this.connections.add(connection); }
    public List<Connection> getConnections() { return Collections.unmodifiableList(connections); }
    public void clearConnections() {
        this.connections.clear();
        this.remainingWireLength.set(Config.INITIAL_WIRE_LENGTH);
    }

    public void incrementGameTime(double deltaTime) {
        this.gameTimeElapsedSeconds += deltaTime;
        this.gameTimeElapsedSecondsProperty.set(this.gameTimeElapsedSeconds);
    }
    public boolean canSpawnMorePackets() { return packetsSpawnedThisLevel < packetsToSpawnInLevel; }
    public boolean isLevelTimeUp() { return gameTimeElapsedSeconds >= levelDurationSeconds; }
    public double getGameTimeElapsedSeconds() { return gameTimeElapsedSeconds; }
    public SimpleDoubleProperty gameTimeElapsedSecondsProperty() { return gameTimeElapsedSecondsProperty; }
    public int getPacketsSpawnedThisLevel() { return packetsSpawnedThisLevel; }
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

    public void activateSkill(ShopSkill skill, double durationSeconds) {
        if (skill == null) return;
        if (skill == ShopSkill.ANAHITA) {
            applyAnahitaEffect();
            System.out.println("Anahita skill activated! Noise cleared.");
            return;
        }
        DoubleProperty currentDurationProp = activeSkillsDuration.computeIfAbsent(skill, k -> new SimpleDoubleProperty(0));
        currentDurationProp.set(currentDurationProp.get() + durationSeconds);
        System.out.println(skill.name() + " skill activated/extended. Duration: " + currentDurationProp.get());
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
            DoubleProperty durationProp = entry.getValue();
            double newDuration = durationProp.get() - deltaTime;
            if (newDuration <= 0) {
                durationProp.set(0);
                System.out.println(entry.getKey().name() + " skill expired.");
            } else {
                durationProp.set(newDuration);
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

    public void setGameLogicPaused(boolean paused) {
        this.isGameLogicPaused.set(paused);
    }

    public BooleanProperty gameLogicPausedProperty() { return isGameLogicPaused; }
}