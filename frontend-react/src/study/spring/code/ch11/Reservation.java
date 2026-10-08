package com.example.reservation.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

// @Entity: 이 클래스가 테이블(reservation)과 연결된다. 필드가 컬럼이 된다(roomId → room_id).
@Entity
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // DB가 번호를 매긴다(auto increment)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false, length = 50)
    private String guestName;

    private LocalDate checkIn;

    private int nights;

    @Enumerated(EnumType.STRING)   // enum을 글자("CONFIRMED")로 저장한다. 기본값(ORDINAL, 숫자)은 쓰지 않는다
    private Status status;

    public enum Status { CONFIRMED, CANCELED }

    // JPA가 객체를 만들 때 쓰는 기본 생성자. 밖에서 쓰지 못하게 protected로 둔다.
    protected Reservation() {
    }

    public Reservation(Long roomId, String guestName, LocalDate checkIn, int nights) {
        this.roomId = roomId;
        this.guestName = guestName;
        this.checkIn = checkIn;
        this.nights = nights;
        this.status = Status.CONFIRMED;
    }

    // setter 대신 의미 있는 메서드로 상태를 바꾼다.
    public void cancel() {
        this.status = Status.CANCELED;
    }

    public Long getId() { return id; }
    public Long getRoomId() { return roomId; }
    public String getGuestName() { return guestName; }
    public LocalDate getCheckIn() { return checkIn; }
    public int getNights() { return nights; }
    public Status getStatus() { return status; }
}
