package com.example.reservation.reservation;

import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 예제를 짧게 하려고 엔티티를 그대로 응답한다. 실무에서는 응답용 DTO로 바꿔서 보낸다(6장 4절).
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationRepository repository;
    private final ReservationService service;

    public ReservationController(ReservationRepository repository, ReservationService service) {
        this.repository = repository;
        this.service = service;
    }

    // 서버가 뜰 때 예제 데이터 넣기. save는 INSERT로 바뀐다.
    @Bean
    static CommandLineRunner sampleData(ReservationRepository repository) {
        return args -> repository.saveAll(List.of(
                new Reservation(1L, "김민수", LocalDate.of(2026, 11, 1), 2),
                new Reservation(2L, "이영희", LocalDate.of(2026, 11, 3), 1),
                new Reservation(1L, "박지훈", LocalDate.of(2026, 11, 10), 3)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reservation> get(@PathVariable Long id) {
        // findById는 Optional을 돌려준다. 없으면 404.
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Reservation> search(@RequestParam String keyword) {
        return repository.findByGuestNameContaining(keyword);
    }

    @GetMapping("/room/{roomId}")
    public List<Reservation> byRoom(@PathVariable Long roomId) {
        return repository.findByRoomIdAndStatus(roomId, Reservation.Status.CONFIRMED);
    }

    @GetMapping("/long-stays")
    public List<Reservation> longStays() {
        return repository.findLongStays(Reservation.Status.CONFIRMED, 2);
    }

    @PostMapping("/{id}/cancel")
    public Reservation cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
