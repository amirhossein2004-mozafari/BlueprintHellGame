package com.view;

import com.blueprinthell.Config;
import com.model.*;

import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;


public class GameView extends Pane {

    private final Map<Packet, Node> packetNodes = new HashMap<>();
    private final Map<Port, Shape> portShapes = new HashMap<>();
    private int myPlayerId = 0;

    public GameView(double windowWidth, double gameAreaHeight) {
        this.setStyle("-fx-background-color: #" + Config.COLOR_BACKGROUND.toString().substring(2) + ";");
        Rectangle background = new Rectangle(windowWidth, gameAreaHeight);
        background.setFill(Color.TRANSPARENT);
        this.getChildren().add(background);
        this.setPrefSize(windowWidth, gameAreaHeight);
        this.setMinWidth(Math.max(0, windowWidth - Config.SHOP_VIEW_DEFAULT_WIDTH - 50));
        this.setMaxSize(windowWidth, gameAreaHeight);
        System.out.println("GameView constructed. PrefWidth: " + getPrefWidth() + " MinWidth: " + getMinWidth());
    }

    public void redrawAll(GameState myGameState, GameState opponentGameState, int myPlayerId) {
        this.myPlayerId = myPlayerId;
        this.getChildren().clear();
        packetNodes.clear();
        portShapes.clear();
        if (opponentGameState != null) {
            for (SystemNode system : opponentGameState.getSystems()) {
                drawSingleSystem(system, 0.3);
            }
        }

        if (myGameState != null) {
            drawConnections(myGameState.getConnections());
            drawSystems(myGameState.getSystems());
            drawPackets(myGameState.getPackets());
            drawEffectPoints(myGameState.getEffectPoints());
        }

    }

    private void drawConnections(List<Connection> connections) {
        for (Connection connection : connections) {
            drawSingleWire(connection);
        }
    }

    private void drawSystems(List<SystemNode> systems) {
        for (SystemNode system : systems) {
            drawSingleSystem(system);
        }
    }

    private void drawPackets(List<Packet> packets) {
        for (Packet packet : packets) {
            drawSinglePacket(packet);
        }
    }

    private void drawSingleWire(Connection connection) {
        List<javafx.geometry.Point2D> path = connection.getFullPath();
        if (path.size() < 2) {
            return;
        }

        for (int i = 0; i < path.size() - 1; i++) {
            javafx.geometry.Point2D startPoint = path.get(i);
            javafx.geometry.Point2D endPoint = path.get(i + 1);

            Line wireSegment = new Line(
                    startPoint.getX(),
                    startPoint.getY(),
                    endPoint.getX(),
                    endPoint.getY()
            );

            wireSegment.setStroke(Config.COLOR_WIRE_NORMAL);
            wireSegment.setStrokeWidth(Config.WIRE_THICKNESS);

            wireSegment.setMouseTransparent(false);

            this.getChildren().add(wireSegment);
        }

        for (int i = 1; i < path.size() - 1; i++) {
            javafx.geometry.Point2D bendPoint = path.get(i);

            Circle bendCircle = new Circle(bendPoint.getX(), bendPoint.getY(), 4, Config.COLOR_WIRE_HIGHLIGHT);
            bendCircle.setStroke(Color.BLACK);
            bendCircle.setStrokeWidth(1);
            bendCircle.setMouseTransparent(false);

            this.getChildren().add(bendCircle);
        }
    }



