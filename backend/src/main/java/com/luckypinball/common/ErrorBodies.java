package com.luckypinball.common;

/**
 * 필터가 chain.doFilter() 없이 직접 쓰는 에러 응답(401/429)의 JSON 본문. GlobalExceptionHandler가
 * 만드는 본문과 같은 모양({"error": ..., "traceId": ...})으로 맞춰서, 프론트가 출처와 상관없이
 * 똑같이 읽을 수 있게 한다. 필터라서 Spring의 JSON 변환기를 못 쓰므로 직접 이스케이프한다.
 */
public final class ErrorBodies {

    private ErrorBodies() {
    }

    public static String json(String message) {
        StringBuilder body = new StringBuilder("{\"error\":\"").append(escape(message)).append('"');
        String traceId = TraceId.current();
        if (traceId != null) {
            body.append(",\"traceId\":\"").append(escape(traceId)).append('"');
        }
        return body.append('}').toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
