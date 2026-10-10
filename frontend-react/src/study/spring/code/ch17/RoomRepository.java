package com.example.reservation.room;

import com.example.reservation.Room;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

// 3장과 같은 코드. 예제를 짧게 하려고 DB 대신 메모리(Map)에 둔다.
@Repository
public class RoomRepository {

    private static final Logger log = LoggerFactory.getLogger(RoomRepository.class);

    private final Map<Long, Room> rooms = new ConcurrentHashMap<>();

    // 빈이 만들어지고 의존성 주입까지 끝난 뒤 한 번 실행된다. 초기 데이터 준비에 쓴다.
    @PostConstruct
    void loadSampleRooms() {
        rooms.put(1L, new Room(1L, "오션뷰 디럭스", 2, 180000));
        rooms.put(2L, new Room(2L, "패밀리 스위트", 4, 320000));
        log.info("객실 {}개를 준비했습니다", rooms.size());
    }

    public List<Room> findAll() {
        return List.copyOf(rooms.values());
    }

    public Optional<Room> findById(Long id) {
        return Optional.ofNullable(rooms.get(id));
    }
}
