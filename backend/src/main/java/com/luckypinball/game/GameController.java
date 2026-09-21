package com.luckypinball.game;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/api/game/create")
    @ResponseStatus(HttpStatus.CREATED)
    public GameCreateResponse create(@Valid @RequestBody GameCreateRequest request) {
        GameSession session = gameService.createGame(request.playerIds());
        return GameCreateResponse.from(session);
    }

    @PostMapping("/api/game/start")
    public GameStartResponse start(@Valid @RequestBody GameIdRequest request) {
        GameSession session = gameService.start(request.gameId());
        return new GameStartResponse(session.getGameId(), session.getStatus().name(), session.getStartedAt());
    }

    @PostMapping("/api/game/result")
    public GameResultView reportResult(@Valid @RequestBody GameResultRequest request) {
        return gameService.reportResult(request.gameId(), request.finishOrder());
    }

    @GetMapping("/api/game/result/{gameId}")
    public GameResultView getResult(@PathVariable String gameId) {
        return gameService.getResult(gameId);
    }

    public record GameCreateRequest(@NotEmpty List<Long> playerIds) {
    }

    public record GameIdRequest(@NotNull String gameId) {
    }

    public record GameResultRequest(@NotNull String gameId, @NotEmpty List<Long> finishOrder) {
    }

    public record GameStartResponse(String gameId, String status, Instant startedAt) {
    }

    public record GameCreateResponse(String gameId, List<GameParticipant> participants) {
        static GameCreateResponse from(GameSession session) {
            return new GameCreateResponse(session.getGameId(), session.getParticipants());
        }
    }
}
