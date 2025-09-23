package com.api.service;

import com.api.dto.StatusDTO;
import com.api.dto.SystemDTO;
import com.api.enums.AbilityType;
import com.model.GameState;
import com.model.SystemNode;
import com.utils.SerializationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameService {

    private static Map<UUID, GameState> activeGames;

    public GameService(Map<UUID, GameState> games) {
        activeGames = games;
    }

    public StatusDTO initializeGame() {
        GameState newGame = new GameState(1, 50, 180);
        UUID gameId = UUID.randomUUID();

        activeGames.put(gameId, newGame);

        System.out.println("New game initialized with ID: " + gameId);

        return new StatusDTO(200, gameId);
    }

    public StatusDTO getAllSystems(UUID gameId) {
        GameState game = activeGames.get(gameId);
        if (game == null) {
            return new StatusDTO(404, "Game not found with ID: " + gameId);
        }

        List<SystemDTO> systemDTOs = new ArrayList<>();
        for (SystemNode systemNode : game.getSystems()) {
            systemDTOs.add(SerializationManager.toDTO(systemNode));
        }

        return new StatusDTO(200, systemDTOs);
    }


    public StatusDTO findPacket(UUID gameId, UUID packetId) {
        return null;
    }

}