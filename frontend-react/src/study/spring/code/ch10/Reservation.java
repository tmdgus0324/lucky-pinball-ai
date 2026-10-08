package com.example.reservation.reservation;

import java.time.LocalDate;

// DB 한 줄을 담는 객체. MyBatis는 기본 생성자로 만든 뒤 setter로 값을 채운다.
// (레거시 코드에서는 ReservationVO, ReservationDTO 같은 이름을 많이 쓴다)
public class Reservation {

    private Long id;
    private Long roomId;
    private String guestName;
    private LocalDate checkIn;
    private int nights;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
