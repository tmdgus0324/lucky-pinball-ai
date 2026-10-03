package com.luckypinball.game;

import com.luckypinball.common.ApiException;
import com.luckypinball.fortune.BuffCalculator;
import com.luckypinball.fortune.BuffCalculator.Buff;
import com.luckypinball.fortune.FortuneQueryService;
import com.luckypinball.fortune.FortuneQueryService.FortuneQueryResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

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
        participants = applyRelativeStartY(participants);

        GameSession session = new GameSession(UUID.randomUUID().toString(), participants);
        GameSession saved = gameRepository.save(session);
        log.info("게임 생성: gameId={} 참가자={}명", saved.getGameId(), participants.size());
        return saved;
    }

    /**
     * 개별 조회(POST /api/fortune)는 참가자 한 명씩 독립 호출이라 상대평가를 할 수 없다 —
     * 이번 판 전체 참가자의 점수를 다 알 수 있는 지점(게임 생성 시점)에서만 계산 가능하다.
     * (devhelp/26 참고 — AI 점수가 좁은 범위에 몰려도 시작 높이 차이가 항상 드러나게 함)
     */
    private List<GameParticipant> applyRelativeStartY(List<GameParticipant> participants) {
        OptionalInt maxScore = participants.stream()
                .filter(p -> p.fortuneScore() != null)
                .mapToInt(GameParticipant::fortuneScore)
                .max();
        if (maxScore.isEmpty()) {
            return participants; // 아무도 실제 점수가 없으면(전원 생년월일 미입력) 그대로 둔다
        }

        return participants.stream()
                .map(p -> {
                    if (p.fortuneScore() == null) {
                        return p; // 생년월일 미입력(NONE)은 상대 비교 대상이 아니다 — 기존 Buff(0,0) 유지
                    }
                    int startY = BuffCalculator.relativeStartY(p.fortuneScore(), maxScore.getAsInt());
                    Buff relativeBuff = new Buff(p.buff().tier(), startY);
                    return new GameParticipant(
                            p.playerId(), p.name(), p.fortuneScore(), p.luckyNumber(), p.fortuneMessage(), relativeBuff);
                })
                .collect(Collectors.toList());
    }

    public GameSession start(String gameId) {
        GameSession session = getSessionOrThrow(gameId);
        session.start();
        log.info("게임 시작: gameId={}", gameId);
        return gameRepository.save(session);
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
        session = gameRepository.save(session);
        // 당첨자는 이름 대신 playerId로 남긴다(이름은 사용자가 입력한 값).
        log.info("게임 결과 확정: gameId={} 당첨 playerId={} 참가자={}명",
                gameId, finishOrder.get(finishOrder.size() - 1), finishOrder.size());

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
