package com.api.dto;

import com.api.enums.SystemType;
import javafx.geometry.Point2D;
import java.util.List;
import java.util.UUID;

public class SystemDTO {
    private UUID id;
    private SystemType systemType;
    private Point2D position;
    private List<PortDTO> inputPorts;
    private List<PortDTO> outputPorts;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public SystemType getSystemType() { return systemType; }
    public void setSystemType(SystemType systemType) { this.systemType = systemType; }

    public Point2D getPosition() { return position; }
    public void setPosition(Point2D position) { this.position = position; }

    public List<PortDTO> getInputPorts() { return inputPorts; }
    public void setInputPorts(List<PortDTO> inputPorts) { this.inputPorts = inputPorts; }

    public List<PortDTO> getOutputPorts() { return outputPorts; }
    public void setOutputPorts(List<PortDTO> outputPorts) { this.outputPorts = outputPorts; }
}