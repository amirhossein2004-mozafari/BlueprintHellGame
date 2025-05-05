package com.model;

import com.blueprinthell.Config;
import javafx.geometry.Point2D;
import javafx.scene.Node;

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


    public Packet(PacketType type, double initialX, double initialY) {
        this.type = type;
        this.x = initialX;
        this.y = initialY;
    }

    public boolean update(double deltaTime) {
        if (currentConnection == null || targetPort == null || deltaTime <= 0) {
            return false;
        }

        if (acceleration > 0) {
            if (velocity.magnitudeSquared() < Config.BASE_PACKET_SPEED * Config.BASE_PACKET_SPEED) {
                Vector2D direction = Vector2D.direction(
                        new Vector2D(currentConnection.getStartPort().getAbsoluteX(), currentConnection.getStartPort().getAbsoluteY()),
                        new Vector2D(targetPort.getAbsoluteX(), targetPort.getAbsoluteY())
                );
                velocity.add(Vector2D.multiply(direction, acceleration * deltaTime));
            } else {
                Vector2D direction = Vector2D.normalized(velocity);
                velocity.set(Vector2D.multiply(direction, Config.BASE_PACKET_SPEED));
                acceleration = 0;
            }
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
            velocity.set(0, 0);
            acceleration = 0;
            return true;
        } else {
            progressOnConnection += distanceToMove / totalWireLength;
            Point2D startPos = new Point2D(currentConnection.getStartPort().getAbsoluteX(), currentConnection.getStartPort().getAbsoluteY());
            Point2D endPos = new Point2D(targetPort.getAbsoluteX(), targetPort.getAbsoluteY());
            this.x = startPos.getX() + (endPos.getX() - startPos.getX()) * progressOnConnection;
            this.y = startPos.getY() + (endPos.getY() - startPos.getY()) * progressOnConnection;
            return false;
        }
    }

    public void startMoving(Connection connection) {
        if (connection == null) return;
        this.currentConnection = connection;
        Port startPort = connection.getStartPort();
        this.targetPort = connection.getEndPort();
        this.progressOnConnection = 0.0;
        this.acceleration = 0.0;

        this.x = startPort.getAbsoluteX();
        this.y = startPort.getAbsoluteY();

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

    public void setCurrentConnection(Connection currentConnection) { this.currentConnection = currentConnection; }
    public void setTargetPort(Port targetPort) { this.targetPort = targetPort; }
    public void setProgressOnConnection(double progressOnConnection) { this.progressOnConnection = progressOnConnection; }
    public void setAcceleration(double acceleration) { this.acceleration = acceleration; }
    public void setVisualNode(Node visualNode) { this.visualNode = visualNode; }

}