package com.example.reservation.reservation;

public record Reservation(Long id, String guestName, String roomName, int nights, String status) {
}
