package com.luckypinball.player;

import com.luckypinball.fortune.FortuneResultEntity;
import com.luckypinball.fortune.FortuneResultJpaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerJpaRepository playerJpaRepository;
    private final FortuneResultJpaRepository fortuneResultJpaRepository;

    public PlayerController(PlayerService playerService,
                             PlayerJpaRepository playerJpaRepository,
                             FortuneResultJpaRepository fortuneResultJpaRepository) {
        this.playerService = playerService;
        this.playerJpaRepository = playerJpaRepository;
        this.fortuneResultJpaRepository = fortuneResultJpaRepository;
    }

    @PostMapping("/api/player")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse register(@Valid @RequestBody RegisterPlayerRequest request) {
        PlayerEntity player = playerService.register(request.name(), request.birthDate());
        return PlayerResponse.from(player);
    }

    /**
     * "DB TEST" 버튼이 재사용할 기존 신원(이름+생년월일)을 고르는 용도의 공개 조회.
     * 관리자 전용인 {@code GET /api/admin/players}와는 의도적으로 분리했다 — 그쪽은 로그인이
     * 필요해졌는데, 이 조회는 로그인 안 한 일반 참가자도 데모(DB TEST)에서 써야 하기 때문이다.
     * 그래서 노출 필드도 DB TEST에 필요한 최소한(이름·생년월일·운세 출처)으로만 좁혔다.
     */
    @GetMapping("/api/players/reusable")
    public List<ReusablePlayerView> reusablePlayers() {
        return playerJpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(player -> player.getBirthDate() != null)
                .map(player -> {
                    List<FortuneResultEntity> history = fortuneResultJpaRepository
                            .findByPlayerIdOrderByCreatedDateAsc(player.getId());
                    String fortuneSource = history.stream()
                            .anyMatch(h -> FortuneResultEntity.SOURCE_AI.equals(h.getSource()))
                            ? FortuneResultEntity.SOURCE_AI
                            : history.stream().anyMatch(h -> FortuneResultEntity.SOURCE_CACHE.equals(h.getSource()))
                                    ? FortuneResultEntity.SOURCE_CACHE
                                    : null;
                    return new ReusablePlayerView(player.getId(), player.getName(), player.getBirthDate(), fortuneSource);
                })
                .filter(view -> view.fortuneSource() != null)
                .toList();
    }

    public record ReusablePlayerView(Long playerId, String name, LocalDate birthDate, String fortuneSource) {
    }

    public record RegisterPlayerRequest(
            // 길이 제한 없이 받으면 화면 레이아웃이 깨지고, Claude 프롬프트에도 그대로 들어간다.
            @NotBlank @Size(max = 20) String name,
            // 생년월일은 선택사항 — 없으면 버프 없이 참여(AI 호출 안 함). 입력하면 미래 날짜는 막는다.
            @Past LocalDate birthDate
    ) {
    }

    public record PlayerResponse(Long playerId, String name, LocalDate birthDate) {
        static PlayerResponse from(PlayerEntity player) {
            return new PlayerResponse(player.getId(), player.getName(), player.getBirthDate());
        }
    }
}
