package com.model;

import com.blueprinthell.Config;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.geometry.Point2D;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


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
    private final String id;
    private Map<String, Double> collisionCooldownWithPackets;
    private double currentMaxSpeedOnWire;
    private double inheritedStabilityDamage = 0.0;
    private double timeSpentIdleOffWireSeconds = 0.0;

    private int currentSegmentIndex;
    private String parentLargePacketId = null;
    private int totalBitsForParent = 0;


    public Packet(PacketType type, double initialX, double initialY) {
        this.id = UUID.randomUUID().toString();
        this.collisionCooldownWithPackets = new HashMap<>();
        this.type = type;
        this.x = initialX;
        this.y = initialY;
        this.velocity = new Vector2D();
        if (type == PacketType.SQUARE) {
            this.initialSize = Config.PACKET_SQUARE_INITIAL_SIZE;
        } else {
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

        List<Point2D> path = connection.getFullPath();
        if (path.size() < 2) {
            this.isOffWire = true;
            return;
        }
        Point2D startPoint = path.get(0);
        Point2D nextPointInPath = path.get(1);

        this.setX(startPoint.getX());
        this.setY(startPoint.getY());

        boolean compatibleStart = (this.type == startPort.getPacketType());
        double initialSpeedFactor;

        if (this.type == PacketType.SQUARE) {
            this.currentMaxSpeedOnWire = Config.BASE_PACKET_SPEED;
            initialSpeedFactor = compatibleStart ? Config.SQUARE_COMPATIBLE_SPEED_FACTOR : Config.SQUARE_INCOMPATIBLE_SPEED_FACTOR;
        } else if (this.type == PacketType.TRIANGLE) {
            this.currentMaxSpeedOnWire = Config.BASE_PACKET_SPEED * (compatibleStart ? Config.TRIANGLE_COMPATIBLE_SPEED_FACTOR : Config.TRIANGLE_INCOMPATIBLE_MAX_SPEED_FACTOR);
            this.acceleration = compatibleStart ? 0 : Config.TRIANGLE_PACKET_ACCELERATION;
            initialSpeedFactor = compatibleStart ? Config.TRIANGLE_COMPATIBLE_INITIAL_SPEED_FACTOR : Config.TRIANGLE_INCOMPATIBLE_INITIAL_SPEED_FACTOR;
        } else {

            this.currentMaxSpeedOnWire = Config.BASE_PACKET_SPEED;
            initialSpeedFactor = 1.0;
        }

        if (this.stabilityOnWire < Config.STABILITY_THRESHOLD_FOR_SPEED_REDUCTION) {
            initialSpeedFactor *= Math.max(Config.MIN_INITIAL_SPEED_FACTOR, Config.INTERMEDIATE_EXIT_SPEED_FACTOR_DAMAGED + this.stabilityOnWire);
        }

        double initialSpeed = this.currentMaxSpeedOnWire * initialSpeedFactor;
        initialSpeed = Math.min(initialSpeed, Config.ABSOLUTE_MAX_SPEED_AFTER_IMPACT);

        Vector2D direction = Vector2D.direction(
                new Vector2D(startPoint.getX(), startPoint.getY()),
                new Vector2D(nextPointInPath.getX(), nextPointInPath.getY())
        );
        if (direction.magnitudeSquared() < 0.0001) {
            direction = new Vector2D(1, 0);
        }

        this.velocity.set(Vector2D.multiply(direction, initialSpeed));

        if (this.sizeLabel != null) {
            updateLabelPosition();
        }
    }


    public boolean update(double deltaTime) {
        if (deltaTime <= 0) return false;

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

        List<Point2D> path = currentConnection.getFullPath();
        if (currentSegmentIndex >= path.size() - 1) return true;

        Point2D segmentStartPoint = path.get(currentSegmentIndex);
        Point2D segmentEndPoint = path.get(currentSegmentIndex + 1);
        Vector2D targetPos = new Vector2D(segmentEndPoint.getX(), segmentEndPoint.getY());
        Vector2D packetPos = new Vector2D(this.x, this.y);

        double distanceToTarget = packetPos.distance(targetPos);
        double travelDistanceThisFrame = velocity.magnitude() * deltaTime;

        if (distanceToTarget <= travelDistanceThisFrame * 1.05) {
            this.setX(targetPos.getX());
            this.setY(targetPos.getY());
            currentSegmentIndex++;

            if (currentSegmentIndex >= path.size() - 1) {
                if (this.getType() == PacketType.LARGE) {
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
            if (velocity.magnitude() < this.currentMaxSpeedOnWire) {
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

        this.x += velocity.getX() * deltaTime;
        this.y += velocity.getY() * deltaTime;
        updateLabelPosition();

        if (stabilityOnWire < Config.MIN_STABILITY_TO_STAY_ON_WIRE || distanceFromWire > Config.DEVIATION_THRESHOLD_FOR_OFF_WIRE) {
            isOffWire = true;
            timeSpentIdleOffWireSeconds = 0.0;
        }

        return false;
    }

    private String getPacketIdSafe() {
        if (this.id == null || this.id.isEmpty()) return "[NO_ID]";
        return (this.id.length() < 4) ? this.id : this.id.substring(0,4);
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
    public String getId() { return id; }
    public boolean isOnCollisionCooldownWith(Packet otherPacket) {
        if (otherPacket == null || !this.collisionCooldownWithPackets.containsKey(otherPacket.getId())) return false;
        double cooldownEndTime = this.collisionCooldownWithPackets.get(otherPacket.getId());
        boolean stillOnCooldown = (System.nanoTime() / 1_000_000_000.0) < cooldownEndTime;
        if (!stillOnCooldown) this.collisionCooldownWithPackets.remove(otherPacket.getId());
        return stillOnCooldown;
    }
    public void registerCollisionWith(Packet otherPacket) {
        if (otherPacket == null) return;
        double cooldownEndTime = System.nanoTime() / 1_000_000_000.0 + Config.PACKET_COLLISION_COOLDOWN_SECONDS;
        this.collisionCooldownWithPackets.put(otherPacket.getId(), cooldownEndTime);
        otherPacket.collisionCooldownWithPackets.put(this.id, cooldownEndTime);
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

    public int getTotalBitsForParent() {
        return totalBitsForParent;
    }

    public void setTotalBitsForParent(int totalBitsForParent) {
        this.totalBitsForParent = totalBitsForParent;
    }

    public String getParentLargePacketId() {
        return parentLargePacketId;
    }

    public void setParentLargePacketId(String parentLargePacketId) {
        this.parentLargePacketId = parentLargePacketId;
    }

}