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
        RateLimitFilter filter = new RateLimitFilter("True-Client-IP");
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
        RateLimitFilter filter = new RateLimitFilter("True-Client-IP");
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
        RateLimitFilter filter = new RateLimitFilter("True-Client-IP");
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
        RateLimitFilter filter = new RateLimitFilter("True-Client-IP");
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

    @Test
    void changingXForwardedForCannotBypassTheLimit() throws Exception {
        // 운영에서 실제로 재현된 우회: 같은 사람이 요청마다 X-Forwarded-For 첫 값을 바꿔 보내면 IP마다 따로 세어져
        // 제한에 걸리지 않았다(Render 프록시는 이 헤더를 교체하지 않고 뒤에 덧붙이므로 첫 값은 클라이언트가 정한다).
        RateLimitFilter filter = new RateLimitFilter("True-Client-IP");
        FilterChain noop = (req, res) -> {
        };

        MockHttpServletResponse last = null;
        for (int i = 1; i <= 21; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/fortune");
            request.setRemoteAddr("203.0.113.40");
            request.addHeader("X-Forwarded-For", "198.51.100." + i + ", 203.0.113.40");
            last = new MockHttpServletResponse();
            last.setStatus(200);
            filter.doFilter(request, last, noop);
        }

        assertEquals(429, last.getStatus(), "헤더를 바꿔도 같은 클라이언트는 21번째에 막혀야 한다");
    }

    /** Cloudflare를 거친 요청을 흉내 낸다: 연결 주소(remoteAddr)는 프록시, 실제 방문자 IP는 헤더에 담긴다. */
    private MockHttpServletResponse doViaProxy(RateLimitFilter filter, String proxyAddr, String headerName, String visitorIp) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/fortune");
        request.setRemoteAddr(proxyAddr);
        if (headerName != null) {
            request.addHeader(headerName, visitorIp);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        filter.doFilter(request, response, (req, res) -> {
        });
        return response;
    }

    @Test
    void visitorsBehindTheSameProxyAreCountedSeparatelyByTheTrustedHeader() throws Exception {
        RateLimitFilter filter = new RateLimitFilter("True-Client-IP");
        for (int i = 0; i < 20; i++) {
            doViaProxy(filter, "10.0.0.1", "True-Client-IP", "203.0.113.50");
        }

        assertEquals(429, doViaProxy(filter, "10.0.0.1", "True-Client-IP", "203.0.113.50").getStatus());
        assertEquals(200, doViaProxy(filter, "10.0.0.1", "True-Client-IP", "203.0.113.51").getStatus(),
                "같은 프록시를 거쳐도 방문자가 다르면 따로 센다(모두가 한 묶음으로 막히면 안 됨)");
    }

    @Test
    void theSameVisitorIsLimitedEvenWhenItArrivesThroughDifferentProxies() throws Exception {
        // Cloudflare는 여러 서버에서 요청을 넘겨주므로 연결 주소가 바뀔 수 있다. 기준은 헤더의 방문자 IP다.
        RateLimitFilter filter = new RateLimitFilter("True-Client-IP");
        for (int i = 0; i < 20; i++) {
            doViaProxy(filter, "10.0.0." + (i + 1), "True-Client-IP", "203.0.113.60");
        }

        assertEquals(429, doViaProxy(filter, "10.0.0.99", "True-Client-IP", "203.0.113.60").getStatus());
    }

    @Test
    void withoutTheTrustedHeaderTheConnectionAddressIsUsed() throws Exception {
        RateLimitFilter filter = new RateLimitFilter("True-Client-IP");
        for (int i = 0; i < 20; i++) {
            doViaProxy(filter, "203.0.113.70", null, null);
        }

        assertEquals(429, doViaProxy(filter, "203.0.113.70", "True-Client-IP", "  ").getStatus(),
                "헤더가 비어 있으면 연결 주소로 센다");
    }

    @Test
    void theHeaderNameIsConfigurableForOtherPlatforms() throws Exception {
        RateLimitFilter filter = new RateLimitFilter("CF-Connecting-IP");
        for (int i = 0; i < 20; i++) {
            doViaProxy(filter, "10.0.0.1", "CF-Connecting-IP", "203.0.113.80");
        }

        assertEquals(429, doViaProxy(filter, "10.0.0.1", "CF-Connecting-IP", "203.0.113.80").getStatus());
        assertEquals(200, doViaProxy(filter, "10.0.0.1", "True-Client-IP", "203.0.113.80").getStatus(),
                "설정하지 않은 헤더는 보지 않는다(연결 주소 10.0.0.1은 아직 한 번도 안 셌으므로 통과)");
    }

    @Test
    void anEmptyHeaderSettingIgnoresClientSuppliedHeadersEntirely() throws Exception {
        RateLimitFilter filter = new RateLimitFilter("");
        for (int i = 0; i < 20; i++) {
            doViaProxy(filter, "203.0.113.90", "True-Client-IP", "198.51.100." + i);
        }

        assertEquals(429, doViaProxy(filter, "203.0.113.90", "True-Client-IP", "198.51.100.200").getStatus());
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
