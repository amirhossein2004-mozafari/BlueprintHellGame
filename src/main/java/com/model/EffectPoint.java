package com.model;

import javafx.geometry.Point2D;

public class EffectPoint {

    private final EffectType type;
    private final Connection parentConnection;
    private final Point2D position;
    private double ttl;

    public EffectPoint(EffectType type, Connection parentConnection, Point2D position, double duration) {
        this.type = type;
        this.parentConnection = parentConnection;
        this.position = position;
        this.ttl = duration;
    }

    public boolean update(double deltaTime) {
        this.ttl -= deltaTime;
        return this.ttl <= 0;
    }

    public EffectType getType() { return type; }
    public Connection getParentConnection() { return parentConnection; }
    public Point2D getPosition() { return position; }
    public double getTimeLeft() { return ttl; }
}