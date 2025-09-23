package com.api.dto;

import com.api.enums.PortType;
import java.util.UUID;

public class PortDTO {

    private UUID id;
    private PortType portType;
    private UUID parentSystemId;
    private boolean isInput;

    public PortDTO() {
    }


    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }

    public PortType getPortType() {
        return portType;
    }
    public void setPortType(PortType portType) {
        this.portType = portType;
    }

    public UUID getParentSystemId() {
        return parentSystemId;
    }
    public void setParentSystemId(UUID parentSystemId) {
        this.parentSystemId = parentSystemId;
    }

    public boolean isInput() {
        return isInput;
    }
    public void setInput(boolean isInput) {
        this.isInput = isInput;
    }
}