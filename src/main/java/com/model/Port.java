package com.model;

import javafx.geometry.Point2D;
import javafx.scene.shape.Shape;

public class Port {

    private final SystemNode parentSystem;
    private final PortType type;
    private PacketType packetType;
    private final Point2D relativePosition;
    private transient Shape visualShape = null;
    private Connection connectedConnection = null;

    public Port(SystemNode parent, PortType type, PacketType packetType, Point2D relativePosition) {
        this.parentSystem = parent;
        this.type = type;
        this.packetType = packetType;
        this.relativePosition = relativePosition;
    }

    public Connection getConnectedConnection() {
        return connectedConnection;
    }

    public void setConnectedConnection(Connection connection) {
        this.connectedConnection = connection;
    }

    public boolean isLargePortType() {
        return this.packetType == PacketType.LARGE;
    }

    public boolean isConnected() {
        return connectedConnection != null;
    }

    public SystemNode getParentSystem() {
        return parentSystem;
    }

    public PortType getType() {
        return type;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public double getAbsoluteX() {
        return parentSystem.getX() + relativePosition.getX();
    }

    public double getAbsoluteY() {
        return parentSystem.getY() + relativePosition.getY();
    }

    public Point2D getRelativePosition() {
        return relativePosition;
    }
    public Shape getVisualShape() {
        return visualShape;
    }

    public void setVisualShape(Shape visualShape) {
        this.visualShape = visualShape;
    }

    public void setPacketType(PacketType packetType) {
        this.packetType = packetType;
    }
}