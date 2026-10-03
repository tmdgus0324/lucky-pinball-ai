package com.luckypinball.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void allowsUpToTheLimitThenReturns429ForTheSameIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        FilterChain noop = (req, res) -> {
        };

        for (int i = 1; i <= 20; i++) {
            MockHttpServletResponse response = doFortuneRequest(filter, noop, "203.0.113.1");
            assertEquals(200, response.getStatus(), "요청 " + i + "번째는 통과해야 한다");
        }

        MockHttpServletResponse blocked = doFortuneRequest(filter, noop, "203.0.113.1");
        assertEquals(429, blocked.getStatus(), "21번째 요청은 막혀야 한다");
    }

    @Test
    void the429BodyCarriesTheTraceId() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        FilterChain noop = (req, res) -> {
        };
        for (int i = 0; i < 20; i++) {
            doFortuneRequest(filter, noop, "203.0.113.2");
        }

        MDC.put(TraceId.MDC_KEY, "abc12345");
        MockHttpServletResponse blocked;
        try {
            blocked = doFortuneRequest(filter, noop, "203.0.113.2");
        } finally {
            MDC.remove(TraceId.MDC_KEY);
        }

        assertEquals(429, blocked.getStatus());
        assertTrue(blocked.getContentAsString().contains("\"traceId\":\"abc12345\""), blocked.getContentAsString());
    }

    @Test
    void differentIpsAreTrackedSeparately() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        FilterChain noop = (req, res) -> {
        };

        for (int i = 0; i < 20; i++) {
            doFortuneRequest(filter, noop, "203.0.113.10");
        }
        MockHttpServletResponse otherIp = doFortuneRequest(filter, noop, "203.0.113.20");

        assertEquals(200, otherIp.getStatus(), "다른 IP는 한도가 독립적으로 유지되어야 한다");
    }

    @Test
    void doesNotLimitOtherEndpoints() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        FilterChain noop = (req, res) -> {
        };

        for (int i = 0; i < 30; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/player");
            request.setRemoteAddr("203.0.113.30");
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(200);
            filter.doFilter(request, response, noop);
            assertEquals(200, response.getStatus(), "제한 대상이 아닌 경로는 계속 통과해야 한다");
        }
    }

    private MockHttpServletResponse doFortuneRequest(RateLimitFilter filter, FilterChain chain, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/fortune");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200); // chain이 실제로는 아무 것도 안 하므로, 통과했을 때의 기본값을 미리 세팅
        filter.doFilter(request, response, chain);
        return response;
    }
}
