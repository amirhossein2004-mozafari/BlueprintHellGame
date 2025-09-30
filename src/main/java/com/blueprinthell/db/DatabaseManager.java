package com.blueprinthell.db;

import com.blueprinthell.db.entities.GameResultEntity;
import com.blueprinthell.db.entities.PlayerEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Date;


public class DatabaseManager {
    private static EntityManagerFactory emf;

    public static void initialize() {
        if (emf == null) {
            emf = Persistence.createEntityManagerFactory("BlueprintHellPU");
        }
    }

    public static void saveGameResult(String username, int coins, double packetLoss) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            PlayerEntity player = em.find(PlayerEntity.class, username);
            if (player == null) {
                player = new PlayerEntity();
                player.setUsername(username);
                em.persist(player);
            }

            GameResultEntity result = new GameResultEntity();
            result.setPlayer(player);
            result.setFinalCoins(coins);
            result.setPacketLossPercentage(packetLoss);
            result.setPlayDate(new Date());

            em.persist(result);
            em.getTransaction().commit();
            System.out.println("DatabaseManager: Successfully saved result for player " + username);

        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public static void shutdown() {
        if (emf != null) {
            emf.close();
        }
    }
}