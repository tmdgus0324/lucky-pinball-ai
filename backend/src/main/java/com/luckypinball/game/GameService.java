package com.luckypinball.game;

import com.luckypinball.common.ApiException;
import com.luckypinball.fortune.FortuneQueryService;
import com.luckypinball.fortune.FortuneQueryService.FortuneQueryResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    private static final int MIN_PARTICIPANTS = 2;
    private static final int MAX_PARTICIPANTS = 8;

    private final GameRepository gameRepository;
    private final FortuneQueryService fortuneQueryService;

    public GameService(GameRepository gameRepository, FortuneQueryService fortuneQueryService) {
        this.gameRepository = gameRepository;
        this.fortuneQueryService = fortuneQueryService;
    }

    public GameSession createGame(List<Long> playerIds) {
        if (playerIds == null || playerIds.size() < MIN_PARTICIPANTS || playerIds.size() > MAX_PARTICIPANTS) {
            throw ApiException.badRequest(
                    "playerIds must contain " + MIN_PARTICIPANTS + " to " + MAX_PARTICIPANTS + " players");
        }

        List<GameParticipant> participants = playerIds.stream()
                .map(this::toParticipant)
                .collect(Collectors.toList());

        GameSession session = new GameSession(UUID.randomUUID().toString(), participants);
        return gameRepository.save(session);
    }

    public GameSession start(String gameId) {
        GameSession session = getSessionOrThrow(gameId);
        session.start();
        return session;
    }

    public GameResultView reportResult(String gameId, List<Long> finishOrder) {
        GameSession session = getSessionOrThrow(gameId);

        if (session.hasResult()) {
            throw ApiException.conflict("Game already has a reported result: " + gameId);
        }

        Set<Long> expected = session.getParticipants().stream()
                .map(GameParticipant::playerId)
                .collect(Collectors.toSet());
        Set<Long> actual = finishOrder == null ? Set.of() : Set.copyOf(finishOrder);

        if (finishOrder == null || finishOrder.size() != expected.size() || !actual.equals(expected)) {
            throw ApiException.badRequest("finishOrder must contain each participant exactly once");
        }

        Map<Long, String> namesByPlayerId = session.getParticipants().stream()
                .collect(Collectors.toMap(GameParticipant::playerId, GameParticipant::name));

        List<RankEntry> ranking = new ArrayList<>();
        for (int i = 0; i < finishOrder.size(); i++) {
            Long playerId = finishOrder.get(i);
            ranking.add(new RankEntry(i + 1, playerId, namesByPlayerId.get(playerId)));
        }

        String selectedName = namesByPlayerId.get(finishOrder.get(finishOrder.size() - 1));
        session.reportResult(ranking, selectedName);

        return toView(session);
    }

    public GameResultView getResult(String gameId) {
        GameSession session = getSessionOrThrow(gameId);
        if (!session.hasResult()) {
            throw ApiException.notFound("Game result not available yet: " + gameId);
        }
        return toView(session);
    }

    public List<GameSession> findAll() {
        return gameRepository.findAll();
    }

    private GameParticipant toParticipant(Long playerId) {
        FortuneQueryResult fortune = fortuneQueryService.getTodayFortune(playerId);
        return new GameParticipant(
                fortune.playerId(), fortune.name(), fortune.fortuneScore(),
                fortune.luckyNumber(), fortune.fortuneMessage(), fortune.buff());
    }

    private GameSession getSessionOrThrow(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> ApiException.notFound("Game not found: " + gameId));
    }

    private GameResultView toView(GameSession session) {
        Instant createdAt = session.getFinishedAt() != null ? session.getFinishedAt() : session.getCreatedAt();
        return new GameResultView(
                session.getGameId(), session.getRanking(), session.getSelectedName(),
                session.getParticipants().size(), createdAt);
    }
}
