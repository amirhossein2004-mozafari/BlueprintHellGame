package com.model;

import com.blueprinthell.Config;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.geometry.Point2D;

import java.util.*;
import java.util.List;

import javafx.scene.paint.Color;


public class Packet {

    private final PacketType type;
    private double x;
    private double y;
    private Vector2D velocity;
    private double acceleration;
    private Connection currentConnection;
    private Port targetPort;
    private Port startPort;
    private double progressOnConnection;
    private transient Node visualNode;
    private final int initialSize;
    private int currentSize;
    private transient Label sizeLabel;
    private double noiseLevel;
    private double stabilityOnWire;
    private boolean isOffWire;
    private UUID id;
    private Map<UUID, Double> collisionCooldownWithPackets;
    private double currentMaxSpeedOnWire;
    private double inheritedStabilityDamage = 0.0;
    private double timeSpentIdleOffWireSeconds = 0.0;

    private int currentSegmentIndex;
    private String parentLargePacketId = null;
    private int totalBitsForParent = 0;
    private transient Color visualColor = null;
    private boolean isReturningToSource = false;
    private Port originalSourcePort = null;
    private transient SystemNode lastVisitedSystem;
    private PacketType originalType = null;
    private PacketType emulatedMovementType = null;
    private int playerId;


    public Packet(PacketType type, double initialX, double initialY) {
        this.id = UUID.randomUUID();
        this.collisionCooldownWithPackets = new HashMap<>();
        this.type = type;
        this.x = initialX;
        this.y = initialY;
        this.velocity = new Vector2D();
        this.originalType = null;

        if (type == PacketType.SQUARE) {
            this.initialSize = Config.PACKET_SQUARE_INITIAL_SIZE;
        } else if (type == PacketType.BIT_PACKET) {
            this.initialSize = 1;
        }
        else if (type == PacketType.LARGE_8) {
            this.initialSize = 8;
        } else if (type == PacketType.LARGE_10) {
            this.initialSize = 10;
        } else if (type == PacketType.MESSENGER_INFINITY) {
            this.initialSize = Config.PACKET_MESSENGER_INITIAL_SIZE;
        } else if (type == PacketType.CONFIDENTIAL_NORMAL) {
            this.initialSize = Config.PACKET_CONFIDENTIAL_NORMAL_INITIAL_SIZE;
        }
        else if (type == PacketType.CONFIDENTIAL_DISTANCING) {
            this.initialSize = Config.PACKET_CONFIDENTIAL_DISTANCING_INITIAL_SIZE;
        }
        else if (type == PacketType.PROTECTED) {
            this.initialSize = 0;
        }
        else {
            this.initialSize = Config.PACKET_TRIANGLE_INITIAL_SIZE;
        }

        this.currentSize = this.initialSize;

        this.sizeLabel = new Label(String.valueOf(this.currentSize));
        this.sizeLabel.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 9));
        this.sizeLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        this.sizeLabel.setMouseTransparent(true);
        this.currentMaxSpeedOnWire = Config.BASE_PACKET_SPEED;
        this.stabilityOnWire = 1.0;
        this.isOffWire = false;
        this.timeSpentIdleOffWireSeconds = 0.0;
        this.inheritedStabilityDamage = 0.0;
        this.noiseLevel = 0.0;
        this.acceleration = 0.0;
        this.progressOnConnection = 0.0;
    }

    public void setInheritedStabilityDamage(double damage) {
        this.inheritedStabilityDamage = Math.max(0, Math.min(1.0, damage));
    }

    public double getInheritedStabilityDamage() {
        return this.inheritedStabilityDamage;
    }



    public void startMoving(Connection connection) {
        if (connection == null || connection.getStartPort() == null || connection.getEndPort() == null) {
            this.isOffWire = true;
            this.velocity.set(Config.INITIAL_OFF_WIRE_VELOCITY_X, Config.INITIAL_OFF_WIRE_VELOCITY_Y);
            return;
        }

        this.currentConnection = connection;
        this.startPort = connection.getStartPort();
        this.targetPort = connection.getEndPort();
        this.progressOnConnection = 0.0;
        this.acceleration = 0.0;
        this.isOffWire = false;
        this.timeSpentIdleOffWireSeconds = 0.0;
        this.stabilityOnWire = 1.0 - this.inheritedStabilityDamage;
        this.inheritedStabilityDamage = 0.0;
        this.currentSegmentIndex = 0;

        if (this.type == PacketType.PROTECTED) {
            PacketType[] possibleMovements = {PacketType.SQUARE, PacketType.TRIANGLE, PacketType.MESSENGER_INFINITY};
            this.emulatedMovementType = possibleMovements[new Random().nextInt(possibleMovements.length)];
            System.out.println("Protected Packet " + id.toString().substring(0, 4) + " is now emulating movement of: " + this.emulatedMovementType);        } else {
            this.emulatedMovementType = this.type;
        }


        PacketType movementType = this.emulatedMovementType;

        List<Point2D> path = connection.getFullPath();
        if (path.size() < 2) {
            this.isOffWire = true;
            return;
        }
        Point2D startPoint = path.get(0);
        Point2D nextPointInPath = path.get(1);

        this.setX(startPoint.getX());
        this.setY(startPoint.getY());

        boolean compatibleStart = (this.type == startPort.getPacketType() || this.type == PacketType.PROTECTED);

        double initialSpeedFactor;

        if (movementType == PacketType.SQUARE) {
            this.currentMaxSpeedOnWire = Config.BASE_PACKET_SPEED;
            this.acceleration = 0;
            initialSpeedFactor = compatibleStart ? Config.SQUARE_COMPATIBLE_SPEED_FACTOR : Config.SQUARE_INCOMPATIBLE_SPEED_FACTOR;
        } else if (movementType == PacketType.TRIANGLE) {
            this.currentMaxSpeedOnWire = Config.BASE_PACKET_SPEED * (compatibleStart ? Config.TRIANGLE_COMPATIBLE_SPEED_FACTOR : Config.TRIANGLE_INCOMPATIBLE_MAX_SPEED_FACTOR);
            this.acceleration = compatibleStart ? 0 : Config.TRIANGLE_PACKET_ACCELERATION;
            initialSpeedFactor = compatibleStart ? Config.TRIANGLE_COMPATIBLE_INITIAL_SPEED_FACTOR : Config.TRIANGLE_INCOMPATIBLE_INITIAL_SPEED_FACTOR;
        } else if (movementType == PacketType.MESSENGER_INFINITY) {
            this.acceleration = Config.TRIANGLE_PACKET_ACCELERATION;
            this.currentMaxSpeedOnWire = Config.BASE_PACKET_SPEED * 1.2;
            initialSpeedFactor = 0.5;
            this.isReturningToSource = false;
            this.originalSourcePort = this.startPort;
        } else {
            this.currentMaxSpeedOnWire = Config.BASE_PACKET_SPEED;
            this.acceleration = 0;
            initialSpeedFactor = 1.0;
        }


        double initialSpeed = this.currentMaxSpeedOnWire * initialSpeedFactor;
        Vector2D direction = Vector2D.direction(
                new Vector2D(startPoint.getX(), startPoint.getY()),
                new Vector2D(nextPointInPath.getX(), nextPointInPath.getY())
        );
        this.velocity.set(Vector2D.multiply(direction, initialSpeed));

        if (this.sizeLabel != null) {
            updateLabelPosition();
        }
    }


    public boolean update(double deltaTime, GameState gameState) {
        if (deltaTime <= 0) return false;

        double effectiveDeltaTime = deltaTime;

        if (this.type == PacketType.CONFIDENTIAL_NORMAL && !isOffWire) {
            Port target = this.getTargetPort();
            if (target != null && target.getParentSystem() != null) {
                if (target.getParentSystem().getStoredPacketCount() > 0) {
                    effectiveDeltaTime *= Config.CONFIDENTIAL_PACKET_SLOWDOWN_FACTOR;
                }
            }
        }
        else if (this.type == PacketType.CONFIDENTIAL_DISTANCING && !isOffWire) {
            Vector2D totalForce = new Vector2D(0, 0);
            for (Packet other : gameState.getPackets()) {
                if (other == this || other.isOffWire()) continue;

                double distance = this.getPosition().distance(other.getPosition());

                if (distance < 1.0) continue;

                if (distance < Config.CONFIDENTIAL_PACKET_IDEAL_DISTANCE * 2) {
                    double displacement = distance - Config.CONFIDENTIAL_PACKET_IDEAL_DISTANCE;

                    double forceMagnitude = -displacement * Config.CONFIDENTIAL_PACKET_FORCE_STRENGTH * 0.05;

                    if (Math.abs(forceMagnitude) > 5.0) {
                        forceMagnitude = 5.0 * Math.signum(forceMagnitude);
                    }

                    Vector2D direction = Vector2D.direction(other.getPosition(), this.getPosition());
                    totalForce.add(Vector2D.multiply(direction, forceMagnitude));
                }
            }
            this.velocity.add(Vector2D.multiply(totalForce, effectiveDeltaTime));
        }


        if (isOffWire) {
            this.x += velocity.getX() * deltaTime;
            this.y += velocity.getY() * deltaTime;
            velocity.multiply(Config.OFF_WIRE_FRICTION);
            if (velocity.magnitudeSquared() < Config.MIN_VELOCITY_SQUARED_TO_STOP_OFF_WIRE) {
                velocity.set(0, 0);
                timeSpentIdleOffWireSeconds += deltaTime;
            } else {
                timeSpentIdleOffWireSeconds = 0.0;
            }
            updateLabelPosition();
            return false;
        }

        if (currentConnection == null) {
            isOffWire = true;
            return false;
        }

        final Vector2D packetPos = new Vector2D(this.x, this.y);
        final List<Point2D> path = currentConnection.getFullPath();

        if (currentSegmentIndex >= path.size() - 1) return true;

        Point2D segmentStartPoint = path.get(currentSegmentIndex);
        Point2D segmentEndPoint = path.get(currentSegmentIndex + 1);
        Vector2D targetPos = new Vector2D(segmentEndPoint.getX(), segmentEndPoint.getY());

        double distanceToTarget = packetPos.distance(targetPos);
        double travelDistanceThisFrame = velocity.magnitude() * deltaTime;

        if (distanceToTarget <= travelDistanceThisFrame * 1.05) {
            this.setX(targetPos.getX());
            this.setY(targetPos.getY());
            currentSegmentIndex++;

            if (currentSegmentIndex >= path.size() - 1) {
                if (this.isReturningToSource && this.type == PacketType.MESSENGER_INFINITY) {
                    System.out.println("Messenger packet has returned to source. Re-launching towards original destination.");

                    Port temp = this.startPort;
                    this.startPort = this.targetPort;
                    this.targetPort = temp;
                    this.isReturningToSource = false;
                    this.currentSegmentIndex = 0;

                    this.velocity.multiply(-1);

                    return false;
                }

                if (this.isLargePacket()) {
                    this.currentConnection.registerLargePacketPassage();
                    System.out.println("LARGE packet passed wire. Passages: " + this.currentConnection.getLargePacketPassages());
                }
                this.velocity.set(0, 0);
                return true;
            }

            else {
                Point2D nextTargetPoint = path.get(currentSegmentIndex + 1);
                Vector2D newDirection = Vector2D.direction(targetPos, new Vector2D(nextTargetPoint.getX(), nextTargetPoint.getY()));
                this.velocity.set(Vector2D.multiply(newDirection, this.velocity.magnitude()));
            }
        }

        if (this.type == PacketType.TRIANGLE && this.acceleration > 0 && stabilityOnWire > Config.MIN_STABILITY_FOR_ACCELERATION) {

            packetPos.set(this.x, this.y);

            boolean hasAcceleration = true;
            for (EffectPoint effect : gameState.getEffectPoints()) {
                if (effect.getType() == EffectType.AERGIA_NO_ACCELERATION) {
                    if (packetPos.distance(new Vector2D(effect.getPosition().getX(), effect.getPosition().getY())) < 10.0) {
                        hasAcceleration = false;
                        break;
                    }
                }
            }

            if (hasAcceleration && velocity.magnitude() < this.currentMaxSpeedOnWire) {
                Vector2D directionToTarget = Vector2D.direction(packetPos, targetPos);
                Vector2D accelVec = Vector2D.multiply(directionToTarget, this.acceleration * deltaTime);
                if (Vector2D.add(new Vector2D(this.velocity), accelVec).magnitudeSquared() <= this.currentMaxSpeedOnWire * this.currentMaxSpeedOnWire) {
                    this.velocity.add(accelVec);
                }
            }
        }

        Vector2D closestPointOnSegment;
        double segmentDx = segmentEndPoint.getX() - segmentStartPoint.getX();
        double segmentDy = segmentEndPoint.getY() - segmentStartPoint.getY();
        double segmentLengthSq = segmentDx * segmentDx + segmentDy * segmentDy;

        if (segmentLengthSq < 0.0001) {
            closestPointOnSegment = new Vector2D(segmentStartPoint.getX(), segmentStartPoint.getY());
        } else {
            double t = ((packetPos.getX() - segmentStartPoint.getX()) * segmentDx + (packetPos.getY() - segmentStartPoint.getY()) * segmentDy) / segmentLengthSq;
            t = Math.max(0, Math.min(1, t));
            closestPointOnSegment = new Vector2D(segmentStartPoint.getX() + t * segmentDx, segmentStartPoint.getY() + t * segmentDy);
        }
        double distanceFromWire = packetPos.distance(closestPointOnSegment);

        if (distanceFromWire > Config.MIN_DEVIATION_FOR_RETURN_FORCE && stabilityOnWire > Config.MIN_STABILITY_FOR_RETURN_FORCE) {
            Vector2D forceDir = Vector2D.subtract(closestPointOnSegment, packetPos).normalize();
            double returnForceMag = Math.min(distanceFromWire * Config.WIRE_RETURN_FORCE_STRENGTH, Config.MAX_WIRE_RETURN_FORCE);
            this.velocity.add(Vector2D.multiply(forceDir, returnForceMag * stabilityOnWire * deltaTime));
        }

        for (EffectPoint effect : gameState.getEffectPoints()) {
            if (effect.getType() == EffectType.ELIPHAS_CENTERING_FORCE) {
                if (packetPos.distance(new Vector2D(effect.getPosition().getX(), effect.getPosition().getY())) < 15.0) {
                    if (distanceFromWire > 0.5) {
                        Vector2D forceToCenter = Vector2D.subtract(closestPointOnSegment, packetPos);
                        double centeringForceStrength = 0.1;
                        this.velocity.add(Vector2D.multiply(forceToCenter, centeringForceStrength));
                    }
                }
            }
        }

        this.x += velocity.getX() * deltaTime;
        this.y += velocity.getY() * deltaTime;
        updateLabelPosition();

        if (stabilityOnWire < Config.MIN_STABILITY_TO_STAY_ON_WIRE || distanceFromWire > Config.DEVIATION_THRESHOLD_FOR_OFF_WIRE) {
            isOffWire = true;
            timeSpentIdleOffWireSeconds = 0.0;
        }

        return false;
    }

    public void handleCollision() {
        if (this.type == PacketType.MESSENGER_INFINITY && !this.isReturningToSource) {
            System.out.println("Messenger packet collided! Returning to source.");
            this.isReturningToSource = true;

            Port temp = this.startPort;
            this.startPort = this.targetPort;
            this.targetPort = temp;

            this.velocity.multiply(-1);

            this.currentSegmentIndex = 0;
        }
    }

    private String getPacketIdSafe() {
        return (this.id != null) ? this.id.toString().substring(0, 4) : "[NO_ID]";
    }

    public void applyImpactForce(Vector2D force) {
        if (this.velocity == null) this.velocity = new Vector2D();
        if (force == null || force.magnitudeSquared() < 0.0001) return;

        Vector2D effectiveForce = Vector2D.multiply(force.normalize(), force.magnitude() * Config.IMPACT_FORCE_EFFECTIVENESS_ON_VELOCITY);

        Vector2D currentVelocityComponent = Vector2D.multiply(this.velocity, Config.PACKET_INERTIA_FACTOR);
        Vector2D newForceComponent = Vector2D.multiply(effectiveForce, 1.0 - Config.PACKET_INERTIA_FACTOR);
        this.velocity.set(Vector2D.add(currentVelocityComponent, newForceComponent));

        double absMaxSpeedSq = Config.ABSOLUTE_MAX_SPEED_AFTER_IMPACT * Config.ABSOLUTE_MAX_SPEED_AFTER_IMPACT;
        if (this.velocity.magnitudeSquared() > absMaxSpeedSq) {
            this.velocity.normalize().multiply(Config.ABSOLUTE_MAX_SPEED_AFTER_IMPACT);
        }
        if (!isOffWire) {
            this.stabilityOnWire = Math.max(0, this.stabilityOnWire - Config.PACKET_STABILITY_REDUCTION_FACTOR_FROM_IMPACT);
        }
    }

    public void setOriginalType(PacketType type) {
        this.originalType = type;
        if (this.type == PacketType.PROTECTED) {
            int originalSize = (type == PacketType.SQUARE) ? Config.PACKET_SQUARE_INITIAL_SIZE :
                    (type == PacketType.TRIANGLE) ? Config.PACKET_TRIANGLE_INITIAL_SIZE :
                            Config.PACKET_MESSENGER_INITIAL_SIZE;

            this.currentSize = (int)(originalSize * Config.PROTECTED_PACKET_SIZE_MULTIPLIER);
            updateLabelPosition();
        }
    }

    public PacketType getOriginalType() {
        return this.originalType;
    }

    public PacketType getType() { return type; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; if(sizeLabel!=null) updateLabelPosition(); }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; if(sizeLabel!=null) updateLabelPosition(); }
    public Vector2D getVelocity() { return velocity; }
    public void setVelocity(Vector2D velocity) { this.velocity.set(velocity); }
    public double getAcceleration() { return acceleration; }
    public double getTimeSpentIdleOffWireSeconds() {
        return timeSpentIdleOffWireSeconds;
    }
    public void setAcceleration(double acceleration) { this.acceleration = acceleration; }
    public Connection getCurrentConnection() { return currentConnection; }
    public void setCurrentConnection(Connection connection) { this.currentConnection = connection; }
    public Port getTargetPort() { return targetPort; }
    public void setTargetPort(Port port) { this.targetPort = port; }
    public Port getStartPort() { return startPort; }
    public void setStartPort(Port port) { this.startPort = port; }
    public double getProgressOnConnection() { return progressOnConnection; }
    public void setProgressOnConnection(double progress) { this.progressOnConnection = progress; }
    public Node getVisualNode() { return visualNode; }
    public void setVisualNode(Node node) { this.visualNode = node; }
    public int getInitialSize() { return initialSize; }
    public int getCurrentSize() { return currentSize; }
    public void setCurrentSize(int size) { this.currentSize = Math.max(0,size); if(sizeLabel!=null) this.sizeLabel.setText(String.valueOf(this.currentSize));}
    public Label getSizeLabel() { return sizeLabel; }
    public double getNoiseLevel() { return noiseLevel; }
    public void setNoiseLevel(double noise) { this.noiseLevel = Math.max(0, Math.min(1.0,noise));}
    public double getStabilityOnWire() { return stabilityOnWire; }
    public void setStabilityOnWire(double stability) { this.stabilityOnWire = Math.max(0,Math.min(1.0,stability));}
    public boolean isOffWire() { return isOffWire; }

    public UUID getId() {
        return this.id;
    }

    public boolean isOnCollisionCooldownWith(Packet otherPacket) {
        if (otherPacket == null || !this.collisionCooldownWithPackets.containsKey(otherPacket.getId())) return false;

        double cooldownEndTime = this.collisionCooldownWithPackets.get(otherPacket.getId());
        boolean stillOnCooldown = (System.nanoTime() / 1_000_000_000.0) < cooldownEndTime;

        if (!stillOnCooldown) {
            this.collisionCooldownWithPackets.remove(otherPacket.getId());
        }
        return stillOnCooldown;
    }

    public void registerCollisionWith(Packet otherPacket) {
        if (otherPacket == null) return;

        double cooldownEndTime = System.nanoTime() / 1_000_000_000.0 + Config.PACKET_COLLISION_COOLDOWN_SECONDS;

        this.collisionCooldownWithPackets.put(otherPacket.getId(), cooldownEndTime);
        otherPacket.collisionCooldownWithPackets.put(this.id, cooldownEndTime);
    }

    private String getPacketIdSafeForGameState(Packet packet) {
        if (packet == null) return "[NULL_PACKET_IN_GS]";
        UUID packetId = packet.getId();
        return (packetId != null) ? packetId.toString().substring(0, 4) : "[NO_ID_IN_GS]";
    }

    public void decreaseSize(int amount) {
        if (amount <= 0 || this.currentSize <= 0) return;
        this.currentSize = Math.max(0, this.currentSize - amount);
        if (this.sizeLabel != null) this.sizeLabel.setText(String.valueOf(this.currentSize));
    }
    public void addNoise(double receivedNoise) {
        if (receivedNoise <= 0) {
            return;
        }

        double previousNoiseLevel = this.noiseLevel;
        this.noiseLevel = Math.max(0.0, Math.min(1.0, this.noiseLevel + receivedNoise));

        if (this.noiseLevel > previousNoiseLevel || receivedNoise > 0) {
            System.out.println("Packet " + getPacketIdSafe() + " (Type: " + this.type +
                    ") received noise: +" + String.format("%.3f", receivedNoise) +
                    ". New noise level: " + String.format("%.3f", this.noiseLevel));
        }
    }

    public void updateLabelPosition() {
        if (this.sizeLabel == null || this.visualNode == null) return;
        double labelOffsetX = -this.sizeLabel.prefWidth(-1) / 2.0;
        double labelOffsetY = -15;
        this.sizeLabel.setLayoutX(this.x + labelOffsetX);
        this.sizeLabel.setLayoutY(this.y + labelOffsetY);
    }

    public int getPlayerId() {
        return playerId;
    }
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public SystemNode getLastVisitedSystem() {
        return lastVisitedSystem;
    }

    public void setLastVisitedSystem(SystemNode system) {
        this.lastVisitedSystem = system;
    }

    public Vector2D getPosition() {
        return new Vector2D(this.x, this.y);
    }


    public int getCurrentSegmentIndex() {
        return currentSegmentIndex;
    }

    public void setCurrentSegmentIndex(int index) {
        this.currentSegmentIndex = index;
    }

    public Color getVisualColor() { return visualColor; }

    public void setVisualColor(Color color) { this.visualColor = color; }

    public boolean isLargePacket() {
        return this.type == PacketType.LARGE_8 || this.type == PacketType.LARGE_10;
    }

    public int getTotalBitsForParent() {
        return totalBitsForParent;
    }

    public void setTotalBitsForParent(int total) {
        this.totalBitsForParent = total;
    }

    public String getParentLargePacketId() {
        return parentLargePacketId;
    }

    public void setParentLargePacketId(String id) {
        this.parentLargePacketId = id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}