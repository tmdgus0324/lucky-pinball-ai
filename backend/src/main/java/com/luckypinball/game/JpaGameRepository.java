package com.luckypinball.game;

import com.luckypinball.fortune.BuffCalculator.Buff;
import com.luckypinball.player.PlayerJpaRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * GameRepository의 실제(영속) 구현체 — 기존 InMemoryGameRepository를 대체한다.
 * GameService는 그대로 GameSession/GameParticipant/RankEntry(평범한 도메인 객체)만 다루고,
 * 이 클래스가 그걸 GameEntity/GameParticipantEntity/GameRankEntity로 변환해 저장·복원한다.
 */
@Repository
public class JpaGameRepository implements GameRepository {

    private final GameJpaRepository gameJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;

    public JpaGameRepository(GameJpaRepository gameJpaRepository, PlayerJpaRepository playerJpaRepository) {
        this.gameJpaRepository = gameJpaRepository;
        this.playerJpaRepository = playerJpaRepository;
    }

    @Override
    @Transactional
    public GameSession save(GameSession session) {
        GameEntity entity = gameJpaRepository.findById(session.getGameId())
                .orElseGet(() -> new GameEntity(session.getGameId(), session.getCreatedAt()));

        // 참가자는 게임 생성 시점에 한 번만 채워진다 — 이후 save() 재호출(start/reportResult)에서는
        // 이미 있으니 다시 안 넣는다.
        if (entity.getParticipants().isEmpty()) {
            session.getParticipants().forEach(p -> entity.addParticipant(new GameParticipantEntity(
                    entity, playerJpaRepository.getReferenceById(p.playerId()), p.name(), p.fortuneScore(),
                    p.luckyNumber(), p.fortuneMessage(), p.buff().tier(), p.buff().startY())));
        }

        if (session.getStatus() == GameStatus.STARTED && entity.getStatus() == GameStatus.CREATED) {
            entity.start(session.getStartedAt());
        }

        if (session.hasResult() && entity.getRanks().isEmpty()) {
            session.getRanking().forEach(r -> entity.addRank(new GameRankEntity(
                    entity, playerJpaRepository.getReferenceById(r.playerId()), r.rank(), r.name())));
            entity.finish(session.getSelectedName(), session.getFinishedAt());
        }

        return toDomain(gameJpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameSession> findById(String gameId) {
        // toDomain()이 participants/ranks(LAZY)를 읽으므로, 세션이 열려있는 동안(@Transactional)
        // 변환까지 끝내야 한다 — 아니면 "no session" LazyInitializationException이 난다.
        return gameJpaRepository.findById(gameId).map(this::toDomain);
    }

    @Override
    public List<GameSession> findAll() {
        // findAllWithDetails()가 fetch join으로 이미 다 채워서 반환하므로, 여기선 트랜잭션이
        // 끝난 뒤에 변환해도 LazyInitializationException이 안 난다.
        return gameJpaRepository.findAllWithDetails().stream().map(this::toDomain).toList();
    }

    private GameSession toDomain(GameEntity entity) {
        List<GameParticipant> participants = entity.getParticipants().stream()
                .map(pe -> new GameParticipant(pe.getPlayer().getId(), pe.getName(), pe.getFortuneScore(),
                        pe.getLuckyNumber(), pe.getFortuneMessage(), new Buff(pe.getBuffTier(), pe.getBuffStartY())))
                .toList();

        List<RankEntry> ranking = entity.getRanks().isEmpty() ? null : entity.getRanks().stream()
                .map(re -> new RankEntry(re.getRank(), re.getPlayer().getId(), re.getName()))
                .sorted(Comparator.comparingInt(RankEntry::rank))
                .toList();

        return GameSession.restore(entity.getGameId(), participants, entity.getCreatedAt(), entity.getStatus(),
                entity.getStartedAt(), entity.getFinishedAt(), ranking, entity.getSelectedName());
    }
}
