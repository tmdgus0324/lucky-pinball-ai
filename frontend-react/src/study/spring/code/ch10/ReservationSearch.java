package com.example.reservation.reservation;

import java.time.LocalDate;
import java.util.List;

// 검색 조건. 값이 있는 조건만 WHERE에 붙인다(동적 SQL).
public record ReservationSearch(String guestName, List<Long> roomIds, LocalDate from, String status) {
}
