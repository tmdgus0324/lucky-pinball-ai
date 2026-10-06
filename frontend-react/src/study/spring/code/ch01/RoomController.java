package com.example.reservation;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 메서드가 돌려주는 값을 그대로 HTTP 응답 본문(JSON)으로 보낸다.
@RestController
public class RoomController {

    // GET /rooms 요청이 오면 이 메서드가 실행된다.
    @GetMapping("/rooms")
    public List<Room> rooms() {
        // 아직 DB가 없으니 고정된 목록을 돌려준다. 10장(MyBatis)과 11장(JPA)에서 DB로 바꾼다.
        return List.of(
                new Room(1L, "오션뷰 디럭스", 2, 180000),
                new Room(2L, "패밀리 스위트", 4, 320000));
    }
}
