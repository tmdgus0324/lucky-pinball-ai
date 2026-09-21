package com.luckypinball.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryGameRepository implements GameRepository {

    private final ConcurrentHashMap<String, GameSession> sessions = new ConcurrentHashMap<>();

    @Override
    public GameSession save(GameSession session) {
        sessions.put(session.getGameId(), session);
        return session;
    }

    @Override
    public Optional<GameSession> findById(String gameId) {
        return Optional.ofNullable(sessions.get(gameId));
    }

    @Override
    public List<GameSession> findAll() {
        return new ArrayList<>(sessions.values());
    }
}
