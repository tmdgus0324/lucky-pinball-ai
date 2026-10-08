package com.example.reservation.reservation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 경우마다 예약을 시도하고, 실패한 뒤 DB에 남은 예약 수를 돌려준다.
@RestController
@RequestMapping("/tx")
public class TransactionController {

    private final ReservationService service;

    public TransactionController(ReservationService service) {
        this.service = service;
    }

    @PostMapping("/runtime")
    public String runtime() {
        return attempt(() -> service.reserveWithRuntimeFailure("김민수"));
    }

    @PostMapping("/checked")
    public String checked() {
        return attempt(() -> service.reserveWithCheckedFailure("이영희"));
    }

    @PostMapping("/checked-rollback")
    public String checkedRollback() {
        return attempt(() -> service.reserveWithCheckedFailureRollback("박지훈"));
    }

    @PostMapping("/self-call")
    public String selfCall() {
        return attempt(() -> service.reserveFromInside("최수진"));
    }

    interface Attempt {
        void run() throws Exception;
    }

    private String attempt(Attempt attempt) {
        long before = service.count();
        try {
            attempt.run();
            return "성공";
        } catch (Exception e) {
            long after = service.count();
            return "실패(" + e.getMessage() + ") / 예약 수 " + before + " → " + after;
        }
    }
}
