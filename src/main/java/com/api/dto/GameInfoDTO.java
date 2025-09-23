package com.api.dto;

import java.util.UUID;

public class GameInfoDTO {
    private UUID id;
    private long spentTime;

    public GameInfoDTO(long spentTime) {
        this.spentTime = spentTime;
    }

    public UUID getId(){ return id; }
    public long getSpentTime() {
        return spentTime;
    }
}