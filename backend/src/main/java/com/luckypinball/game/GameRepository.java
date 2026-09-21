package com.luckypinball.game;

import java.util.Optional;

public interface GameRepository {
    GameSession save(GameSession session);

    Optional<GameSession> findById(String gameId);

    java.util.List<GameSession> findAll();
}
