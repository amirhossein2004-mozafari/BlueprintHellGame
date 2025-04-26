package com.model;

import javafx.geometry.Point2D;

public class Port {

    private final SystemNode parentSystem;
    private final PortType type;
    private final PacketType packetType;
    private final Point2D relativePosition;

    public Port(SystemNode parent, PortType type, PacketType packetType, Point2D relativePosition) {
        this.parentSystem = parent;
        this.type = type;
        this.packetType = packetType;
        this.relativePosition = relativePosition;
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

}