package com.luckypinball.common;

import com.luckypinball.config.WebConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 필터가 체인을 계속 태우지 않고(chain.doFilter() 없이) 직접 응답을 써버리는 경우
 * (AdminAuthFilter의 401, RateLimitFilter의 429) 쓰는 헬퍼.
 *
 * 그런 응답은 DispatcherServlet까지 도달하지 않아서 WebConfig의 addCorsMappings가 적용된
 * 정상 요청과 달리 CORS 응답 헤더가 전혀 안 붙는다 — 브라우저는 이걸 실제 상태 코드(401 등)
 * 대신 그냥 "Failed to fetch"로 뭉뚱그려버려서, 프론트가 에러 메시지조차 못 읽는다
 * (devhelp/34에서 실제로 겪은 문제).
 */
public final class CorsSupport {

    private CorsSupport() {
    }

    public static void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && WebConfig.ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.addHeader("Vary", "Origin");
        }
    }
}
