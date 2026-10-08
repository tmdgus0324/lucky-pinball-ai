package com.example.reservation.reservation;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 인터페이스만 만들면 Spring Data JPA가 구현체를 만든다.
// save, findById, findAll, deleteById, count 같은 기본 메서드는 이미 들어 있다.
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 메서드 이름으로 SQL을 만든다(쿼리 메서드).
    // findBy + 필드 이름 + 조건(Containing = LIKE '%값%')
    List<Reservation> findByGuestNameContaining(String keyword);

    List<Reservation> findByRoomIdAndStatus(Long roomId, Reservation.Status status);

    List<Reservation> findByCheckInGreaterThanEqualOrderByCheckInAsc(LocalDate from);

    // 이름이 너무 길어지거나 복잡하면 JPQL을 직접 쓴다. 테이블이 아니라 엔티티 이름(Reservation)으로 쓴다.
    @Query("select r from Reservation r where r.status = :status and r.nights >= :minNights")
    List<Reservation> findLongStays(@Param("status") Reservation.Status status, @Param("minNights") int minNights);
}
