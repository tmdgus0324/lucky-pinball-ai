package com.luckypinball.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 요청에 추적 ID를 붙인다. 가장 먼저 실행되어야 해서(HIGHEST_PRECEDENCE) RateLimitFilter·
 * AdminAuthFilter가 컨트롤러까지 가지 않고 직접 쓰는 429/401 응답과 그 로그에도 같은 ID가 찍힌다.
 *
 * 클라이언트가 보낸 ID는 받지 않고 항상 서버가 새로 만든다 — 외부 입력이 로그에 그대로 박히는
 * (줄바꿈 등으로 가짜 로그 줄을 만드는) 일을 막는다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = TraceId.newId();
        MDC.put(TraceId.MDC_KEY, traceId);
        response.setHeader(TraceId.HEADER, traceId);

        long startedAt = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            // CORS preflight(OPTIONS)는 브라우저가 알아서 보내는 확인 요청이라 로그만 어지럽힌다.
            // 쿼리스트링은 남기지 않는다(입력값이 섞일 수 있다).
            if (!"OPTIONS".equalsIgnoreCase(request.getMethod())) {
                log.info("{} {} -> {} ({}ms)", request.getMethod(), request.getRequestURI(),
                        response.getStatus(), System.currentTimeMillis() - startedAt);
            }
            // 톰캣 스레드는 재사용되므로 반드시 비워야 다음 요청에 이전 ID가 새어 나가지 않는다.
            MDC.remove(TraceId.MDC_KEY);
        }
    }
}
