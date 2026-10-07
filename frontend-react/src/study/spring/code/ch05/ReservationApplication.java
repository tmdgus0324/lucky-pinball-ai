package com.example.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 이 클래스가 있는 패키지(com.example.reservation)와 그 아래 패키지에서
// @RestController, @Service 같은 클래스를 찾아 등록한다.
@SpringBootApplication
public class ReservationApplication {

    public static void main(String[] args) {
        // 내장 톰캣을 띄우고, 자동 설정을 적용하고, 우리가 만든 클래스를 등록한다.
        SpringApplication.run(ReservationApplication.class, args);
    }
}
