package com.example.reservation.reservation;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // fetch join: 예약과 객실을 SQL 한 번(join)으로 같이 가져온다.
    @Query("select r from Reservation r join fetch r.room")
    List<Reservation> findAllWithRoom();

    // 같은 효과를 애너테이션으로: 이 조회에서는 room을 같이 가져오라고 지정한다.
    @EntityGraph(attributePaths = "room")
    List<Reservation> findByGuestNameStartingWith(String prefix);
}
