package com.example.reservation;

import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml의 reservation: 아래 값을 이 record에 한 번에 담는다.
// yml의 max-nights(케밥 표기)가 maxNights(카멜 표기)로 자동 연결된다.
@ConfigurationProperties(prefix = "reservation")
public record ReservationProperties(
        int maxNights,           // 한 번에 예약할 수 있는 최대 박수
        LocalTime checkinTime,   // "15:00" 같은 글자를 시간으로 바꿔 준다
        String notice) {
}
