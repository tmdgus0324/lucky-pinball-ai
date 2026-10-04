package com.luckypinball.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    public static ApiException tooManyRequests(String message) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message);
    }

    /** 우리가 의존하는 외부 서비스(예: Claude API) 호출이 실패했을 때 사용. */
    public static ApiException upstreamFailure(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, message);
    }

    /** 원인 예외를 같이 넘기면 GlobalExceptionHandler가 남기는 서버 로그에 "Caused by"로 그대로 보인다. */
    public static ApiException upstreamFailure(String message, Throwable cause) {
        return new ApiException(HttpStatus.BAD_GATEWAY, message, cause);
    }

    /** 외부 서비스가 제한 시간 안에 응답하지 않았을 때(504). */
    public static ApiException upstreamTimeout(String message, Throwable cause) {
        return new ApiException(HttpStatus.GATEWAY_TIMEOUT, message, cause);
    }

    /** 외부 서비스가 요청 과다 등으로 잠시 받아주지 못할 때(503). */
    public static ApiException upstreamUnavailable(String message, Throwable cause) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
