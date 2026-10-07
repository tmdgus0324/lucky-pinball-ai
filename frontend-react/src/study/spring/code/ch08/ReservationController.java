package com.example.reservation.reservation;

import com.example.reservation.common.ReservationException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final Map<Long, Reservation> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    // 컨트롤러에는 if (없으면) return 404 같은 응답 처리가 없다. 예외만 던지고 응답은 GlobalExceptionHandler가 만든다.
    @GetMapping("/{id}")
    public Reservation get(@PathVariable Long id) {
        Reservation found = store.get(id);
        if (found == null) {
            throw ReservationException.notFound(id);
        }
        return found;
    }

    @PostMapping
    public ResponseEntity<Reservation> create(@Valid @RequestBody CreateReservationRequest request) {
        boolean booked = store.values().stream()
                .anyMatch(r -> r.roomId().equals(request.roomId()) && r.checkIn().equals(request.checkIn()));
        if (booked) {
            throw ReservationException.alreadyBooked(request.roomId(), request.checkIn());
        }
        long id = sequence.incrementAndGet();
        Reservation saved = new Reservation(id, request.roomId(), request.guestName(), request.checkIn(), request.nights());
        store.put(id, saved);
        return ResponseEntity.created(URI.create("/reservations/" + id)).body(saved);
    }

    // 예상하지 못한 오류를 보여 주려고 만든 주소
    @GetMapping("/broken")
    public Reservation broken() {
        String roomName = null;
        return new Reservation(1L, 1L, roomName.trim(), null, 1);   // NullPointerException
    }
}
