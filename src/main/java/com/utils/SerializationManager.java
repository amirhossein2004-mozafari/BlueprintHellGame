package com.utils;

import com.api.dto.PacketDTO;
import com.api.dto.PortDTO;
import com.api.dto.SystemDTO;
import com.api.dto.WireDTO;
import com.api.enums.PacketType;
import com.api.enums.PortType;
import com.api.enums.SystemType;
import com.model.*;
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

    public static String toJsonPackets(List<PacketDTO> packetDTOs) {
        JSONArray jsonArray = new JSONArray();
        for (PacketDTO dto : packetDTOs) {
            JSONObject pJson = new JSONObject();
            pJson.put("id", dto.getId().toString());
            pJson.put("packetType", dto.getPacketType().name());
            pJson.put("x", dto.getX());
            pJson.put("y", dto.getY());
            jsonArray.put(pJson);
        }
        return jsonArray.toString();
    }

    public static List<PacketDTO> fromJsonToPacketDTOs(String json) {
        List<PacketDTO> dtoList = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject pJson = jsonArray.getJSONObject(i);
            PacketDTO dto = new PacketDTO();
            dto.setId(UUID.fromString(pJson.getString("id")));
            dto.setPacketType(PacketType.valueOf(pJson.getString("packetType")));
            dto.setPosition(new Point2D(pJson.getDouble("x"), pJson.getDouble("y")));
            dtoList.add(dto);
        }
        return dtoList;
    }


    public static Packet fromDTO(PacketDTO dto) {
        if (dto == null) return null;
        com.model.PacketType modelType = mapApiPacketTypeToGamePacketType(dto.getPacketType());

        Packet packet = new Packet(modelType, dto.getX(), dto.getY());
        packet.setId(dto.getId());

        return packet;
    }

    public static com.model.PacketType mapApiPacketTypeToGamePacketType(com.api.enums.PacketType apiType) {
        return com.model.PacketType.valueOf(apiType.name());
    }


    public static PacketDTO toDTO(Packet packet) {
        if (packet == null) return null;
        PacketDTO dto = new PacketDTO();
        dto.setId(packet.getId());
        dto.setPacketType(mapGamePacketTypeToApiPacketType(packet.getType()));
        dto.setPosition(new Point2D(packet.getX(), packet.getY()));
        return dto;
    }

    public static com.api.enums.PacketType mapGamePacketTypeToApiPacketType(com.model.PacketType modelType) {
        try {
            return com.api.enums.PacketType.valueOf(modelType.name());
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: No matching API PacketType for model type: " + modelType.name());
            return com.api.enums.PacketType.SQUARE;
        }
    }


    public static WireDTO toDTO(Connection connection) {
        if (connection == null) return null;
        WireDTO dto = new WireDTO();
        dto.setId(connection.getId());
        dto.setSourceId(connection.getStartPort().getId());
        dto.setDestinationId(connection.getEndPort().getId());
        dto.setColor(javafx.scene.paint.Color.WHITE);
        return dto;
    }

    public static String toJsonWires(List<WireDTO> wireDTOs) {
        JSONArray jsonArray = new JSONArray();
        for (WireDTO dto : wireDTOs) {
            JSONObject wJson = new JSONObject();
            wJson.put("id", dto.getId().toString());
            wJson.put("sourceId", dto.getSourceId().toString());
            wJson.put("destinationId", dto.getDestinationId().toString());
            jsonArray.put(wJson);
        }
        return jsonArray.toString();
    }

    public static List<WireDTO> fromJsonToWireDTOs(String json) {
        List<WireDTO> dtoList = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject wJson = jsonArray.getJSONObject(i);
            WireDTO dto = new WireDTO();
            dto.setId(UUID.fromString(wJson.getString("id")));
            dto.setSourceId(UUID.fromString(wJson.getString("sourceId")));
            dto.setDestinationId(UUID.fromString(wJson.getString("destinationId")));
            dtoList.add(dto);
        }
        return dtoList;
    }



}