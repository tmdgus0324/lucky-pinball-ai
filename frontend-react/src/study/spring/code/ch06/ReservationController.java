package com.example.reservation.reservation;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 클래스에 붙인 @RequestMapping 주소가 아래 모든 메서드 주소의 앞부분이 된다.
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    // 이번 장은 컨트롤러에 집중하려고 저장소를 여기에 간단히 둔다(원래는 3장처럼 서비스·저장소로 나눈다).
    private final Map<Long, Reservation> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    // GET /reservations            → 전체
    // GET /reservations?roomId=1   → 1번 객실 예약만 (쿼리 파라미터는 @RequestParam)
    @GetMapping
    public List<Reservation> list(@RequestParam(required = false) Long roomId) {
        return store.values().stream()
                .filter(r -> roomId == null || r.roomId().equals(roomId))
                .toList();
    }

    // GET /reservations/1 → 주소 안의 값은 @PathVariable
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> get(@PathVariable Long id) {
        Reservation found = store.get(id);
        if (found == null) {
            return ResponseEntity.notFound().build();   // 404, 본문 없음
        }
        return ResponseEntity.ok(found);                // 200
    }

    // POST /reservations + JSON 본문 → @RequestBody가 JSON을 객체로 바꿔 준다
    @PostMapping
    public ResponseEntity<Reservation> create(@RequestBody CreateReservationRequest request) {
        long id = sequence.incrementAndGet();
        Reservation saved = new Reservation(id, request.roomId(), request.guestName(), request.checkIn(), request.nights());
        store.put(id, saved);
        // 201 Created + Location 헤더(새로 만든 예약의 주소)
        return ResponseEntity.created(URI.create("/reservations/" + id)).body(saved);
    }

    // DELETE /reservations/1 → 성공하면 본문 없이 204
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        return store.remove(id) == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.noContent().build();
    }
}
