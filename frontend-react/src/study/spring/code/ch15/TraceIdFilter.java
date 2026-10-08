package com.example.reservation.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 요청마다 짧은 추적 ID를 만들어 MDC에 넣는다. 로그 패턴의 %X{traceId}가 이 값을 찍는다.
// 같은 요청에서 나온 로그는 모두 같은 ID를 갖게 되어, 여러 요청이 섞인 로그에서 한 요청만 골라 볼 수 있다.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);   // 응답에도 알려 준다. 사용자가 이 값을 알려 주면 로그를 찾는다
        long started = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            log.info("{} {} -> {} ({}ms)", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), System.currentTimeMillis() - started);
            MDC.remove("traceId");   // 톰캣 스레드는 재사용되므로 꼭 지운다(5장)
        }
    }
}
