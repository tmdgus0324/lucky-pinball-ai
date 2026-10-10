package com.example.reservation.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.reservation.Room;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// 단위 테스트. 스프링을 띄우지 않고 RoomService 하나만 new로 만들어 확인한다.
// 가장 빠르고, 업무 규칙(가격 계산)을 확인하는 테스트는 대부분 이 모양이다.
class RoomServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    // 진짜 저장소 대신 Mockito가 만든 가짜. 어떤 값을 돌려줄지 테스트에서 정한다.
    private final RoomRepository roomRepository = mock(RoomRepository.class);

    // 정해 둔 날짜의 낮 12시에 멈춰 있는 시계
    private static Clock fixedAt(String date) {
        return Clock.fixed(LocalDate.parse(date).atTime(12, 0).atZone(SEOUL).toInstant(), SEOUL);
    }

    // 각 테스트 메서드 전에 한 번씩 실행된다
    @BeforeEach
    void setUp() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(new Room(1L, "오션뷰 디럭스", 2, 180000)));
    }

    @Test
    @DisplayName("금요일에는 기본 가격보다 20% 비싸다")
    void fridayPrice() {
        // given: 준비. 2026-10-09는 금요일
        RoomService roomService = new RoomService(roomRepository, fixedAt("2026-10-09"));

        // when: 실행
        int price = roomService.todayPrice(1L);

        // then: 확인
        assertThat(price).isEqualTo(216000);
        verify(roomRepository).findById(1L);   // 저장소를 1번 객실로 불렀는지도 확인
    }

    // 같은 테스트를 값만 바꿔 여러 번 돌린다.
    // JUnit 6부터 이름에 넣은 문자열 값에 따옴표가 붙어서("2026-10-08") 끄는 옵션을 줬다.
    @ParameterizedTest(name = "{0}({1}) → {2}원", quoteTextArguments = false)
    @CsvSource({
            "2026-10-08, 목, 180000",
            "2026-10-09, 금, 216000",
            "2026-10-10, 토, 216000",
            "2026-10-11, 일, 180000",
    })
    @DisplayName("요일별 가격")
    void priceByDay(String date, String dayName, int expected) {
        RoomService roomService = new RoomService(roomRepository, fixedAt(date));

        assertThat(roomService.todayPrice(1L)).isEqualTo(expected);
    }

    @Test
    @DisplayName("없는 객실이면 예외가 난다")
    void unknownRoom() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());
        RoomService roomService = new RoomService(roomRepository, fixedAt("2026-10-09"));

        assertThatThrownBy(() -> roomService.todayPrice(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("없는 객실입니다: 99");
    }
}
