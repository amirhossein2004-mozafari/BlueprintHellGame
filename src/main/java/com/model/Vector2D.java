package com.model;

public class Vector2D {
    private double x;
    private double y;

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public Vector2D() { this(0, 0); }
    public Vector2D(Vector2D other) { this(other.x, other.y); }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public void set(double x, double y) { this.x = x; this.y = y; }
    public void set(Vector2D other) { set(other.x, other.y); }

    public Vector2D add(Vector2D other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }
    public Vector2D subtract(Vector2D other) {
        this.x -= other.x;
        this.y -= other.y;
        return this;
    }
    public Vector2D multiply(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
    }
    public static Vector2D add(Vector2D v1, Vector2D v2) { return new Vector2D(v1.x + v2.x, v1.y + v2.y); }
    public static Vector2D subtract(Vector2D v1, Vector2D v2) { return new Vector2D(v1.x - v2.x, v1.y - v2.y); }
    public static Vector2D multiply(Vector2D v, double scalar) { return new Vector2D(v.x * scalar, v.y * scalar); }

    public double magnitude() { return Math.sqrt(x * x + y * y); }
    public double magnitudeSquared() { return x * x + y * y; }

    public Vector2D normalize() {
        double mag = magnitude();
        if (mag > 0.00001) {
            x /= mag;
            y /= mag;
        }
        return this;
    }
    public static Vector2D normalized(Vector2D v) {
        Vector2D result = new Vector2D(v);
        result.normalize();
        return result;
    }

    public double distance(Vector2D other) { return Math.sqrt(Math.pow(other.x - this.x, 2) + Math.pow(other.y - this.y, 2)); }
    public static double distance(Vector2D v1, Vector2D v2) { return v1.distance(v2); }

    public static Vector2D direction(Vector2D from, Vector2D to) {
        return new Vector2D(to.x - from.x, to.y - from.y).normalize();
    }

    @Override
    public String toString() { return String.format("(%.2f, %.2f)", x, y); }
}