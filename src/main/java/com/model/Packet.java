package com.model;

import com.blueprinthell.Config;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Packet {

    private final PacketType type;
    private double x;
    private double y;
    private Vector2D velocity = new Vector2D();
    private double acceleration = 0.0;
    private Connection currentConnection = null;
    private Port targetPort = null;
    private Port startPort = null;
    private double progressOnConnection = 0.0;
    private transient Node visualNode = null;
    private final int initialSize;
    private int currentSize;
    private transient Label sizeLabel = null;
    private double noiseLevel = 0.0;
    private double stabilityOnWire = 1.0;
    private boolean isOffWire = false;

    public Packet(PacketType type, double initialX, double initialY) {
        this.type = type;
        this.x = initialX;
        this.y = initialY;

        if (type == PacketType.SQUARE) {
            this.initialSize = 2;
        } else {
            this.initialSize = 3;
        }
        this.currentSize = this.initialSize;

        this.sizeLabel = new Label(String.valueOf(this.currentSize));
        this.sizeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        this.sizeLabel.setTextFill(Color.WHITE);
        this.sizeLabel.setMouseTransparent(true);

    }

    public boolean update(double deltaTime) {
        if (deltaTime <= 0) return false;

        if (isOffWire) {
            this.setX(this.x + velocity.getX() * deltaTime);
            this.setY(this.y + velocity.getY() * deltaTime);
            velocity.multiply(Config.OFF_WIRE_FRICTION);
            if (velocity.magnitudeSquared() < 0.5) {
                velocity.set(0, 0);
            }
            return false;
        }

        if (currentConnection == null || targetPort == null || startPort == null) {
            if (velocity.magnitudeSquared() > 0.01) {
                isOffWire = true;
                System.out.println("Packet " + type + " became off-wire (no connection but moving).");
            }
            return false;
        }

        if (acceleration > 0 && stabilityOnWire > 0.1) {
            if (velocity.magnitudeSquared() < (Config.BASE_PACKET_SPEED * Config.BASE_PACKET_SPEED * 1.1)) {
                Vector2D direction = Vector2D.direction(new Vector2D(startPort.getAbsoluteX(), startPort.getAbsoluteY()), new Vector2D(targetPort.getAbsoluteX(), targetPort.getAbsoluteY()));
                velocity.add(Vector2D.multiply(direction, acceleration * deltaTime));
            } else {
                Vector2D direction = Vector2D.normalized(velocity);
                velocity.set(Vector2D.multiply(direction, Config.BASE_PACKET_SPEED));
                acceleration = 0;
            }
        }

        Vector2D packetPosVec = new Vector2D(this.x, this.y);
        Point2D p1_javafx = new Point2D(startPort.getAbsoluteX(), startPort.getAbsoluteY());
        Point2D p2_javafx = new Point2D(targetPort.getAbsoluteX(), targetPort.getAbsoluteY());
        double dx_segment = p1_javafx.getX() - p2_javafx.getX();
        double dy_segment = p1_javafx.getY() - p2_javafx.getY();
        double lineSegmentLengthSq = dx_segment * dx_segment + dy_segment * dy_segment;
        Vector2D closestPointOnWireSegment;

        if (lineSegmentLengthSq < 0.0001) {
            closestPointOnWireSegment = new Vector2D(p1_javafx.getX(), p1_javafx.getY());
        } else {
            double t = ((packetPosVec.getX() - p1_javafx.getX()) * (p2_javafx.getX() - p1_javafx.getX()) + (packetPosVec.getY() - p1_javafx.getY()) * (p2_javafx.getY() - p1_javafx.getY())) / lineSegmentLengthSq;
            t = Math.max(0, Math.min(1, t));
            closestPointOnWireSegment = new Vector2D(p1_javafx.getX() + t * (p2_javafx.getX() - p1_javafx.getX()), p1_javafx.getY() + t * (p2_javafx.getY() - p1_javafx.getY()));
        }
        double distanceFromWire = packetPosVec.distance(closestPointOnWireSegment);

        if (distanceFromWire > 0.1 && stabilityOnWire > 0.01) {
            Vector2D forceToWireDir = Vector2D.subtract(closestPointOnWireSegment, packetPosVec).normalize();
            double returnForceMag = distanceFromWire * stabilityOnWire * Config.WIRE_RETURN_FORCE_STRENGTH;
            velocity.add(Vector2D.multiply(forceToWireDir, returnForceMag * deltaTime));
        }

        this.setX(this.x + velocity.getX() * deltaTime);
        this.setY(this.y + velocity.getY() * deltaTime);

        Vector2D wireDirection = Vector2D.direction(new Vector2D(startPort.getAbsoluteX(), startPort.getAbsoluteY()), new Vector2D(targetPort.getAbsoluteX(), targetPort.getAbsoluteY()));
        double speedAlongWire = velocity.getX() * wireDirection.getX() + velocity.getY() * wireDirection.getY();
        double distanceMovedAlongWire = speedAlongWire * deltaTime;
        double totalWireLength = currentConnection.getLength();

        if (totalWireLength < 0.01) {
            if(!isOffWire) {
                this.setX(targetPort.getAbsoluteX()); this.setY(targetPort.getAbsoluteY());
                this.velocity.set(0,0); this.acceleration = 0;
                return true;
            }
            return false;
        }

        progressOnConnection += distanceMovedAlongWire / totalWireLength;

        if (!isOffWire && (stabilityOnWire <= 0.01 || distanceFromWire > Config.COLLISION_DEVIATION_THRESHOLD)) {
            isOffWire = true;
            System.out.println("Packet " + type + " became off-wire. Stability: " + String.format("%.2f", stabilityOnWire) + ", Deviation: " + String.format("%.2f", distanceFromWire));
        }
        progressOnConnection = Math.max(0, Math.min(1.0, progressOnConnection));

        if (progressOnConnection >= 0.99 && !isOffWire) {
            this.setX(targetPort.getAbsoluteX());
            this.setY(targetPort.getAbsoluteY());
            this.velocity.set(0, 0);
            this.acceleration = 0;
            return true;
        }
        return false;
    }

    public void startMoving(Connection connection) {
        if (connection == null) return;
        this.currentConnection = connection;
        this.startPort = connection.getStartPort();
        this.targetPort = connection.getEndPort();
        this.progressOnConnection = 0.0;
        this.acceleration = 0.0;

        this.setX(startPort.getAbsoluteX());
        this.setY(startPort.getAbsoluteY());

        boolean compatibleStart = (this.type == startPort.getPacketType());
        double initialSpeed;
        Vector2D direction = Vector2D.direction(
                new Vector2D(startPort.getAbsoluteX(), startPort.getAbsoluteY()),
                new Vector2D(targetPort.getAbsoluteX(), targetPort.getAbsoluteY())
        );

        if (this.type == PacketType.SQUARE) {
            initialSpeed = compatibleStart ? (Config.BASE_PACKET_SPEED * 0.5) : Config.BASE_PACKET_SPEED;
        } else {
            if (compatibleStart) {
                initialSpeed = Config.BASE_PACKET_SPEED;
            } else {
                initialSpeed = 1.0;
                this.acceleration = Config.TRIANGLE_PACKET_ACCELERATION;
            }
        }
        this.velocity.set(direction.multiply(initialSpeed));
        updateLabelPosition();
        this.isOffWire = false;
        this.stabilityOnWire = 1.0;
    }

    public void applyImpactForce(Vector2D force) {
        if (this.velocity == null) this.velocity = new Vector2D();

        double forceMag = force.magnitude();


        this.velocity.add(force);
        this.stabilityOnWire = Math.max(0, this.stabilityOnWire - (forceMag / Config.COLLISION_IMPACT_BASE_FORCE) * Config.PACKET_STABILITY_REDUCTION_FACTOR);

        if (this.stabilityOnWire <= 0.01 && !isOffWire) {
            System.out.println("Packet stability depleted by impact. Now off-wire.");
            isOffWire = true;
        }
        this.acceleration = 0;
    }

    public void updateLabelPosition() {
        if (this.sizeLabel != null) {
            double visualPacketWidth, visualPacketHeight;
            if (type == PacketType.SQUARE) {
                visualPacketWidth = Config.PORT_VISUAL_SIZE_SQUARE * 2;
                visualPacketHeight = Config.PORT_VISUAL_SIZE_SQUARE * 2;
            } else {
                visualPacketWidth = Config.PORT_VISUAL_SIZE_TRIANGLE * 1.8;
                visualPacketHeight = Config.PORT_VISUAL_SIZE_TRIANGLE * 1.8 * (Math.sqrt(3)/2.0) / (2.0/3.0);
            }

            double labelWidth = this.sizeLabel.prefWidth(-1);
            double labelOffsetX = -labelWidth / 2.0;
            double labelOffsetY = -visualPacketHeight / 1.5 - this.sizeLabel.prefHeight(-1)/2;

            this.sizeLabel.setLayoutX(this.x + labelOffsetX);
            this.sizeLabel.setLayoutY(this.y + labelOffsetY);
        }
    }

    public void decreaseSize(int amount) {
        if (amount > 0) {
            this.currentSize = Math.max(0, this.currentSize - amount);
            if (this.sizeLabel != null) {
                this.sizeLabel.setText(String.valueOf(this.currentSize));
            }
        }
    }

    public void addNoise(double receivedNoise) {
        if (receivedNoise > 0) {
            this.stabilityOnWire = Math.max(0, this.stabilityOnWire - receivedNoise * Config.PACKET_NOISE_TO_STABILITY_FACTOR);
            if (this.sizeLabel != null && Config.COLOR_HUD_TEXT != null) {
                double G = Math.max(0, 1.0 - noiseLevel - receivedNoise);
                this.sizeLabel.setTextFill(Color.rgb(255, (int)(G * 200) + 55, (int)(G*150)+55));
            }
            if (this.stabilityOnWire <= 0.01 && !isOffWire) {
                System.out.println("Packet " + type + " stability depleted by noise. Becoming off-wire.");
            }
        }
    }

    public PacketType getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
    public Vector2D getVelocity() { return velocity; }
    public double getAcceleration() { return acceleration; }
    public Connection getCurrentConnection() { return currentConnection; }
    public Port getTargetPort() { return targetPort; }
    public Port getStartPort() { return startPort; }
    public double getProgressOnConnection() { return progressOnConnection; }
    public Node getVisualNode() { return visualNode; }
    public int getInitialSize() { return initialSize; }
    public int getCurrentSize() { return currentSize; }
    public Label getSizeLabel() { return sizeLabel; }
    public double getNoiseLevel() { return noiseLevel; }
    public boolean isOffWire() { return isOffWire; }


    public void setX(double x) { this.x = x; updateLabelPosition(); }
    public void setY(double y) { this.y = y; updateLabelPosition(); }
    public void setVelocity(Vector2D velocity) { this.velocity.set(velocity); }
    public void setAcceleration(double acceleration) { this.acceleration = acceleration; }
    public void setCurrentConnection(Connection currentConnection) { this.currentConnection = currentConnection; }
    public void setTargetPort(Port targetPort) { this.targetPort = targetPort; }
    public void setStartPort(Port startPort) { this.startPort = startPort; }
    public void setProgressOnConnection(double progressOnConnection) { this.progressOnConnection = progressOnConnection; }
    public void setVisualNode(Node visualNode) { this.visualNode = visualNode; }
    public void setCurrentSize(int currentSize) {
        this.currentSize = Math.max(0, currentSize);
        if(this.sizeLabel != null) this.sizeLabel.setText(String.valueOf(this.currentSize));
    }
}