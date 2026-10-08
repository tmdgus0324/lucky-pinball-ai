package com.example.reservation.reservation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }

    // 변경 감지(dirty checking): 트랜잭션 안에서 조회한 엔티티의 값을 바꾸기만 하면,
    // save를 부르지 않아도 트랜잭션이 끝날 때 JPA가 UPDATE를 실행한다. 트랜잭션은 12장에서 자세히 본다.
    @Transactional
    public Reservation cancel(Long id) {
        Reservation reservation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약이 없습니다: " + id));
        reservation.cancel();
        return reservation;
    }
}
