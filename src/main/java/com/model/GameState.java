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
import com.api.enums.AbilityType;

public class GameState {

    private final List<SystemNode> systems;
    private final List<Packet> packets;
    private final List<Connection> connections;

    private final DoubleProperty remainingWireLength;
    private final IntegerProperty coins;
    private final IntegerProperty totalPacketsSuccessfullySpawnedCount;
    private final IntegerProperty totalInitialSizeOfSpawnedPackets;
    private final IntegerProperty totalSizeLost;
    private final DoubleProperty packetLossPercentageBySize;
    private final DoubleProperty currentTimeScale;

    private final int packetsToSpawnInLevel;
    private final double levelDurationSeconds;

    private final SimpleIntegerProperty packetsSpawnedThisLevelProperty;
    private double gameTimeElapsedSeconds;
    private final SimpleDoubleProperty gameTimeElapsedSecondsProperty;

    private final Map<AbilityType, DoubleProperty> activeSkillsDuration;
    private final BooleanProperty atarSkillActiveProperty;
    private final BooleanProperty airyamanSkillActiveProperty;

    private final BooleanProperty isGameLogicPaused;
    private final BooleanProperty isGameRunning;
    private final BooleanProperty allPortsConnectedProperty;
    private final IntegerProperty currentLevelNumberProperty;

    private final int packetsToSpawnInLevel_value;
    private final ReadOnlyIntegerWrapper packetsToSpawnInLevelPropertyWrapper;

    private final List<EffectPoint> effectPoints = new ArrayList<>();

    public GameState(int levelNumber, int packetsToSpawnInLevel, double levelDurationSeconds) {
        this.packetsToSpawnInLevel_value = packetsToSpawnInLevel;
        this.packetsToSpawnInLevelPropertyWrapper = new ReadOnlyIntegerWrapper(packetsToSpawnInLevel);
        this.currentLevelNumberProperty = new SimpleIntegerProperty(levelNumber);
        this.packetsToSpawnInLevel = packetsToSpawnInLevel;
        this.levelDurationSeconds = levelDurationSeconds;

        this.systems = new ArrayList<>();
        this.packets = new ArrayList<>();
        this.connections = new ArrayList<>();

        this.remainingWireLength = new SimpleDoubleProperty(Config.INITIAL_WIRE_LENGTH);
        this.coins = new SimpleIntegerProperty(0);
        this.totalPacketsSuccessfullySpawnedCount = new SimpleIntegerProperty(0);
        this.totalInitialSizeOfSpawnedPackets = new SimpleIntegerProperty(0);
        this.totalSizeLost = new SimpleIntegerProperty(0);
        this.currentTimeScale = new SimpleDoubleProperty(Config.DEFAULT_TIME_SCALE);

        this.packetLossPercentageBySize = new SimpleDoubleProperty(0.0);
        this.packetLossPercentageBySize.bind(
                Bindings.when(this.totalInitialSizeOfSpawnedPackets.greaterThan(0))
                        .then(this.totalSizeLost.multiply(100.0).divide(this.totalInitialSizeOfSpawnedPackets))
                        .otherwise(0.0)
        );

        this.packetsSpawnedThisLevelProperty = new SimpleIntegerProperty(0);
        this.gameTimeElapsedSeconds = 0.0;
        this.gameTimeElapsedSecondsProperty = new SimpleDoubleProperty(0.0);

        this.activeSkillsDuration = new EnumMap<>(AbilityType.class);
        this.atarSkillActiveProperty = new SimpleBooleanProperty(false);
        this.airyamanSkillActiveProperty = new SimpleBooleanProperty(false);
        for (AbilityType skill : AbilityType.values()) {
            if (skill == AbilityType.ATAR || skill == AbilityType.AIRYAMAN) {
                activeSkillsDuration.put(skill, new SimpleDoubleProperty(0.0));
            }
        }

        this.isGameLogicPaused = new SimpleBooleanProperty(false);
        this.isGameRunning = new SimpleBooleanProperty(false);
        this.allPortsConnectedProperty = new SimpleBooleanProperty(false);
    }

