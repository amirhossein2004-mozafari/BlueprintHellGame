package com.api.dto;

import javafx.scene.paint.Color;
import java.util.UUID;

public class WireDTO {
    private UUID id;
    private Color color;
    private UUID sourceId;
    private UUID destinationId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }

    public UUID getDestinationId() { return destinationId; }
    public void setDestinationId(UUID destinationId) { this.destinationId = destinationId; }
}