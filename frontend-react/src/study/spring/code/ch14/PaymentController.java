package com.example.reservation.payment;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentClient client;
    private final PaymentClient downClient;   // 꺼진 서버를 부르는 경우를 보여 주기 위한 것

    public PaymentController(@Value("${payment.base-url}") String baseUrl,
                             @Value("${payment.down-url}") String downUrl,
                             @Value("${payment.connect-timeout}") Duration connectTimeout,
                             @Value("${payment.read-timeout}") Duration readTimeout) {
        this.client = new PaymentClient(baseUrl, connectTimeout, readTimeout);
        this.downClient = new PaymentClient(downUrl, connectTimeout, readTimeout);
    }

    // POST /reservations/1/pay?card=OK|LIMIT|SLOW|BROKEN&down=false
    @PostMapping("/reservations/{id}/pay")
    public Map<String, Object> pay(@PathVariable Long id,
                                   @RequestParam String card,
                                   @RequestParam(defaultValue = "false") boolean down) {
        long started = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            PaymentClient.PayResponse response = (down ? downClient : client)
                    .pay(new PaymentClient.PayRequest(id, 180000, card));
            result.put("result", "승인 " + response.approvalNo());
        } catch (PaymentException e) {
            log.warn("결제 실패 [{}] {}", e.getReason(), e.getMessage());
            result.put("result", e.getReason() + ": " + e.getMessage());
        }
        result.put("elapsedMs", System.currentTimeMillis() - started);
        return result;
    }
}
