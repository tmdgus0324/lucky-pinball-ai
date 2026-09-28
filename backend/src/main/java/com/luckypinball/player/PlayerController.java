package com.luckypinball.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
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
