package com.model;

import com.blueprinthell.Config;


public class SystemNode {

    private final double x;
    private final double y;
    private final double width;
    private final double height;


    public SystemNode(double x, double y) {
        this.x = x;
        this.y = y;
        this.width = Config.SYSTEM_WIDTH;
        this.height = Config.SYSTEM_HEIGHT;

    }


    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }


}