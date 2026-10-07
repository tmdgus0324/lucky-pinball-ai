package com.example.reservation.reservation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateReservationRequest(
        @NotNull(message = "객실을 선택해 주세요") Long roomId,
        @NotBlank(message = "예약자 이름을 입력해 주세요") String guestName,
        @NotNull(message = "체크인 날짜를 입력해 주세요") LocalDate checkIn,
        @Min(value = 1, message = "1박 이상이어야 합니다") @Max(value = 14, message = "14박까지 예약할 수 있습니다") int nights) {
}