    public GameState() {
        this(1,50,180);
    }

    public void addSystem(SystemNode system) { if (system != null) this.systems.add(system); }
    public List<SystemNode> getSystems() { return Collections.unmodifiableList(systems); }
    public void clearSystems() { this.systems.clear(); }

    public void addPacket(Packet packet) {
        if (packet != null && !this.packets.contains(packet)) {
            this.packets.add(packet);
        }
    }

    public boolean removePacket(Packet packet) {
        if (packet != null && this.packets != null) {
            return this.packets.remove(packet);
        }
        return false;
    }
    private String getPacketIdSafeForGameState(Packet packet) {
        if (packet == null) return "[NULL_PACKET_IN_GS]";
        String id = packet.getId().toString();
        if (id == null || id.isEmpty()) return "[NO_ID_IN_GS]";
        return (id.length() < 4) ? id : id.substring(0, 4);
    }

    public List<Packet> getPackets() {
        if (this.packets == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.packets);
    }
    public void clearPackets() {
        this.packets.clear();
        this.packetsSpawnedThisLevelProperty.set(0);
        this.gameTimeElapsedSeconds = 0.0;
        this.gameTimeElapsedSecondsProperty.set(0.0);

        this.totalPacketsSuccessfullySpawnedCount.set(0);
        this.totalInitialSizeOfSpawnedPackets.set(0);
        this.totalSizeLost.set(0);
    }

    public void addConnection(Connection connection) { if (connection != null) this.connections.add(connection); }
    public List<Connection> getConnections() { return Collections.unmodifiableList(connections); }

    public void clearConnections() {
        this.connections.clear();
        this.remainingWireLength.set(Config.INITIAL_WIRE_LENGTH);
        this.updateAllPortsConnectedStatus(false);
    }

    public void incrementGameTime(double deltaTime) {
        this.gameTimeElapsedSeconds += deltaTime;
        this.gameTimeElapsedSecondsProperty.set(this.gameTimeElapsedSeconds);
    }

    public void registerSpawnedPacket(Packet packet) {
        if (packet == null) return;
        this.totalPacketsSuccessfullySpawnedCount.set(this.totalPacketsSuccessfullySpawnedCount.get() + 1);
        this.totalInitialSizeOfSpawnedPackets.set(this.totalInitialSizeOfSpawnedPackets.get() + packet.getInitialSize());
    }

    public void incrementTotalSizeLost(int sizeLostAmount) {
        if (sizeLostAmount > 0) {
            this.totalSizeLost.set(this.totalSizeLost.get() + sizeLostAmount);
        }
    }

    public void registerCompletelyLostPacket(Packet packet) {
        if (packet != null && packet.getCurrentSize() > 0) {
            incrementTotalSizeLost(packet.getCurrentSize());
        }
    }

    public boolean canSpawnMorePackets() {

        boolean canSpawn = getTotalPacketsSuccessfullySpawnedCount() < getPacketsToSpawnInLevel();
        return canSpawn;
    }

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

    public int getTotalPacketsSuccessfullySpawnedCount() { return totalPacketsSuccessfullySpawnedCount.get(); }
    public IntegerProperty totalPacketsSuccessfullySpawnedCountProperty() { return totalPacketsSuccessfullySpawnedCount; }
    public int getTotalInitialSizeOfSpawnedPackets() { return totalInitialSizeOfSpawnedPackets.get(); }
    public IntegerProperty totalInitialSizeOfSpawnedPacketsProperty() { return totalInitialSizeOfSpawnedPackets; }
    public int getTotalSizeLost() { return totalSizeLost.get(); }
    public IntegerProperty totalSizeLostProperty() { return totalSizeLost; }
    public double getPacketLossPercentageBySize() { return packetLossPercentageBySize.get(); }
    public DoubleProperty packetLossPercentageBySizeProperty() { return packetLossPercentageBySize; }