    private void drawSingleSystem(SystemNode system, double opacity) {

        Rectangle systemBody = new Rectangle(system.getX(), system.getY(), system.getWidth(), system.getHeight());


        Color bodyColor;
        if (system.isOnCooldown()) {
            bodyColor = Color.DARKSLATEGRAY;
        } else if (system.isControllable()) {
            bodyColor = Color.rgb(100, 50, 150);
        } else if (system.getRole() == SystemRole.VPN) {
            bodyColor = Color.STEELBLUE;
        } else if (system.getRole() == SystemRole.SABOTEUR) {
            bodyColor = Color.FIREBRICK;
        } else if (system.getRole() == SystemRole.SPY) {
            bodyColor = Color.SLATEGRAY;
        } else if (system.getRole() == SystemRole.ANTI_TROJAN) {
            bodyColor = Color.MEDIUMSEAGREEN;
        } else if (system.getRole() == SystemRole.DISTRIBUTE) {
            bodyColor = Color.ORANGE;
        } else if (system.getRole() == SystemRole.MERGE) {
            bodyColor = Color.DARKORANGE;
        } else {
            bodyColor = Config.COLOR_SYSTEM_RECT;
        }
        systemBody.setFill(bodyColor);
        systemBody.setOpacity(opacity);
        systemBody.setStroke(Color.BLACK);
        systemBody.setStrokeWidth(1);
        systemBody.setArcWidth(Config.SYSTEM_CORNER_ARC);
        systemBody.setArcHeight(Config.SYSTEM_CORNER_ARC);
        this.getChildren().add(systemBody);


        if (system.getRole() != SystemRole.ANTI_TROJAN) {
            Line divider = new Line(
                    system.getX() + Config.SYSTEM_DIVIDER_X_OFFSET, system.getY() + 3,
                    system.getX() + Config.SYSTEM_DIVIDER_X_OFFSET, system.getY() + system.getHeight() - 3
            );
            divider.setStroke(Config.COLOR_SYSTEM_DIVIDER);
            divider.setStrokeWidth(1);
            this.getChildren().add(divider);
        }

        drawIndicatorForSystem(system);
        drawPortsForSystem(system.getInputPorts());
        drawPortsForSystem(system.getOutputPorts());

        if (system.getRole() == SystemRole.INTERMEDIATE ||
                system.getRole() == SystemRole.SABOTEUR ||
                system.getRole() == SystemRole.SPY ||
                system.getRole() == SystemRole.MERGE ||
                system.getRole() == SystemRole.DISTRIBUTE) {

            Label storedCountLabel = new Label();

            storedCountLabel.textProperty().bind(
                    Bindings.format("%d/%d",
                            system.getStoredPacketCount(),
                            Config.SYSTEM_PACKET_STORAGE_CAPACITY
                    )
            );
            storedCountLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            storedCountLabel.setTextFill(Color.WHITE);

            storedCountLabel.setLayoutX(system.getX() + 3);
            storedCountLabel.setLayoutY(system.getY() + system.getHeight() - 15);
            storedCountLabel.setMouseTransparent(true);
            this.getChildren().add(storedCountLabel);
        }
    }

    private void drawSingleSystem(SystemNode system) {
        drawSingleSystem(system, 1.0);
    }

    private void drawPortsForSystem(List<Port> ports) {
        for (Port port : ports) {
            Shape portShape = createPortShape(port);
            if (portShape != null) {
                portShape.setStroke(Color.BLACK);
                portShape.setStrokeWidth(0.5);
                port.setVisualShape(portShape);
                this.getChildren().add(portShape);
            }
        }
    }

    private Shape createPortShape(Port port) {
        double size;
        double cx = port.getAbsoluteX();
        double cy = port.getAbsoluteY();

        if (port.getPacketType() == PacketType.SQUARE) {
            size = Config.PORT_VISUAL_SIZE_SQUARE;
            Rectangle squarePort = new Rectangle(cx - size, cy - size, size * 2, size * 2);
            squarePort.setFill(Config.COLOR_PORT_SQUARE);
            return squarePort;
        } else if (port.getPacketType() == PacketType.TRIANGLE) {
            size = Config.PORT_VISUAL_SIZE_TRIANGLE;
            Polygon trianglePort = new Polygon();
            double x1, y1, x2, y2, x3, y3;
            double hf = 1.0, bf = 0.866;
            if (port.getType() == PortType.INPUT) { x1=cx+size*hf*0.6; y1=cy; x2=cx-size*hf*0.4; y2=cy-size*bf; x3=cx-size*hf*0.4; y3=cy+size*bf;}
            else { x1=cx-size*hf*0.6; y1=cy; x2=cx+size*hf*0.4; y2=cy-size*bf; x3=cx+size*hf*0.4; y3=cy+size*bf;}
            trianglePort.getPoints().addAll(x1, y1, x2, y2, x3, y3);
            trianglePort.setFill(Config.COLOR_PORT_TRIANGLE);
            return trianglePort;
        } else if (port.isLargePortType()) {
            size = Config.PORT_BASE_SIZE_UNIT * 2.0;
            Polygon hexagon = new Polygon();
            for (int i = 0; i < 6; i++) {
                hexagon.getPoints().addAll(
                        cx + size * Math.cos(Math.toRadians(i * 60)),
                        cy + size * Math.sin(Math.toRadians(i * 60))
                );
            }
            hexagon.setFill(Color.MAGENTA.darker());
            return hexagon;
        } else if (port.getPacketType() == PacketType.MESSENGER_INFINITY) {
            size = Config.PORT_BASE_SIZE_UNIT * 1.5;
            Path path = new Path();
            path.getElements().add(new MoveTo(cx - size, cy));
            path.getElements().add(new CubicCurveTo(cx - size, cy - size, cx + size, cy + size, cx + size, cy));
            path.getElements().add(new CubicCurveTo(cx + size, cy - size, cx - size, cy + size, cx - size, cy));
            path.setFill(Color.LIGHTGRAY);
            path.setStroke(Color.WHITE);
            path.setStrokeWidth(0.5);
            return path;
        } else {
            return new Circle(cx, cy, Config.PORT_BASE_SIZE_UNIT * 0.5, Color.GRAY);
        }
    }


