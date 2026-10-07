package com.example.reservation;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomController {

    private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    @GetMapping("/rooms")
    public List<Room> rooms() {
        log.info("[컨트롤러] 객실 목록 조회");
        return List.of(new Room(1L, "오션뷰 디럭스", 2, 180000));
    }

    // 처리 중 예외가 나는 경우를 보여 주려고 만든 주소
    @GetMapping("/rooms/broken")
    public List<Room> broken() {
        log.info("[컨트롤러] 처리 중 예외 발생");
        throw new IllegalStateException("객실 정보를 읽지 못했습니다");
    }
}
