package com.model;


public class Packet {

    private final PacketType type;
    private double x;
    private double y;


    public Packet(PacketType type, double initialX, double initialY) {
        this.type = type;
        this.x = initialX;
        this.y = initialY;
    }

    public PacketType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }
}