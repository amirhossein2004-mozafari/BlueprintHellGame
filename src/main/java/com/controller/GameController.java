package com.controller;

import com.blueprinthell.Config;
import com.model.*;
import com.view.GameView;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Line;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;
import java.util.List;
import java.util.Random;

public class GameController {

    private final GameState gameState;
    private final GameView gameView;
    private final Random random = new Random();

    private boolean isWiring = false;
    private Port wiringStartPort = null;
    private Line tempWire = null;

    public GameController(GameState gameState, GameView gameView) {
        this.gameState = gameState;
        this.gameView = gameView;
    }

    public void initializeGame() {
        gameState.clearSystems();
        gameState.clearPackets();
        gameState.clearConnections();

        double[][] positions = { {150, 100}, {700, 100}, {400, 300}, {150, 500}, {700, 500} };
        for (int i = 0; i < 5; i++) {
            if (i < positions.length) {
                SystemNode sys = new SystemNode(positions[i][0], positions[i][1]);
                gameState.addSystem(sys);
            }
        }

        gameState.addPacket(new Packet(PacketType.SQUARE, 50, 50));
        gameState.addPacket(new Packet(PacketType.TRIANGLE, 600, 400));
        gameState.addPacket(new Packet(PacketType.SQUARE, 300, 50));

        gameView.redrawAll(gameState);

        System.out.println("Game Initialized with " + gameState.getSystems().size() + " systems and " + gameState.getPackets().size() + " packets.");
    }

    public void updateGame(double deltaTime) {

    }

    public void handleMousePress(MouseEvent event) {
        if (isWiring) return;

        Port clickedPort = findClickedPort(event.getX(), event.getY(), PortType.OUTPUT);

        if (clickedPort != null) {
            isWiring = true;
            wiringStartPort = clickedPort;
            tempWire = new Line(
                    clickedPort.getAbsoluteX(), clickedPort.getAbsoluteY(),
                    event.getX(), event.getY()
            );
            tempWire.setStrokeWidth(Config.WIRE_THICKNESS);
            tempWire.setStroke(Config.COLOR_WIRE_TEMP_VALID);
            tempWire.setMouseTransparent(true);
            gameView.addNode(tempWire);
            System.out.println("Wiring started from Port on System: " + clickedPort.getParentSystem());
        } else {
            System.out.println("Mouse Pressed outside any output port.");
        }
    }

    public void handleMouseDrag(MouseEvent event) {
        if (isWiring && tempWire != null) {
            tempWire.setEndX(event.getX());
            tempWire.setEndY(event.getY());
        }
    }

    public void handleMouseRelease(MouseEvent event) {
        boolean connectionMade = false;
        if (isWiring) {

            Port targetPort = findClickedPort(event.getX(), event.getY(), PortType.INPUT);

            if (targetPort != null &&
                    targetPort.getParentSystem() != wiringStartPort.getParentSystem() &&
                    targetPort.getPacketType() == wiringStartPort.getPacketType() )

            {

                double requiredLength = new Point2D(wiringStartPort.getAbsoluteX(), wiringStartPort.getAbsoluteY())
                        .distance(targetPort.getAbsoluteX(), targetPort.getAbsoluteY());

                if (gameState.getRemainingWireLength() >= requiredLength) {
                    Connection newConnection = new Connection(wiringStartPort, targetPort);

                    if (gameState.useWire(requiredLength)) {
                        gameState.addConnection(newConnection);
                        System.out.println("Connection created successfully! Length: " + String.format("%.1f", requiredLength));
                        connectionMade = true;
                    } else {
                        System.out.println("Wire length usage failed (should not happen after check).");
                    }
                } else {
                    System.out.println("Wiring failed: Not enough wire length remaining.");
                }
            } else {
                System.out.println("Wiring failed: Invalid target port (not input, same system, type mismatch, or already connected).");
            }
        }

        isWiring = false;
        wiringStartPort = null;
        if (tempWire != null) {
            gameView.removeNode(tempWire);
            tempWire = null;
        }
        if (!connectionMade && event.getEventType() == MouseEvent.MOUSE_RELEASED) {
            System.out.println("Wiring cancelled.");
        }
        gameView.redrawAll(gameState);
    }

    public void handleKeyPress(KeyCode code) {
        System.out.println("Key Pressed: " + code);
    }

    private Port findClickedPort(double clickX, double clickY, PortType typeToFind) {
        for (SystemNode system : gameState.getSystems()) {
            List<Port> portsToCheck = (typeToFind == PortType.INPUT) ? system.getInputPorts() : system.getOutputPorts();
            for (Port port : portsToCheck) {
                double portX = port.getAbsoluteX();
                double portY = port.getAbsoluteY();
                double clickRadiusMultiplier = 1.5;
                double portHitRadius = (port.getPacketType() == PacketType.SQUARE ? Config.PORT_VISUAL_SIZE_SQUARE : Config.PORT_VISUAL_SIZE_TRIANGLE) * clickRadiusMultiplier;
                double distanceSquared = Math.pow(clickX - portX, 2) + Math.pow(clickY - portY, 2);
                if (distanceSquared < Math.pow(portHitRadius, 2)) {
                    return port;
                }
            }
        }
        return null;
    }

    public void pauseGameLogic() {
        System.out.println("Game Logic Paused (by Controller)");
    }
    public void resumeGameLogic() {
        System.out.println("Game Logic Resumed (by Controller)");
    }
    public void pauseGame() {
        System.out.println("Game Paused (Animation Stopped)");
    }
    public void resumeGame() {
        System.out.println("Game Resumed (Animation Started)");
    }
}