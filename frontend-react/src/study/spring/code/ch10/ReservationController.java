package com.example.reservation.reservation;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 이번 장은 SQL에 집중하려고 컨트롤러가 Mapper를 바로 쓴다(원래는 서비스를 거친다, 12장).
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationMapper mapper;

    public ReservationController(ReservationMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reservation> get(@PathVariable Long id) {
        Reservation found = mapper.findById(id);
        return found == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(found);
    }

    // GET /reservations?guestName=김&roomIds=1,2&from=2026-11-01&status=CONFIRMED (모두 선택)
    @GetMapping
    public List<Reservation> search(@RequestParam(required = false) String guestName,
                                    @RequestParam(required = false) List<Long> roomIds,
                                    @RequestParam(required = false) LocalDate from,
                                    @RequestParam(required = false) String status) {
        return mapper.search(new ReservationSearch(guestName, roomIds, from, status));
    }

    @PostMapping
    public Reservation create(@RequestBody Reservation reservation) {
        reservation.setStatus("CONFIRMED");
        mapper.insert(reservation);   // insert 뒤에 reservation.getId()에 새 id가 들어 있다
        return reservation;
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        int updated = mapper.updateStatus(id, "CANCELED");   // 바뀐 줄 수를 돌려준다
        return updated == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }

    // ${} 위험 시연용. 실제 서비스에는 이런 주소를 만들지 않는다.
    @GetMapping("/unsafe")
    public List<Reservation> unsafe(@RequestParam String guestName) {
        return mapper.findByGuestNameUnsafe(guestName);
    }
}
