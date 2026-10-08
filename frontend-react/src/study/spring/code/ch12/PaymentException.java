package com.example.reservation.reservation;

// 결제 실패를 나타내는 checked 예외(Exception을 상속). 메서드에 throws를 꼭 적어야 한다.
public class PaymentException extends Exception {

    public PaymentException(String message) {
        super(message);
    }
}
