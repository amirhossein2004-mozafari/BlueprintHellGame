package com.api.dto;

import com.api.enums.PacketType;
import javafx.geometry.Point2D;
import java.util.UUID;

public class PacketDTO {
    private UUID id;
    private PacketType packetType;
    private Point2D position;

    public PacketDTO() {

    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PacketType getPacketType() { return packetType; }
    public void setPacketType(PacketType packetType) { this.packetType = packetType; }

    public Point2D getPosition() { return position; }
    public void setPosition(Point2D position) { this.position = position; }

    public double getX() { return position != null ? position.getX() : 0; }
    public double getY() { return position != null ? position.getY() : 0; }
}