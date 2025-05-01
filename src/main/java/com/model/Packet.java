package com.model;

import com.blueprinthell.Config;
import javafx.geometry.Point2D;

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

    public Packet(PacketType type, double initialX, double initialY) {
        this.type = type;
        this.x = initialX;
        this.y = initialY;
        this.velocity = new Vector2D();
        this.acceleration = 0.0;
        this.progressOnConnection = 0.0;
    }

    public boolean update(double deltaTime) {
        if (currentConnection == null || targetPort == null || deltaTime <= 0) {
            return false;
        }
        if (acceleration > 0) {
            Vector2D direction = Vector2D.direction(
                    new Vector2D(startPort.getAbsoluteX(), startPort.getAbsoluteY()),
                    new Vector2D(targetPort.getAbsoluteX(), targetPort.getAbsoluteY())
            );
            velocity.add(Vector2D.multiply(direction, acceleration * deltaTime));
        }
        double speed = velocity.magnitude();
        double distanceToMove = speed * deltaTime;
        double totalWireLength = currentConnection.getLength();
        if (totalWireLength < 0.01) return true;
        double remainingDistance = (1.0 - progressOnConnection) * totalWireLength;
        if (distanceToMove >= remainingDistance) {
            progressOnConnection = 1.0;
            this.x = targetPort.getAbsoluteX();
            this.y = targetPort.getAbsoluteY();
            resetMovementState();
            System.out.println("Packet reached target port: " + type);
            return true;
        } else {
            progressOnConnection += distanceToMove / totalWireLength;
            Point2D startPos = new Point2D(startPort.getAbsoluteX(), startPort.getAbsoluteY());
            Point2D endPos = new Point2D(targetPort.getAbsoluteX(), targetPort.getAbsoluteY());
            this.x = startPos.getX() + (endPos.getX() - startPos.getX()) * progressOnConnection;
            this.y = startPos.getY() + (endPos.getY() - startPos.getY()) * progressOnConnection;
            return false;
        }
    }
    public void startMoving(Connection connection) {
        if (connection == null) return;

        this.currentConnection = connection;
        this.startPort = connection.getStartPort();
        this.targetPort = connection.getEndPort();
        this.progressOnConnection = 0.0;
        this.x = startPort.getAbsoluteX();
        this.y = startPort.getAbsoluteY();

        boolean isCompatibleStart = (this.type == this.startPort.getPacketType());
        double initialSpeed;
        this.acceleration = 0;

        Vector2D direction = Vector2D.direction(
                new Vector2D(startPort.getAbsoluteX(), startPort.getAbsoluteY()),
                new Vector2D(targetPort.getAbsoluteX(), targetPort.getAbsoluteY())
        );

        if (this.type == PacketType.SQUARE) {
            if (isCompatibleStart) {
                initialSpeed = Config.BASE_PACKET_SPEED * Config.SPEED_FACTOR_INCOMPATIBLE_START;
            } else {
                initialSpeed = Config.BASE_PACKET_SPEED;
            }
        } else {
            if (isCompatibleStart) {
                initialSpeed = Config.BASE_PACKET_SPEED;
            } else {
                this.acceleration = Config.TRIANGLE_PACKET_ACCELERATION;
                initialSpeed = 1.0;
            }
        }

        this.velocity.set(direction.multiply(initialSpeed));
    }

    public void resetMovementState() {
        this.currentConnection = null;
        this.targetPort = null;
        this.startPort = null;
        this.progressOnConnection = 0;
        if (this.velocity != null) {
            this.velocity.set(0, 0);
        }
        this.acceleration = 0;
    }

    public PacketType getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
    public Connection getCurrentConnection() { return currentConnection; }
    public Port getTargetPort() { return targetPort; }
    public Port getStartPort() { return startPort; }
}