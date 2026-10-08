package com.example.reservation.reservation;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    // 응답용 객체. 엔티티를 그대로 내보내지 않고 필요한 값만 담는다.
    public record ReservationView(String guestName, String roomName) {
        static ReservationView from(Reservation r) {
            return new ReservationView(r.getGuestName(), r.getRoom().getName());   // 여기서 room을 처음 쓴다
        }
    }

    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }

    // 1) N+1: 예약 목록 1번 + 예약마다 객실 조회
    @Transactional(readOnly = true)
    public List<ReservationView> listNPlusOne() {
        return repository.findAll().stream().map(ReservationView::from).toList();
    }

    // 2) fetch join: 1번
    @Transactional(readOnly = true)
    public List<ReservationView> listFetchJoin() {
        return repository.findAllWithRoom().stream().map(ReservationView::from).toList();
    }

    // 3) @EntityGraph: 1번
    @Transactional(readOnly = true)
    public List<ReservationView> listEntityGraph(String prefix) {
        return repository.findByGuestNameStartingWith(prefix).stream().map(ReservationView::from).toList();
    }

    // 4) 트랜잭션 없이 엔티티를 밖으로 넘긴 경우. 받은 쪽에서 room을 쓰면 오류가 난다.
    public List<Reservation> listEntitiesWithoutTransaction() {
        return repository.findAll();
    }
}
