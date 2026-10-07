package com.example.reservation.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 우리가 만든 클래스가 아니라서 @Component를 붙일 수 없는 객체(Clock)는 @Bean 메서드로 등록한다.
@Configuration
public class AppConfig {

    // 메서드 이름(clock)이 빈 이름이 되고, 돌려준 객체가 빈이 된다.
    // 테스트에서는 이 빈을 날짜가 고정된 Clock으로 바꿔 끼워 "금요일 가격"을 확인할 수 있다(17장).
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
