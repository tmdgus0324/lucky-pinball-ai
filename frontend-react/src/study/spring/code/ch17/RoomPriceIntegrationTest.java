package com.example.reservation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

// 통합 테스트. 애플리케이션 전체(모든 빈)를 띄워서 컨트롤러 → 서비스 → 저장소까지 진짜로 지나간다.
// 시계만 금요일로 고정한다.
@SpringBootTest
@AutoConfigureMockMvc
class RoomPriceIntegrationTest {

    // 테스트에서만 쓰는 설정. Clock 빈을 하나 더 등록하고 @Primary로 이것을 먼저 쓰게 한다.
    // 같은 타입의 빈이 둘이면 서버가 뜨지 않지만, @Primary가 있으면 그것을 고른다(3장).
    @TestConfiguration
    static class FridayClockConfig {
        @Bean
        @Primary
        Clock fridayClock() {
            ZoneId seoul = ZoneId.of("Asia/Seoul");
            return Clock.fixed(LocalDate.of(2026, 10, 9).atTime(12, 0).atZone(seoul).toInstant(), seoul);
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("금요일에 1번 객실 가격을 물으면 216000원")
    void fridayPrice() throws Exception {
        mockMvc.perform(get("/rooms/1/price"))
                .andExpect(status().isOk())
                .andExpect(content().string("216000"));
    }

    @Test
    @DisplayName("객실 목록에는 미리 넣어 둔 객실 2개가 있다")
    void rooms() throws Exception {
        mockMvc.perform(get("/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
