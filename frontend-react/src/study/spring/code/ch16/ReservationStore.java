package com.example.reservation.reservation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

// 예제를 짧게 하려고 DB 대신 메모리에 보관한다(10~13장에서는 DB를 썼다).
@Component
public class ReservationStore {

    private final List<Reservation> reservations = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    public ReservationStore() {
        save("김민수", "오션뷰 디럭스", 2);
        save("<b>박지영</b>", "스탠다드 트윈", 1);   // 태그가 섞인 입력이 화면에 어떻게 나오는지 보려고 넣었다
    }

    public synchronized List<Reservation> findAll() {
        return List.copyOf(reservations);
    }

    public synchronized Reservation save(String guestName, String roomName, int nights) {
        Reservation reservation = new Reservation(sequence.incrementAndGet(), guestName, roomName, nights, "CONFIRMED");
        reservations.add(reservation);
        return reservation;
    }
}
