package com.example.reservation.reservation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// 필드마다 규칙을 애너테이션으로 붙인다. 컨트롤러에서 @Valid를 붙이면 이 규칙으로 검사한다.
public record CreateReservationRequest(
        @NotNull
        Long roomId,

        // message를 주면 그 문구를 쓰고, 안 주면 기본 문구를 쓴다.
        @NotBlank(message = "예약자 이름을 입력해 주세요")
        @Size(max = 20, message = "예약자 이름은 20자까지입니다")
        String guestName,

        @NotNull
        @FutureOrPresent(message = "체크인은 오늘 이후여야 합니다")
        LocalDate checkIn,

        @Min(1) @Max(14)
        int nights,

        @Email   // 값이 없으면(null) 검사하지 않는다. 필수로 하려면 @NotBlank도 붙인다.
        String email) {
}
