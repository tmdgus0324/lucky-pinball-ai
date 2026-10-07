package com.example.reservation.reservation;

import java.time.LocalDate;

// 예약 만들기 요청 본문(JSON)을 받는 객체. id는 서버가 정하므로 요청에는 없다.
// 저장용 객체(Reservation)와 요청용 객체를 나누면, 요청으로 받으면 안 되는 값(id 등)을 막을 수 있다.
public record CreateReservationRequest(Long roomId, String guestName, LocalDate checkIn, int nights) {
}
