package com.example.reservation.member;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

@RestController
public class MyReservationController {

    // 인터셉터가 로그인을 확인했으므로 여기서는 세션 값을 꺼내 쓰기만 한다.
    // @SessionAttribute: 세션에 저장된 값을 이름으로 꺼내 파라미터로 받는다.
    @GetMapping("/reservations/my")
    public String myReservations(@SessionAttribute(LoginMember.SESSION_KEY) LoginMember member) {
        return member.name() + "님의 예약 목록입니다";
    }

    @GetMapping("/reservations/public")
    public String publicNotice() {
        return "누구나 볼 수 있는 안내입니다";
    }
}
