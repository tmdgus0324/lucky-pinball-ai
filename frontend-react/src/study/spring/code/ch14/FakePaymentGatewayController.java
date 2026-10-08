package com.example.reservation.payment;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// 예제용 가짜 결제 서버(PG). 카드 값에 따라 정상, 거절, 느린 응답, 서버 오류를 흉내 낸다.
@RestController
public class FakePaymentGatewayController {

    @PostMapping("/fake-pg/pay")
    public ResponseEntity<Map<String, String>> pay(@RequestBody Map<String, Object> request) throws InterruptedException {
        String card = String.valueOf(request.get("card"));
        return switch (card) {
            case "LIMIT" -> ResponseEntity.status(400).body(Map.of("code", "LIMIT_EXCEEDED"));
            case "BROKEN" -> ResponseEntity.status(500).body(Map.of("code", "INTERNAL"));
            case "SLOW" -> {
                Thread.sleep(5000);   // 5초 뒤에야 응답한다
                yield ResponseEntity.ok(Map.of("approvalNo", "A-SLOW"));
            }
            default -> ResponseEntity.ok(Map.of("approvalNo", "A-" + request.get("reservationId")));
        };
    }
}
