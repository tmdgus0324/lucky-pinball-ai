package com.example.reservation.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 필터: 서블릿(톰캣) 단계에서 DispatcherServlet보다 먼저 실행된다.
// 빈으로 등록하면 모든 주소(/*)에 자동으로 걸린다.
@Component
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.currentTimeMillis();
        log.info("[필터] 요청 시작 {} {}", request.getMethod(), request.getRequestURI());

        // 차단 예: 점검 중 헤더가 있으면 여기서 바로 응답하고 끝낸다. 컨트롤러까지 가지 않는다.
        if ("true".equals(request.getHeader("X-Maintenance"))) {
            response.setStatus(503);
            log.info("[필터] 점검 중이라 막음");
            return;
        }

        chain.doFilter(request, response);   // 다음 필터, 그리고 DispatcherServlet으로 넘긴다
        log.info("[필터] 요청 끝 {} ({}ms)", response.getStatus(), System.currentTimeMillis() - started);
    }
}
