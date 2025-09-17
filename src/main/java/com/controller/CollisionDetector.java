package com.controller;

import com.model.GameState;
import com.model.Packet;
import com.model.Vector2D;
import com.model.ShopSkill;
import com.blueprinthell.Config;
import com.utils.AudioManager;
import javafx.geometry.Bounds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollisionDetector {

    public static void checkPacketPacketCollisions(GameState gameState, List<Packet> activePackets) {
        if (gameState == null || activePackets == null || activePackets.size() < 2) return;
        if (gameState.isSkillActive(ShopSkill.AIRYAMAN)) return;

        List<Packet> packetsToCheck = new ArrayList<>(activePackets);

        for (int i = 0; i < packetsToCheck.size(); i++) {
            Packet packet1 = packetsToCheck.get(i);
            if (packet1.getVisualNode() == null || packet1.getCurrentSize() <= 0) continue;

            for (int j = i + 1; j < packetsToCheck.size(); j++) {
                Packet packet2 = packetsToCheck.get(j);
                if (packet2.getVisualNode() == null || packet2.getCurrentSize() <= 0) continue;

                if (packet1.isOnCollisionCooldownWith(packet2) || packet2.isOnCollisionCooldownWith(packet1)) {
                    continue;
                }

                Bounds bounds1 = packet1.getVisualNode().getBoundsInParent();
                Bounds bounds2 = packet2.getVisualNode().getBoundsInParent();

                if (bounds1.intersects(bounds2)) {
                    System.out.println("CollisionDetector: Actual Collision! P1(" + packet1.getType() + ",S" + packet1.getCurrentSize() +
                            ") vs P2(" + packet2.getType() + ",S" + packet2.getCurrentSize() + ")");

                    packet1.registerCollisionWith(packet2);

                    int sizeReduction = Config.PACKET_COLLISION_SIZE_REDUCTION;
                    int p1InitialSize = packet1.getCurrentSize();
                    int p2InitialSize = packet2.getCurrentSize();
                    packet1.decreaseSize(sizeReduction);
                    packet2.decreaseSize(sizeReduction);
                    int p1Lost = p1InitialSize - packet1.getCurrentSize();
                    int p2Lost = p2InitialSize - packet2.getCurrentSize();
                    if (p1Lost > 0) gameState.incrementTotalSizeLost(p1Lost);
                    if (p2Lost > 0) gameState.incrementTotalSizeLost(p2Lost);
                    AudioManager.playSoundEffect(AudioManager.SFX_PACKET_DAMAGE);

                    Vector2D p1Pos = new Vector2D(packet1.getX(), packet1.getY());
                    Vector2D p2Pos = new Vector2D(packet2.getX(), packet2.getY());
                    Vector2D sepForceDirP1 = Vector2D.subtract(p1Pos, p2Pos).normalize();
                    Vector2D sepForceDirP2 = Vector2D.subtract(p2Pos, p1Pos).normalize();
                    double sepForceMag = Config.PACKET_COLLISION_SEPARATION_FORCE;
                    if (sepForceDirP1.magnitudeSquared() > 0.001) packet1.applyImpactForce(Vector2D.multiply(sepForceDirP1, sepForceMag));
                    if (sepForceDirP2.magnitudeSquared() > 0.001) packet2.applyImpactForce(Vector2D.multiply(sepForceDirP2, sepForceMag));

                    packet1.handleCollision();
                    packet2.handleCollision();

                    if (!gameState.isSkillActive(ShopSkill.ATAR)) {
                        double colX = (packet1.getX() + packet2.getX()) / 2.0;
                        double colY = (packet1.getY() + packet2.getY()) / 2.0;
                        Vector2D colPoint = new Vector2D(colX, colY);
                        Set<Packet> processedWave = new HashSet<>();
                        for (Packet target : new ArrayList<>(gameState.getPackets())) {
                            if (target == packet1 || target == packet2 || target.getCurrentSize() <= 0 || processedWave.contains(target)) continue;
                            applyImpactAndNoiseToNearbyPacket(target, colPoint, gameState, processedWave);
                        }
                    }
                }
            }
        }
    }

    private static void applyImpactAndNoiseToNearbyPacket(Packet targetPacket, Vector2D collisionPoint, GameState gameState, Set<Packet> alreadyProcessed) {
        if (targetPacket == null || targetPacket.getVisualNode() == null || targetPacket.getCurrentSize() <= 0 || collisionPoint == null || gameState == null || alreadyProcessed == null) return;
        Vector2D packetPosition = new Vector2D(targetPacket.getX(), targetPacket.getY());
        double distanceToCollision = packetPosition.distance(collisionPoint);
        if (distanceToCollision < Config.COLLISION_IMPACT_RADIUS && distanceToCollision > Config.MIN_IMPACT_DISTANCE_THRESHOLD) {
            Vector2D forceDirection = Vector2D.subtract(packetPosition, collisionPoint).normalize();
            double normalizedDistanceFactor = distanceToCollision / Config.COLLISION_IMPACT_RADIUS;
            double forceStrengthFactor = 1.0 - normalizedDistanceFactor;
            double forceMagnitude = Config.COLLISION_IMPACT_BASE_FORCE * forceStrengthFactor;
            forceMagnitude = Math.max(0, Math.min(Config.COLLISION_IMPACT_BASE_FORCE, forceMagnitude));
            if (forceDirection.magnitudeSquared() > 0.001) targetPacket.applyImpactForce(Vector2D.multiply(forceDirection, forceMagnitude));
            double noiseToAdd = forceStrengthFactor * Config.PACKET_WAVE_NOISE_AMOUNT;
            noiseToAdd = Math.max(0, Math.min(Config.PACKET_WAVE_NOISE_AMOUNT, noiseToAdd));
            targetPacket.addNoise(noiseToAdd);
            alreadyProcessed.add(targetPacket);
        }
    }
}