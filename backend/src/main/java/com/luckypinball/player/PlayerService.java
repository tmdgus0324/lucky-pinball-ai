package com.luckypinball.player;

import com.luckypinball.common.ApiException;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    private final PlayerJpaRepository playerJpaRepository;

    public PlayerService(PlayerJpaRepository playerJpaRepository) {
        this.playerJpaRepository = playerJpaRepository;
    }

    public PlayerEntity register(String name, LocalDate birthDate) {
        PlayerEntity saved = playerJpaRepository.save(new PlayerEntity(name, birthDate));
        // 이름·생년월일은 개인정보라 남기지 않고, 운세 대상인지(생년월일 입력 여부)만 남긴다.
        log.info("참가자 등록: playerId={} 생년월일입력={}", saved.getId(), birthDate != null);
        return saved;
    }

    public PlayerEntity getById(Long playerId) {
        return playerJpaRepository.findById(playerId)
                .orElseThrow(() -> ApiException.notFound("Player not found: " + playerId));
    }
}
