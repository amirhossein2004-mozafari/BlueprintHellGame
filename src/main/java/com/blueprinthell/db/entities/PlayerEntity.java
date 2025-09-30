package com.blueprinthell.db.entities;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "players")
public class PlayerEntity {

    private String username;
    private List<GameResultEntity> gameResults = new ArrayList<>();

    @Id
    @Column(unique = true, nullable = false)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<GameResultEntity> getGameResults() {
        return gameResults;
    }

    public void setGameResults(List<GameResultEntity> gameResults) {
        this.gameResults = gameResults;
    }
}