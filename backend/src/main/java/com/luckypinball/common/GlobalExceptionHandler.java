package com.luckypinball.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ErrorLogStore errorLogStore;

    public GlobalExceptionHandler(ErrorLogStore errorLogStore) {
        this.errorLogStore = errorLogStore;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException ex, HttpServletRequest request) {
        errorLogStore.record(request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Invalid request");
        errorLogStore.record(request.getRequestURI(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(message));
    }

    // 존재하지 않는 경로 요청(예: /h2-console, /favicon.ico)은 정상적인 404 상황이지,
    // 우리 서버가 잘못 동작한 게 아니다. Exception.class로 잡아버리면 이런 요청까지 전부
    // "500 Unexpected error"로 둔갑하고, 운영 로그 화면(/api/admin/logs)이 실제 장애가
    // 아닌 항목으로 가득 차서 정작 봐야 할 에러를 찾기 어려워진다 — 별도로 조용히 404만 낸다.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex, HttpServletRequest request) {
        errorLogStore.record(request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody("Unexpected error"));
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return body;
    }
}
