package com.example.reservation.payment;

// 결제 실패. 원인별로 나눠 두면 화면 안내와 재시도 여부를 다르게 정할 수 있다.
public class PaymentException extends RuntimeException {

    public enum Reason {
        REJECTED,       // 거절(4xx): 사용자에게 다른 카드를 안내
        SERVER_ERROR,   // 결제 서버 오류(5xx): 잠시 뒤 다시
        UNAVAILABLE     // 연결 실패·시간 초과: 결제가 됐는지 모르므로 확인이 필요
    }

    private final Reason reason;

    public PaymentException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
