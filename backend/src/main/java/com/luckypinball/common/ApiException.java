package com.luckypinball.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
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

    public static ApiException notImplemented(String message) {
        return new ApiException(HttpStatus.NOT_IMPLEMENTED, message);
    }

    /** 우리가 의존하는 외부 서비스(예: Claude API) 호출이 실패했을 때 사용. */
    public static ApiException upstreamFailure(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
