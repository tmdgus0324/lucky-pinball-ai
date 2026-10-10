package com.example.reservation.room;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.reservation.Room;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 컨트롤러 테스트. 웹 계층(컨트롤러, JSON 변환, 주소 연결)만 띄운다. 서비스와 저장소는 만들지 않는다.
@WebMvcTest(RoomController.class)
class RoomControllerTest {

    // 서버를 실제 포트로 띄우지 않고 요청을 흉내 내서 보내는 도구
    @Autowired
    MockMvc mockMvc;

    // 컨트롤러가 필요로 하는 RoomService 자리에 가짜를 빈으로 넣는다.
    // Boot 3 이하 코드에서 보던 @MockBean은 Boot 4에서 없어졌고 이것으로 바뀌었다.
    @MockitoBean
    RoomService roomService;

    @Test
    @DisplayName("GET /rooms는 객실 목록을 JSON으로 준다")
    void rooms() throws Exception {
        when(roomService.findAll()).thenReturn(List.of(new Room(1L, "오션뷰 디럭스", 2, 180000)));

        mockMvc.perform(get("/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("오션뷰 디럭스"))
                .andExpect(jsonPath("$[0].price").value(180000));
    }

    @Test
    @DisplayName("GET /rooms/1/price는 서비스가 계산한 가격을 그대로 준다")
    void price() throws Exception {
        when(roomService.todayPrice(1L)).thenReturn(216000);

        mockMvc.perform(get("/rooms/1/price"))
                .andExpect(status().isOk())
                .andExpect(content().string("216000"));
    }

    @Test
    @DisplayName("주소의 id가 숫자가 아니면 400이고, 서비스는 불리지 않는다")
    void notANumber() throws Exception {
        mockMvc.perform(get("/rooms/abc/price"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(roomService);
    }
}
