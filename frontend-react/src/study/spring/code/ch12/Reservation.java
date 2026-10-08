package com.example.reservation.reservation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String guestName;

    protected Reservation() {
    }

    public Reservation(String guestName) {
        this.guestName = guestName;
    }

    public Long getId() { return id; }
    public String getGuestName() { return guestName; }
}
