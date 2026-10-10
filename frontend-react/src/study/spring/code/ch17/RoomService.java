package com.example.reservation.room;

import com.example.reservation.Room;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// 업무 규칙을 담는 역할. 컨트롤러는 요청을 받고, 계산과 판단은 서비스가 한다.
@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final Clock clock;

    // 생성자 주입: 필요한 객체를 생성자 파라미터로 받는다.
    // 스프링이 RoomRepository 빈과 Clock 빈을 찾아서 넣어 준다(생성자가 하나면 @Autowired 생략 가능).
    public RoomService(RoomRepository roomRepository, Clock clock) {
        this.roomRepository = roomRepository;
        this.clock = clock;
        log.info("RoomService를 만들었습니다");
    }

    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    // 오늘이 금·토요일이면 20% 비싸다.
    public int todayPrice(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("없는 객실입니다: " + roomId));
        DayOfWeek today = LocalDate.now(clock).getDayOfWeek();
        boolean weekend = today == DayOfWeek.FRIDAY || today == DayOfWeek.SATURDAY;
        return weekend ? room.price() * 120 / 100 : room.price();
    }
}
