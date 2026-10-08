package com.example.reservation.reservation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// 화면의 <form> 입력값을 받는 객체. 입력란 이름(name="guestName")과 필드 이름이 같으면 자동으로 채워진다.
// 화면 폼은 getter/setter가 있는 클래스로 만드는 경우가 많다(th:field, JSP의 <form:input>이 getter로 값을 읽는다).
public class ReservationForm {

    @NotBlank(message = "이름을 입력하세요")
    private String guestName;

    @NotBlank(message = "객실을 고르세요")
    private String roomName;

    @Min(value = 1, message = "1박 이상이어야 합니다")
    @Max(value = 30, message = "30박까지 예약할 수 있습니다")
    private int nights = 1;

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }
}
