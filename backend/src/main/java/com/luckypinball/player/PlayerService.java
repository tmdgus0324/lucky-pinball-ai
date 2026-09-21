package com.luckypinball.player;

import com.luckypinball.common.ApiException;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

    private final PlayerJpaRepository playerJpaRepository;

    public PlayerService(PlayerJpaRepository playerJpaRepository) {
        this.playerJpaRepository = playerJpaRepository;
    }

    public PlayerEntity register(String name, LocalDate birthDate) {
        return playerJpaRepository.save(new PlayerEntity(name, birthDate));
    }

    public PlayerEntity getById(Long playerId) {
        return playerJpaRepository.findById(playerId)
                .orElseThrow(() -> ApiException.notFound("Player not found: " + playerId));
    }
}
