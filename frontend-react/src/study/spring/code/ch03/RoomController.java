package com.example.reservation.room;

import com.example.reservation.Room;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomController {

    private final RoomService roomService;

    // 컨트롤러는 RoomService를 직접 new 하지 않는다. 스프링이 만들어 둔 것을 받아 쓴다.
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/rooms")
    public List<Room> rooms() {
        return roomService.findAll();
    }

    // {id} 자리의 값을 받는 방법은 6장에서 자세히 본다.
    @GetMapping("/rooms/{id}/price")
    public int todayPrice(@PathVariable Long id) {
        return roomService.todayPrice(id);
    }
}
