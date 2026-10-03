package com.luckypinball.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 에러 응답은 항상 {"error": 메시지, "traceId": 추적 ID} 모양이다. 서버 내부 사정(예외 메시지,
 * 스택트레이스)은 응답에 싣지 않고 로그에만 남긴다 — 사용자는 traceId만 알려주면 되고,
 * 우리는 그 ID로 로그를 찾는다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorLogStore errorLogStore;

    public GlobalExceptionHandler(ErrorLogStore errorLogStore) {
        this.errorLogStore = errorLogStore;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            // 우리가 의존하는 외부 서비스 장애 등 — 서버 쪽 문제라 스택트레이스까지 남긴다.
            log.error("{} {} -> {} {}", request.getMethod(), request.getRequestURI(), ex.getStatus().value(), ex.getMessage(), ex);
        } else {
            log.warn("{} {} -> {} {}", request.getMethod(), request.getRequestURI(), ex.getStatus().value(), ex.getMessage());
        }
        errorLogStore.record(request.getRequestURI(), ex.getMessage(), TraceId.current());
        return ResponseEntity.status(ex.getStatus()).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Invalid request");
        log.warn("{} {} -> 400 {}", request.getMethod(), request.getRequestURI(), message);
        errorLogStore.record(request.getRequestURI(), message, TraceId.current());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(message));
    }

    // 요청 본문이 JSON이 아니거나 타입이 안 맞는 경우 등은 클라이언트가 잘못 보낸 것이지 우리 서버
    // 오류가 아니다. Exception.class로 잡히게 두면 500으로 둔갑해 ERROR 로그와 스택트레이스로
    // 로그가 어지러워진다(실제로 깨진 JSON 요청이 500으로 응답되고 있었다).
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<Map<String, String>> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        String message = "요청 형식이 올바르지 않습니다.";
        log.warn("{} {} -> 400 {} ({})", request.getMethod(), request.getRequestURI(), message, ex.getClass().getSimpleName());
        errorLogStore.record(request.getRequestURI(), message, TraceId.current());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(message));
    }

    // 존재하지 않는 API에 잘못된 메서드/타입으로 오는 요청(스캐너·봇 등) — 같은 이유로 4xx로 돌려주되,
    // 관리자 오류 로그(ErrorLogStore)는 이런 요청으로 채우지 않는다.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("{} {} -> 405", request.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorBody("지원하지 않는 요청 방식입니다."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("{} {} -> 415", request.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorBody("지원하지 않는 요청 형식입니다."));
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
        // 스택트레이스는 여기 로그에만 남는다. 응답에는 일반 메시지 + traceId만 준다.
        log.error("처리되지 않은 예외: {} {}", request.getMethod(), request.getRequestURI(), ex);
        errorLogStore.record(request.getRequestURI(), ex.getMessage(), TraceId.current());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody("Unexpected error"));
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        String traceId = TraceId.current();
        if (traceId != null) {
            body.put("traceId", traceId);
        }
        return body;
    }
}
