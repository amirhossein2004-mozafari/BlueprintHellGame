package com.utils;

import com.api.dto.PacketDTO;
import com.api.dto.PortDTO;
import com.api.dto.SystemDTO;
import com.api.enums.PacketType;
import com.api.enums.PortType;
import com.api.enums.SystemType;
import com.model.Packet;
import com.model.Port;
import com.model.SystemNode;
import com.model.SystemRole;
import javafx.geometry.Point2D;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SerializationManager {


    public static SystemDTO toDTO(SystemNode system) {
        if (system == null) return null;
        SystemDTO dto = new SystemDTO();
        dto.setId(system.getId());
        dto.setSystemType(mapSystemRole(system.getRole()));
        dto.setPosition(new Point2D(system.getX(), system.getY()));

        List<PortDTO> inputPortDTOs = new ArrayList<>();
        for (Port port : system.getInputPorts()) {
            inputPortDTOs.add(toDTO(port));
        }
        dto.setInputPorts(inputPortDTOs);

        List<PortDTO> outputPortDTOs = new ArrayList<>();
        for (Port port : system.getOutputPorts()) {
            outputPortDTOs.add(toDTO(port));
        }
        dto.setOutputPorts(outputPortDTOs);
        return dto;
    }

    public static PortDTO toDTO(Port port) {
        if (port == null) return null;
        PortDTO dto = new PortDTO();
        dto.setId(port.getId());
        dto.setParentSystemId(port.getParentSystem().getId());
        dto.setInput(port.getType() == com.model.PortType.INPUT);
        dto.setPortType(mapPacketTypeToPortType(port.getPacketType()));
        return dto;
    }


    public static SystemNode fromDTO(SystemDTO dto) {
        if (dto == null) return null;

        double x = dto.getPosition().getX();
        double y = dto.getPosition().getY();
        SystemRole role = mapSystemType(dto.getSystemType());

        SystemNode node = new SystemNode(x, y, role);

        node.setId(dto.getId());

        return node;
    }

    public static String toJson(List<SystemDTO> systemDTOs) {
        JSONArray jsonArray = new JSONArray();
        for (SystemDTO dto : systemDTOs) {
            JSONObject sysJson = new JSONObject();
            sysJson.put("id", dto.getId().toString());
            sysJson.put("systemType", dto.getSystemType().name());

            sysJson.put("x", dto.getPosition().getX());
            sysJson.put("y", dto.getPosition().getY());


            jsonArray.put(sysJson);
        }
        return jsonArray.toString();
    }

    public static List<SystemDTO> fromJsonToSystemDTOs(String json) {
        List<SystemDTO> dtoList = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject sysJson = jsonArray.getJSONObject(i);
            SystemDTO dto = new SystemDTO();
            dto.setId(UUID.fromString(sysJson.getString("id")));
            dto.setSystemType(SystemType.valueOf(sysJson.getString("systemType")));
            dto.setPosition(new Point2D(sysJson.getDouble("x"), sysJson.getDouble("y")));
            dtoList.add(dto);
        }
        return dtoList;
    }


    public static SystemType mapSystemRole(SystemRole role) {
        return SystemType.valueOf(role.name());
    }

    public static SystemRole mapSystemType(SystemType type) {
        return SystemRole.valueOf(type.name());
    }

    public static PortType mapPacketTypeToPortType(com.model.PacketType packetType) {
        return switch (packetType) {
            case SQUARE, BIT_PACKET -> PortType.SQUARE;
            case TRIANGLE -> PortType.TRIANGLE;
            default -> PortType.SQUARE;
        };
    }
}