package com.luckypinball.admin;

import com.luckypinball.common.ApiException;
import com.luckypinball.common.ErrorLogEntry;
import com.luckypinball.common.ErrorLogStore;
import com.luckypinball.fortune.FortuneResultEntity;
import com.luckypinball.fortune.FortuneResultJpaRepository;
import com.luckypinball.game.GameRepository;
import com.luckypinball.game.GameSession;
import com.luckypinball.player.PlayerEntity;
import com.luckypinball.player.PlayerJpaRepository;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 화면 골격 API — 조회 기능은 실제로 동작하고, 가중치 임의 조정은 스텁(501)이다.
 * 인증 없음(MVP 한정) — 실제 공개 배포 전에는 반드시 최소 인증을 추가해야 한다.
 */
@RestController
public class AdminController {

    private final PlayerJpaRepository playerJpaRepository;
    private final FortuneResultJpaRepository fortuneResultJpaRepository;
    private final GameRepository gameRepository;
    private final ErrorLogStore errorLogStore;

    public AdminController(PlayerJpaRepository playerJpaRepository,
                            FortuneResultJpaRepository fortuneResultJpaRepository,
                            GameRepository gameRepository,
                            ErrorLogStore errorLogStore) {
        this.playerJpaRepository = playerJpaRepository;
        this.fortuneResultJpaRepository = fortuneResultJpaRepository;
        this.gameRepository = gameRepository;
        this.errorLogStore = errorLogStore;
    }

    @GetMapping("/api/admin/players")
    public List<PlayerAdminView> players() {
        // 가장 최근에 등록한 참가자가 먼저 보이도록 정렬 (관리자가 방금 등록한 참가자를 바로 확인할 수 있게)
        return playerJpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toPlayerAdminView)
                .toList();
    }

    @GetMapping("/api/admin/games")
    public List<GameAdminView> games() {
        return gameRepository.findAll().stream()
                .map(GameAdminView::from)
                .toList();
    }

    @GetMapping("/api/admin/logs")
    public List<ErrorLogEntry> logs() {
        return errorLogStore.recent();
    }

    @PostMapping("/api/admin/fortune/override")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public void overrideFortune(@RequestBody FortuneOverrideRequest request) {
        throw ApiException.notImplemented("Not implemented yet — planned for Phase 2");
    }

    private PlayerAdminView toPlayerAdminView(PlayerEntity player) {
        List<FortuneHistoryEntry> history = fortuneResultJpaRepository
                .findByPlayerIdOrderByCreatedDateAsc(player.getId()).stream()
                .map(FortuneHistoryEntry::from)
                .toList();
        // 이 참가자에서 실제로 Claude를 호출한 적이 있는지(AI) 없으면 캐시만 썼는지(CACHE)를
        // 목록 컬럼용으로 뽑아둔다. "마지막 조회"가 아니라 "한 번이라도 AI였는지"를 보는 이유:
        // 운세 확인 때 AI를 호출해도, 게임 시작 시 GameService가 운세를 다시 조회하면서 항상
        // 캐시 행이 뒤에 붙기 때문에 마지막 행만 보면 게임을 한 번 돌린 뒤에는 늘 CACHE가 된다.
        // 출처(source) 컬럼이 생기기 전(Mock 시절)에 쌓인 옛 이력은 source가 null이다 — 이런 행은
        // 이름+생년월일도 비어 있어서 캐시 조회에 걸리지 않으므로 AI/CACHE 어느 쪽으로도 세지 않는다.
        String fortuneSource = history.stream().anyMatch(h -> FortuneResultEntity.SOURCE_AI.equals(h.source()))
                ? FortuneResultEntity.SOURCE_AI
                : history.stream().anyMatch(h -> FortuneResultEntity.SOURCE_CACHE.equals(h.source()))
                        ? FortuneResultEntity.SOURCE_CACHE
                        : null;
        return new PlayerAdminView(player.getId(), player.getName(), player.getBirthDate(), history, fortuneSource);
    }

    public record FortuneOverrideRequest(@NotNull Long playerId, @NotNull Integer overrideScore) {
    }

    public record FortuneHistoryEntry(int fortuneScore, LocalDate createdDate, String source) {
        static FortuneHistoryEntry from(FortuneResultEntity entity) {
            return new FortuneHistoryEntry(entity.getFortuneScore(), entity.getCreatedDate(), entity.getSource());
        }
    }

    /** fortuneSource: "AI"(AI 호출한 적 있음) | "CACHE"(기존 데이터만 사용) | null(아직 운세를 확인한 적 없음). */
    public record PlayerAdminView(Long playerId, String name, LocalDate birthDate, List<FortuneHistoryEntry> fortuneHistory, String fortuneSource) {
    }

    public record GameAdminView(String gameId, String selectedName, int participantCount, Instant createdAt) {
        static GameAdminView from(GameSession session) {
            Instant createdAt = session.getFinishedAt() != null ? session.getFinishedAt() : session.getCreatedAt();
            return new GameAdminView(session.getGameId(), session.getSelectedName(), session.getParticipants().size(), createdAt);
        }
    }
}
