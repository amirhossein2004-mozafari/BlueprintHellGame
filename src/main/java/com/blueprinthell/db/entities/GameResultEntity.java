
package com.blueprinthell.db.entities;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "game_results")
public class GameResultEntity {

    private Long id;
    private PlayerEntity player;
    private int finalCoins;
    private double packetLossPercentage;
    private Date playDate;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "username")
    public PlayerEntity getPlayer() {
        return player;
    }
    public void setPlayer(PlayerEntity player) {
        this.player = player;
    }

    public int getFinalCoins() {
        return finalCoins;
    }
    public void setFinalCoins(int finalCoins) {
        this.finalCoins = finalCoins;
    }

    public double getPacketLossPercentage() {
        return packetLossPercentage;
    }
    public void setPacketLossPercentage(double packetLossPercentage) {
        this.packetLossPercentage = packetLossPercentage;
    }

    public Date getPlayDate() {
        return playDate;
    }
    public void setPlayDate(Date playDate) {
        this.playDate = playDate;
    }
}
