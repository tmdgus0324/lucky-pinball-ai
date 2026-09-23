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
        return new PlayerAdminView(player.getId(), player.getName(), player.getBirthDate(), history);
    }

    public record FortuneOverrideRequest(@NotNull Long playerId, @NotNull Integer overrideScore) {
    }

    public record FortuneHistoryEntry(int fortuneScore, LocalDate createdDate) {
        static FortuneHistoryEntry from(FortuneResultEntity entity) {
            return new FortuneHistoryEntry(entity.getFortuneScore(), entity.getCreatedDate());
        }
    }

    public record PlayerAdminView(Long playerId, String name, LocalDate birthDate, List<FortuneHistoryEntry> fortuneHistory) {
    }

    public record GameAdminView(String gameId, String selectedName, int participantCount, Instant createdAt) {
        static GameAdminView from(GameSession session) {
            Instant createdAt = session.getFinishedAt() != null ? session.getFinishedAt() : session.getCreatedAt();
            return new GameAdminView(session.getGameId(), session.getSelectedName(), session.getParticipants().size(), createdAt);
        }
    }
}
