package com.luckypinball.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * `/api/fortune`은 실제로 비용이 드는 Claude API를 호출할 수 있는 유일한 엔드포인트다.
 * 인증이 없는 공개 서비스라, IP당 1분에 너무 많이 호출하면 429로 막는다 — 이번 판(최대 8명)
 * 정도는 문제없이 통과하고, 반복 스크립트로 긁는 것만 막는 정도의 단순한 방어다.
 *
 * 인메모리 카운터라서 서버가 여러 대로 늘어나면 IP별 카운트가 서버마다 따로 세어지지만,
 * 지금 규모(단일 인스턴스, 무료 티어)에서는 충분하다 — Redis 등으로 옮기는 건 트래픽이
 * 실제로 늘었을 때 할 일이다.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LIMITED_PATH = "/api/fortune";
    private static final int MAX_REQUESTS_PER_WINDOW = 20;
    private static final long WINDOW_MILLIS = 60_000;

    private final Map<String, Window> windowsByIp = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!LIMITED_PATH.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        Window window = windowsByIp.computeIfAbsent(clientIp(request), key -> new Window());
        if (!window.tryConsume()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        // Render는 요청을 프록시(Cloudflare 등)로 넘기므로 getRemoteAddr()은 프록시 주소일 수
        // 있다 — 실제 클라이언트 IP는 X-Forwarded-For의 첫 값이다.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** 1분 고정 창(fixed window) 방식 — 정교하지 않지만 구현이 단순하고 이 규모엔 충분하다. */
    private static final class Window {
        private long windowStartMillis = System.currentTimeMillis();
        private int count;

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStartMillis > WINDOW_MILLIS) {
                windowStartMillis = now;
                count = 0;
            }
            count++;
            return count <= MAX_REQUESTS_PER_WINDOW;
        }
    }
}
