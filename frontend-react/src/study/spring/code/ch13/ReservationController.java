package com.example.reservation.reservation;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    // 객실 3개, 예약 6개(객실마다 2개)
    @Bean
    static CommandLineRunner sampleData(RoomRepository rooms, ReservationRepository reservations) {
        return args -> {
            Room ocean = rooms.save(new Room(1L, "오션뷰 디럭스"));
            Room family = rooms.save(new Room(2L, "패밀리 스위트"));
            Room garden = rooms.save(new Room(3L, "가든 트윈"));
            reservations.saveAll(List.of(
                    new Reservation("김민수", ocean), new Reservation("이영희", family),
                    new Reservation("박지훈", garden), new Reservation("김하늘", ocean),
                    new Reservation("최수진", family), new Reservation("정다은", garden)));
        };
    }

    @GetMapping("/n-plus-one")
    public List<ReservationService.ReservationView> nPlusOne() {
        return service.listNPlusOne();
    }

    @GetMapping("/fetch-join")
    public List<ReservationService.ReservationView> fetchJoin() {
        return service.listFetchJoin();
    }

    @GetMapping("/entity-graph")
    public List<ReservationService.ReservationView> entityGraph() {
        return service.listEntityGraph("김");
    }

    // 트랜잭션이 끝난 뒤(서비스 밖)에서 room 이름을 읽는다 → LazyInitializationException
    @GetMapping("/lazy-error")
    public List<String> lazyError() {
        return service.listEntitiesWithoutTransaction().stream()
                .map(r -> r.getGuestName() + " - " + r.getRoom().getName())
                .toList();
    }
}
