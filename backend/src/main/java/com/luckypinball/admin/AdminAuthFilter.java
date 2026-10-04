package com.luckypinball.admin;

import com.luckypinball.common.CorsSupport;
import com.luckypinball.common.ErrorBodies;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * `/api/admin/**` 요청은 `Authorization: Bearer <토큰>` 헤더가 유효할 때만 통과시킨다.
 * 로그인 자체(`/api/admin/login`)는 당연히 제외 — 그거까지 막으면 로그인을 할 수가 없다.
 *
 * 쿠키 대신 헤더 토큰 방식을 쓴 이유: 백엔드(Render)와 프론트(Vercel)가 서로 다른 도메인이라,
 * 쿠키 기반 세션은 SameSite/Secure 설정을 배포 환경에 맞게 맞추는 게 까다롭다. 커스텀 헤더는
 * 그런 브라우저 쿠키 정책과 무관해서 배포 환경에서도 그대로 동작한다.
 */
@Component
public class AdminAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthFilter.class);

    private static final String ADMIN_PATH_PREFIX = "/api/admin/";
    private static final String LOGIN_PATH = "/api/admin/login";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AdminSessionStore sessionStore;

    public AdminAuthFilter(AdminSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isAdminPath = path.startsWith(ADMIN_PATH_PREFIX);

        // CORS preflight(OPTIONS)는 브라우저가 토큰 없이 미리 보내는 확인 요청이다 — 여기서
        // 막아버리면 스프링의 CORS 응답 헤더 처리(WebConfig)까지 못 가서, 실제 요청이 브라우저
        // 단에서 "Failed to fetch"로 통째로 실패한다(실제로 이 버그로 겪었다). preflight는
        // 항상 통과시키고, 진짜 요청에서만 토큰을 검사한다.
        boolean isPreflight = "OPTIONS".equalsIgnoreCase(request.getMethod());

        if (!isAdminPath || isPreflight || LOGIN_PATH.equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractToken(request.getHeader("Authorization"));
        if (!sessionStore.isValid(token)) {
            // 이 응답은 DispatcherServlet까지 안 가서 WebConfig의 CORS 설정이 안 먹는다 —
            // 직접 CORS 헤더를 안 붙이면 브라우저가 401을 "Failed to fetch"로 뭉개버린다
            // (devhelp/08(구 34)에서 실제로 겪은 버그).
            // 토큰 값 자체는 로그에 남기지 않는다 — 어떤 요청이 막혔는지만 남긴다.
            log.warn("관리자 API 인증 실패: {} {}", request.getMethod(), path);
            CorsSupport.applyCorsHeaders(request, response);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(ErrorBodies.json("로그인이 필요합니다."));
            return;
        }

        chain.doFilter(request, response);
    }

    static String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }
}
