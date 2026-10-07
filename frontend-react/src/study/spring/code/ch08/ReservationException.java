package com.example.reservation.common;

import org.springframework.http.HttpStatus;

// 업무 규칙 위반을 나타내는 예외. 상태 코드와 오류 코드를 같이 들고 다닌다.
// RuntimeException을 상속하므로 메서드마다 throws를 적지 않아도 된다.
public class ReservationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    private ReservationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ReservationException notFound(Long id) {
        return new ReservationException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "예약을 찾을 수 없습니다: " + id);
    }

    public static ReservationException alreadyBooked(Long roomId, Object checkIn) {
        return new ReservationException(HttpStatus.CONFLICT, "ROOM_ALREADY_BOOKED",
                roomId + "번 객실은 " + checkIn + "에 이미 예약되어 있습니다");
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
