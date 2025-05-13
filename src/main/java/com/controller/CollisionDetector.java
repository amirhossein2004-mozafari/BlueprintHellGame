package com.controller;

import com.model.GameState;
import com.model.Packet;
import com.model.Vector2D;
import com.model.ShopSkill;
import com.blueprinthell.Config;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollisionDetector {

    public static void checkPacketPacketCollisions(GameState gameState, List<Packet> activePackets) {
        if (gameState.isSkillActive(ShopSkill.AIRYAMAN)) {
            return;
        }

        if (activePackets == null || activePackets.size() < 2) return;
        List<Packet> packetsToCheck = new ArrayList<>(activePackets);

        for (int i = 0; i < packetsToCheck.size(); i++) {
            Packet packet1 = packetsToCheck.get(i);
            if (packet1.getVisualNode() == null || packet1.getCurrentSize() <= 0) continue;
            for (int j = i + 1; j < packetsToCheck.size(); j++) {
                Packet packet2 = packetsToCheck.get(j);
                if (packet2.getVisualNode() == null || packet2.getCurrentSize() <= 0) continue;

                Bounds bounds1 = packet1.getVisualNode().getBoundsInParent();
                Bounds bounds2 = packet2.getVisualNode().getBoundsInParent();

                if (bounds1.intersects(bounds2)) {
                    packet1.decreaseSize(1);
                    packet2.decreaseSize(1);

                    double collisionX = (packet1.getX() + packet2.getX()) / 2.0;
                    double collisionY = (packet1.getY() + packet2.getY()) / 2.0;
                    Vector2D collisionPoint = new Vector2D(collisionX, collisionY);
                    Set<Packet> alreadyProcessedForImpact = new HashSet<>();

                    for (Packet targetPacket : new ArrayList<>(gameState.getPackets())) {
                        if (targetPacket.getCurrentSize() <= 0 || alreadyProcessedForImpact.contains(targetPacket)) continue;
                        applyImpactAndNoise(targetPacket, collisionPoint, gameState, alreadyProcessedForImpact);
                    }
                }
            }
        }
    }

    private static void applyImpactAndNoise(Packet targetPacket, Vector2D collisionPoint, GameState gameState, Set<Packet> alreadyProcessed) {
        if (gameState.isSkillActive(ShopSkill.ATAR)) {
            return;
        }

        if (targetPacket.getVisualNode() == null || targetPacket.getCurrentSize() <= 0) return;
        Vector2D packetPosition = new Vector2D(targetPacket.getX(), targetPacket.getY());
        double distanceToCollision = packetPosition.distance(collisionPoint);

        if (distanceToCollision < Config.COLLISION_IMPACT_RADIUS && distanceToCollision > 0.001) {
            Vector2D forceDirection = Vector2D.subtract(packetPosition, collisionPoint).normalize();
            double forceMagnitude = Config.COLLISION_IMPACT_BASE_FORCE / (distanceToCollision + 1.0);
            forceMagnitude = Math.min(forceMagnitude, Config.COLLISION_IMPACT_BASE_FORCE);
            targetPacket.applyImpactForce(Vector2D.multiply(forceDirection, forceMagnitude));

            double noise = 1.0 - (distanceToCollision / Config.COLLISION_IMPACT_RADIUS);
            targetPacket.addNoise(Math.max(0, Math.min(1.0, noise)) * Config.PACKET_NOISE_TO_STABILITY_FACTOR);
            alreadyProcessed.add(targetPacket);
        }
    }
}