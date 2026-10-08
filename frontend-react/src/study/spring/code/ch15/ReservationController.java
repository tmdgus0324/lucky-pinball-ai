package com.example.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {

    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);

    @GetMapping("/reservations/{id}")
    public String get(@PathVariable Long id) {
        log.info("예약 조회 시작 id={}", id);
        log.debug("DEBUG 로그: 상세 조회 조건 id={}", id);   // 레벨이 info면 찍히지 않는다
        if (id == 99) {
            try {
                Integer.parseInt("구십구");
            } catch (NumberFormatException e) {
                // 예외 객체를 마지막 인자로 넘겨야 스택 트레이스가 남는다(8장)
                log.error("예약 번호 변환 실패 id={}", id, e);
            }
        }
        return "예약 " + id;
    }

    // 롤링을 보여 주려고 로그를 많이 쓰는 주소
    @PostMapping("/logs/burst")
    public String burst() {
        for (int i = 1; i <= 300; i++) {
            log.info("대량 로그 {}번째 줄 - 롤링 확인용 메시지입니다", i);
        }
        return "300줄 기록";
    }
}
