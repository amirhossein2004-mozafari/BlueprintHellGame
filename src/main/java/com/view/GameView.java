package com.view;

import com.blueprinthell.Config;
import com.model.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import java.util.List;

public class GameView extends Pane {

    public GameView(double width, double height) {
        this.setPrefSize(width, height);
        this.setMinSize(width, height);
        this.setMaxSize(width, height);
        this.setStyle("-fx-background-color: #" + Config.COLOR_BACKGROUND.toString().substring(2) + ";");
    }

    public void drawInitialState(GameState gameState) {
        this.getChildren().clear();
        for (SystemNode system : gameState.getSystems()) {
            drawSingleSystem(system);
        }
    }

    private void drawSingleSystem(SystemNode system) {
        Rectangle systemBody = new Rectangle(system.getX(), system.getY(), system.getWidth(), system.getHeight());
        systemBody.setFill(Config.COLOR_SYSTEM_RECT);
        systemBody.setStroke(Color.BLACK);
        systemBody.setStrokeWidth(1);
        systemBody.setArcWidth(Config.SYSTEM_CORNER_ARC);
        systemBody.setArcHeight(Config.SYSTEM_CORNER_ARC);
        this.getChildren().add(systemBody);

        Line divider = new Line(
                system.getX() + Config.SYSTEM_DIVIDER_X_OFFSET, system.getY() + 3,
                system.getX() + Config.SYSTEM_DIVIDER_X_OFFSET, system.getY() + system.getHeight() - 3
        );
        divider.setStroke(Config.COLOR_SYSTEM_DIVIDER);
        divider.setStrokeWidth(1);
        this.getChildren().add(divider);

        drawIndicatorForSystem(system);
        drawPortsForSystem(system.getInputPorts());
        drawPortsForSystem(system.getOutputPorts());
    }

    private void drawPortsForSystem(List<Port> ports) {
        for (Port port : ports) {
            Shape portShape = null;
            double size = 0;
            double cx = port.getAbsoluteX();
            double cy = port.getAbsoluteY();

            if (port.getPacketType() == PacketType.SQUARE) {
                size = Config.PORT_VISUAL_SIZE_SQUARE;
                Rectangle squarePort = new Rectangle(cx - size, cy - size, size * 2, size * 2);
                squarePort.setFill(Config.COLOR_PORT_SQUARE);
                portShape = squarePort;
            } else if (port.getPacketType() == PacketType.TRIANGLE) {
                size = Config.PORT_VISUAL_SIZE_TRIANGLE;
                Polygon trianglePort = new Polygon();
                double x1, y1, x2, y2, x3, y3;
                double hf = 1.0;
                double bf = 0.866;

                if (port.getType() == PortType.INPUT) {
                    x1 = cx + size * hf * 0.6; y1 = cy;
                    x2 = cx - size * hf * 0.4; y2 = cy - size * bf;
                    x3 = cx - size * hf * 0.4; y3 = cy + size * bf;
                } else {
                    x1 = cx - size * hf * 0.6; y1 = cy;
                    x2 = cx + size * hf * 0.4; y2 = cy - size * bf;
                    x3 = cx + size * hf * 0.4; y3 = cy + size * bf;
                }
                trianglePort.getPoints().addAll(x1, y1, x2, y2, x3, y3);
                trianglePort.setFill(Config.COLOR_PORT_TRIANGLE);
                portShape = trianglePort;
            } else {
                portShape = new Circle(cx, cy, Config.PORT_BASE_SIZE_UNIT * 0.5, Color.GRAY);
            }

            if (portShape != null) {
                portShape.setStroke(Color.BLACK);
                portShape.setStrokeWidth(0.5);
                this.getChildren().add(portShape);
            }
        }
    }

    private void drawIndicatorForSystem(SystemNode system) {
        Rectangle indicatorRect = new Rectangle();
        double indicatorWidth = Config.SYSTEM_DIVIDER_X_OFFSET - 2 * Config.INDICATOR_PADDING_X;
        indicatorRect.setX(system.getX() + Config.INDICATOR_PADDING_X);
        indicatorRect.setY(system.getY() + Config.INDICATOR_Y_POS);
        indicatorRect.setWidth(indicatorWidth);
        indicatorRect.setHeight(Config.INDICATOR_HEIGHT);
        indicatorRect.setFill(system.isIndicatorOn() ? Config.COLOR_SYSTEM_INDICATOR_ON : Config.COLOR_SYSTEM_INDICATOR_OFF);
        indicatorRect.setArcWidth(3);
        indicatorRect.setArcHeight(3);
        this.getChildren().add(indicatorRect);
    }
}