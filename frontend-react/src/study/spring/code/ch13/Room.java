package com.example.reservation.reservation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Room {

    @Id
    private Long id;

    private String name;

    // 객실 하나에 예약 여러 개(1:N). mappedBy: 연관관계의 주인은 Reservation.room 쪽이다(외래 키가 그 테이블에 있다).
    @OneToMany(mappedBy = "room")
    private List<Reservation> reservations = new ArrayList<>();

    protected Room() {
    }

    public Room(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public List<Reservation> getReservations() { return reservations; }
}
