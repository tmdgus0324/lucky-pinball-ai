package com.luckypinball.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 *
 * 클라이언트 IP는 X-Forwarded-For가 아니라 앞단 프록시(Cloudflare)가 넣는 헤더로 판단한다 — 아래 clientIp() 참고.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String LIMITED_PATH = "/api/fortune";
    private static final int MAX_REQUESTS_PER_WINDOW = 20;
    private static final long WINDOW_MILLIS = 60_000;

    private final Map<String, Window> windowsByIp = new ConcurrentHashMap<>();

    /** 앞단 프록시가 실제 방문자 IP를 넣어 주는 헤더 이름. 비어 있으면 연결 주소(getRemoteAddr)만 쓴다. */
    private final String clientIpHeader;

    public RateLimitFilter(@Value("${rate-limit.client-ip-header:True-Client-IP}") String clientIpHeader) {
        this.clientIpHeader = clientIpHeader == null ? "" : clientIpHeader.trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!LIMITED_PATH.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        Window window = windowsByIp.computeIfAbsent(ip, key -> new Window());
        if (!window.tryConsume()) {
            // AdminAuthFilter의 401과 같은 이유로 CORS 헤더를 직접 붙여야 한다 — 안 그러면
            // 브라우저가 429를 "Failed to fetch"로 뭉개버린다(devhelp/08(구 34)).
            log.warn("요청 제한 초과: ip={} path={}", ip, request.getRequestURI());
            CorsSupport.applyCorsHeaders(request, response);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(ErrorBodies.json("요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * 예전에는 X-Forwarded-For의 첫 값을 썼는데, Render의 프록시는 이 헤더를 교체하지 않고 **뒤에 덧붙여서**
     * 첫 값은 클라이언트가 마음대로 정할 수 있었다. 요청마다 값을 바꿔 보내면 제한을 그대로 우회됐다
     * (운영에서 실제로 확인, devhelp/08). 그래서 X-Forwarded-For는 아예 보지 않는다.
     *
     * 대신 앞단의 Cloudflare가 넣는 헤더(기본 True-Client-IP)를 쓴다. 클라이언트가 같은 이름으로 보내도
     * Cloudflare가 실제 방문자 IP로 덮어쓴다는 전제이고, 이 전제는 배포 후 운영에서 다시 확인했다.
     * 이 헤더가 없으면(로컬 개발 등) 연결 주소를 쓴다. 다른 플랫폼으로 옮기면 그 플랫폼이 보장하는 헤더로
     * 바꿔야 한다(환경변수 CLIENT_IP_HEADER).
     */
    private String clientIp(HttpServletRequest request) {
        if (!clientIpHeader.isEmpty()) {
            String value = request.getHeader(clientIpHeader);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
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
