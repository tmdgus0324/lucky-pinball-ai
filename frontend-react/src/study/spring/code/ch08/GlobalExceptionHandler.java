package com.example.reservation.common;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// 모든 컨트롤러에서 던진 예외가 여기로 모인다. 예외 종류별로 응답을 정한다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1) 업무 규칙 위반(404, 409 등): 예상한 상황이라 WARN 한 줄만 남긴다.
    @ExceptionHandler(ReservationException.class)
    public ResponseEntity<ErrorResponse> handleReservation(ReservationException e) {
        log.warn("업무 오류 {}: {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    // 2) @Valid 검증 실패(400): 어떤 필드가 왜 틀렸는지 모두 돌려준다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();
        log.warn("검증 실패 {}", details);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_INPUT", "입력값을 확인해 주세요", details));
    }

    // 3) JSON 형식 오류(400): 클라이언트 잘못이므로 500으로 만들지 않는다.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("요청 형식 오류: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ErrorResponse.of("BAD_FORMAT", "요청 형식이 올바르지 않습니다"));
    }

    // 4) 없는 주소(404), 지원하지 않는 메서드(405): 서버 잘못이 아니다.
    //    이 둘을 따로 잡지 않으면 아래 Exception 처리기에 걸려 500이 된다(실행 결과 참고).
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of("NOT_FOUND", "없는 주소입니다"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of("METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다"));
    }

    // 5) 그 밖의 모든 예외(500): 예상하지 못한 상황. 스택 트레이스를 로그에 남기고,
    //    응답에는 내부 사정(예외 메시지, 클래스 이름)을 싣지 않는다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리하지 못한 예외", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "잠시 후 다시 시도해 주세요"));
    }
}
