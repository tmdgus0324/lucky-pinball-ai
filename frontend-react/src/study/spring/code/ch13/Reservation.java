package com.example.reservation.reservation;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String guestName;

    // 예약 여러 개가 객실 하나를 가리킨다(N:1). reservation 테이블에 room_id 외래 키가 생긴다.
    // LAZY: 예약을 조회할 때 객실은 바로 가져오지 않고, room을 처음 쓰는 순간에 따로 조회한다.
    // (@ManyToOne의 기본값은 EAGER지만, 실무에서는 거의 항상 LAZY로 바꾼다)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    protected Reservation() {
    }

    public Reservation(String guestName, Room room) {
        this.guestName = guestName;
        this.room = room;
    }

    public Long getId() { return id; }
    public String getGuestName() { return guestName; }
    public Room getRoom() { return room; }
}