    public Port findPortAt(double screenX, double screenY) {
        for (Map.Entry<Port, Shape> entry : portShapes.entrySet()) {
            Shape shape = entry.getValue();
            if (shape.contains(screenX, screenY)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void drawIndicatorForSystem(SystemNode system) {
        Rectangle indicatorRect = new Rectangle();
        double indicatorWidth = Config.SYSTEM_DIVIDER_X_OFFSET - 2 * Config.INDICATOR_PADDING_X;
        indicatorRect.setX(system.getX() + Config.INDICATOR_PADDING_X);
        indicatorRect.setY(system.getY() + Config.INDICATOR_Y_POS);
        indicatorRect.setWidth(indicatorWidth);
        indicatorRect.setHeight(Config.INDICATOR_HEIGHT);

        if (system.isAllPortsActuallyConnected()) {
            indicatorRect.setFill(Config.COLOR_SYSTEM_INDICATOR_ON);
        } else {
            indicatorRect.setFill(Config.COLOR_SYSTEM_INDICATOR_OFF);
        }

        indicatorRect.setArcWidth(3);
        indicatorRect.setArcHeight(3);
        this.getChildren().add(indicatorRect);
    }

    private void drawSinglePacket(Packet packet) {
        Node packetNode = packetNodes.get(packet);
        Label sizeLabel = packet.getSizeLabel();

        if (packetNode == null) {
            packetNode = createPacketNode(packet);
            if (packetNode != null) {
                packet.setVisualNode(packetNode);
                this.getChildren().add(packetNode);
                packetNodes.put(packet, packetNode);

                if (sizeLabel != null && !this.getChildren().contains(sizeLabel)) {
                    this.getChildren().add(sizeLabel);
                    packet.updateLabelPosition();
                }
            }
        }
    }

    private Node createPacketNode(Packet packet) {
        double size;
        double x = packet.getX();
        double y = packet.getY();
        Shape node;

        switch (packet.getType()) {
            case SQUARE:
                size = Config.PORT_VISUAL_SIZE_SQUARE * 2;
                node = new Rectangle(x - size / 2.0, y - size / 2.0, size, size);
                node.setFill(Config.COLOR_PACKET_SQUARE);
                break;
            case TRIANGLE:
                size = Config.PORT_VISUAL_SIZE_TRIANGLE * 1.8;
                Polygon poly = new Polygon();
                double height = size * Math.sqrt(3) / 2.0;
                poly.getPoints().addAll(x, y - height * (2.0/3.0), x - size / 2.0, y + height * (1.0/3.0), x + size / 2.0, y + height * (1.0/3.0));
                node = poly;
                node.setFill(Config.COLOR_PACKET_TRIANGLE);
                break;
            case PROTECTED:
                size = 8;
                node = new Circle(x, y, size / 2.0);
                node.setFill(Color.DODGERBLUE);
                node.setStroke(Color.WHITE);
                node.setStrokeWidth(1.5);
                break;
            case TROJAN:
                size = 9;
                node = new Circle(x, y, size / 2.0);
                node.setFill(Color.CRIMSON);
                node.setStroke(Color.WHITE);
                break;
            case MESSENGER_INFINITY:
                size = Config.PORT_BASE_SIZE_UNIT * 1.8;
                Path path = new Path();
                path.getElements().add(new MoveTo(x - size, y));
                path.getElements().add(new CubicCurveTo(x - size, y - size, x + size, y + size, x + size, y));
                path.getElements().add(new CubicCurveTo(x + size, y - size, x - size, y + size, x - size, y));
                node = path;
                node.setFill(Color.WHITE);
                break;
            case CONFIDENTIAL_NORMAL:
                size = 10;
                node = new Circle(x, y, size / 2.0);
                node.setFill(Color.MIDNIGHTBLUE);
                node.setStroke(Color.LIGHTBLUE);
                node.setStrokeWidth(1.5);
                break;
            case CONFIDENTIAL_DISTANCING:
                size = 12;
                node = new Circle(x, y, size / 2.0);
                node.setFill(Color.DARKSLATEGRAY);
                node.setStroke(Color.CYAN);
                node.setStrokeWidth(1.5);
                break;

            case LARGE_8:
            case LARGE_10:
                size = 14;
                Polygon hexagon = new Polygon();
                for (int i = 0; i < 6; i++) {
                    hexagon.getPoints().addAll(
                            x + size * 0.5 * Math.cos(Math.toRadians(i * 60 + 30)),
                            y + size * 0.5 * Math.sin(Math.toRadians(i * 60 + 30))
                    );
                }
                node = hexagon;
                node.setFill(packet.getType() == PacketType.LARGE_8 ? Color.CORNFLOWERBLUE : Color.DEEPSKYBLUE);
                break;

            case BIT_PACKET:
                size = 5;
                node = new Circle(x, y, size / 2.0);
                node.setFill((packet.getVisualColor() != null) ? packet.getVisualColor() : Color.YELLOW);
                break;

            default:
                node = new Circle(x, y, 5);
                node.setFill(Color.WHITE);
                break;
        }

        if (node.getStroke() == null) node.setStroke(Color.DARKSLATEGRAY);
        if (node.getStrokeWidth() == 0) node.setStrokeWidth(0.5);
        return node;
    }

    public Node getNodeForPacket(Packet packet) {
        return packetNodes.get(packet);
    }

    public void addNode(Node node) {
        if (node != null && !this.getChildren().contains(node)) {
            if (node instanceof Line) { this.getChildren().add(0, node); }
            else { this.getChildren().add(node); }
        }
    }

    public void removePacketNode(Packet packet) {
        if (packet != null) {
            Node nodeToRemove = packetNodes.remove(packet);
            if (nodeToRemove != null) {
                this.getChildren().remove(nodeToRemove);
            }
        }
    }

    private void drawEffectPoints(List<EffectPoint> effectPoints) {
        for (EffectPoint effect : effectPoints) {
            Circle effectCircle = new Circle(effect.getPosition().getX(), effect.getPosition().getY(), 6);

            if (effect.getType() == EffectType.AERGIA_NO_ACCELERATION) {
                effectCircle.setFill(Color.rgb(173, 216, 230, 0.5));
                effectCircle.setStroke(Color.WHITE);
                effectCircle.getStrokeDashArray().addAll(5.0, 5.0);
            }
            else if (effect.getType() == EffectType.ELIPHAS_CENTERING_FORCE) {
                effectCircle.setFill(Color.rgb(144, 238, 144, 0.5));
                effectCircle.setStroke(Color.GREENYELLOW);
                effectCircle.getStrokeDashArray().addAll(2.0, 6.0);
            }

            effectCircle.setMouseTransparent(true);
            this.getChildren().add(effectCircle);
        }
    }

    public void removeNode(Node node) {
        if (node != null) { this.getChildren().remove(node); }
    }

    public void updatePacketPositions(GameState gameState) {
        for (Packet packet : gameState.getPackets()) {
            Node node = packetNodes.get(packet);
            Label label = packet.getSizeLabel();

            if (node != null) {
                node.relocate(packet.getX() - node.getBoundsInLocal().getWidth() / 2.0,
                        packet.getY() - node.getBoundsInLocal().getHeight() / 2.0);
                if (label != null) {
                    packet.updateLabelPosition();
                }
            } else {
                drawSinglePacket(packet);
            }
        }
        List<Packet> currentPackets = gameState.getPackets();
        packetNodes.entrySet().removeIf(entry -> {
            if (!currentPackets.contains(entry.getKey())) {
                this.getChildren().remove(entry.getValue());
                Label labelToRemove = entry.getKey().getSizeLabel();
                if (labelToRemove != null) {
                    this.getChildren().remove(labelToRemove);
                }
                return true;
            }
            return false;
        });
    }
}