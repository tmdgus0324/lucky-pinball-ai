package com.example.reservation;

// 객실 하나. record는 생성자, getter, equals를 자동으로 만들어 주는 Java 16 이상 문법이다.
// JSON으로 바꿀 때는 필드 이름이 그대로 키가 된다.
public record Room(Long id, String name, int capacity, int price) {
}
