package com.example.reservation.reservation;

import java.time.LocalDate;

// 저장된 예약 하나. 응답 JSON의 모양이 된다.
public record Reservation(Long id, Long roomId, String guestName, LocalDate checkIn, int nights) {
}
