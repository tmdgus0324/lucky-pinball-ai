package com.luckypinball.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/api/player")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse register(@Valid @RequestBody RegisterPlayerRequest request) {
        PlayerEntity player = playerService.register(request.name(), request.birthDate());
        return PlayerResponse.from(player);
    }

    public record RegisterPlayerRequest(
            @NotBlank String name,
            @NotNull LocalDate birthDate
    ) {
    }

    public record PlayerResponse(Long playerId, String name, LocalDate birthDate) {
        static PlayerResponse from(PlayerEntity player) {
            return new PlayerResponse(player.getId(), player.getName(), player.getBirthDate());
        }
    }
}
