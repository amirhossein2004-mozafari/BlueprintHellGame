package com.api.dto;

import java.io.Serializable;

public class LeaderboardEntryDTO implements Serializable {
    private String username;
    private int score;
    private double packetLoss;
    private String playedAt;

    public LeaderboardEntryDTO(String username, int score, double packetLoss, String playedAt) {
        this.username = username;
        this.score = score;
        this.packetLoss = packetLoss;
        this.playedAt = playedAt;
    }

    public String getUsername() { return username; }
    public int getScore() { return score; }
    public double getPacketLoss() { return packetLoss; }
    public String getPlayedAt() { return playedAt; }
}