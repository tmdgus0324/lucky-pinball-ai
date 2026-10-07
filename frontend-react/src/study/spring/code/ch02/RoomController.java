package com.example.reservation;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomController {

    // 클래스마다 로거를 하나 만든다. 로그에 이 클래스 이름이 찍힌다.
    private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    @GetMapping("/rooms")
    public List<Room> rooms() {
        // application.yml에서 이 패키지를 debug로 열어 두었기 때문에 찍힌다.
        log.debug("객실 목록 요청");
        return List.of(
                new Room(1L, "오션뷰 디럭스", 2, 180000),
                new Room(2L, "패밀리 스위트", 4, 320000));
    }
}