    public double getCurrentTimeScale() { return currentTimeScale.get(); }
    public void setCurrentTimeScale(double scale) {
        this.currentTimeScale.set(Math.max(Config.MIN_TIME_SCALE, Math.min(Config.MAX_TIME_SCALE, scale)));
    }
    public DoubleProperty currentTimeScaleProperty() { return currentTimeScale; }

    public boolean isGameRunning() { return isGameRunning.get(); }
    public void setGameRunning(boolean running) { isGameRunning.set(running); }
    public BooleanProperty gameRunningProperty() { return isGameRunning; }

    public boolean areAllPortsConnected() { return allPortsConnectedProperty.get(); }
    public void updateAllPortsConnectedStatus(boolean status) { allPortsConnectedProperty.set(status); }
    public BooleanProperty allPortsConnectedProperty() { return allPortsConnectedProperty; }

    public void activateSkill(AbilityType ability, double durationSeconds) {
        if (ability == null) return;
        if (ability == AbilityType.ANAHITA) {
            applyAnahitaEffect();
            return;
        }
        DoubleProperty currentDurationProp = activeSkillsDuration.computeIfAbsent(ability, k -> new SimpleDoubleProperty(0.0));
        currentDurationProp.set(durationSeconds);
        if (ability == AbilityType.ATAR) atarSkillActiveProperty.set(true);
        if (ability == AbilityType.AIRYAMAN) airyamanSkillActiveProperty.set(true);
    }

    public boolean isSkillActive(AbilityType ability) {
        if (ability == null || !activeSkillsDuration.containsKey(ability)) return false;
        return activeSkillsDuration.get(ability).get() > 0;
    }

    public void updateActiveSkills(double deltaTime) {
        if (deltaTime <= 0) return;
        Iterator<Map.Entry<AbilityType, DoubleProperty>> iterator = activeSkillsDuration.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<AbilityType, DoubleProperty> entry = iterator.next();
            AbilityType ability = entry.getKey();
            DoubleProperty durationProp = entry.getValue();
            if (durationProp.get() > 0) {
                double newDuration = durationProp.get() - deltaTime;
                if (newDuration <= 0) {
                    durationProp.set(0);
                    if (ability == AbilityType.ATAR) atarSkillActiveProperty.set(false);
                    if (ability == AbilityType.AIRYAMAN) airyamanSkillActiveProperty.set(false);
                } else {
                    durationProp.set(newDuration);
                }
            }
        }
    }

    public List<SystemNode> getSystemsByRole(SystemRole role) {
        List<SystemNode> result = new ArrayList<>();
        for (SystemNode system : this.systems) {
            if (system.getRole() == role) {
                result.add(system);
            }
        }
        return result;
    }


    public boolean removeConnection(Connection connection) {
        if (connection != null) {

            return this.connections.remove(connection);
        }
        return false;
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

    public List<EffectPoint> getEffectPoints() {
        return effectPoints;
    }
    public void addEffectPoint(EffectPoint effectPoint) {
        if (effectPoint != null) {
            this.effectPoints.add(effectPoint);
        }
    }

    public void setGameTimeElapsed(double time) {
        this.gameTimeElapsedSeconds = time;
    }

    public ReadOnlyIntegerProperty packetsToSpawnInLevelProperty() { return packetsToSpawnInLevelPropertyWrapper.getReadOnlyProperty(); }

    public boolean isGameLogicPaused() { return isGameLogicPaused.get(); }
    public void setGameLogicPaused(boolean paused) { this.isGameLogicPaused.set(paused); }
    public BooleanProperty gameLogicPausedProperty() { return isGameLogicPaused; }

    public BooleanProperty atarSkillActiveProperty() { return atarSkillActiveProperty; }
    public BooleanProperty airyamanSkillActiveProperty() { return airyamanSkillActiveProperty; }

    public IntegerProperty currentLevelNumberProperty() { return currentLevelNumberProperty; }
    public int getCurrentLevelNumber() { return currentLevelNumberProperty.get(); }
}