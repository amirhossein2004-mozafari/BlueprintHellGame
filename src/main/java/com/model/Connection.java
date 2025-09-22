package com.model;

import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Connection {

    private final String id;
    private final Port startPort;
    private final Port endPort;


    private final List<Point2D> bendPoints;
    private double length;
    private int largePacketPassages = 0;

    public Connection(Port startPort, Port endPort) {
        if (startPort == null || endPort == null) {
            throw new IllegalArgumentException("Start and End ports cannot be null");
        }
        if (startPort.getType() != PortType.OUTPUT || endPort.getType() != PortType.INPUT) {
            throw new IllegalArgumentException("Connection must be from OUTPUT to INPUT port");
        }

        this.id = UUID.randomUUID().toString();
        this.startPort = startPort;
        this.endPort = endPort;
        this.bendPoints = new ArrayList<>();

        startPort.setConnectedConnection(this);
        endPort.setConnectedConnection(this);

        recalculateLength();
    }


    private void recalculateLength() {
        List<Point2D> fullPath = getFullPath();
        double totalLength = 0;
        for (int i = 0; i < fullPath.size() - 1; i++) {
            totalLength += fullPath.get(i).distance(fullPath.get(i + 1));
        }
        this.length = totalLength;
    }


    public void addBendPoint(Point2D point) {
        if (point != null) {
            this.bendPoints.add(point);
            recalculateLength();
        }
    }


    public void moveBendPoint(int index, Point2D newPosition) {
        if (index >= 0 && index < bendPoints.size() && newPosition != null) {
            this.bendPoints.set(index, newPosition);
            recalculateLength();
        }
    }

    public List<Point2D> getFullPath() {
        List<Point2D> fullPath = new ArrayList<>();
        fullPath.add(new Point2D(startPort.getAbsoluteX(), startPort.getAbsoluteY()));
        fullPath.addAll(bendPoints);
        fullPath.add(new Point2D(endPort.getAbsoluteX(), endPort.getAbsoluteY()));
        return Collections.unmodifiableList(fullPath);
    }

    public List<Point2D> getBendPoints() {
        return Collections.unmodifiableList(bendPoints);
    }


    public void registerLargePacketPassage() {
        this.largePacketPassages++;
        System.out.println("Connection " + this.id.substring(0,4) + " has had " + this.largePacketPassages + " large packets pass.");
    }



    public String getId() { return id; }
    public Port getStartPort() { return startPort; }
    public Port getEndPort() { return endPort; }
    public double getLength() { return length; }
    public int getLargePacketPassages() { return largePacketPassages; }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}