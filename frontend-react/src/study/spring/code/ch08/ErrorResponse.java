package com.example.reservation.common;

import java.util.List;

// 모든 오류 응답을 이 모양 하나로 맞춘다. 화면과 연동 업체는 이 형식 하나만 알면 된다.
//   code:    프로그램이 구분하는 값(화면에서 code별로 처리)
//   message: 사람에게 보여 줄 문구
//   details: 검증 실패 같은 상세 목록(없으면 빈 목록)
public record ErrorResponse(String code, String message, List<String> details) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of());
    }
}
