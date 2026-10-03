package com.luckypinball.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void putsTheSameIdInMdcDuringTheRequestAndInTheResponseHeader() throws Exception {
        List<String> seenInChain = new ArrayList<>();
        FilterChain chain = (req, res) -> seenInChain.add(TraceId.current());

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/players/reusable"), response, chain);

        String headerId = response.getHeader(TraceId.HEADER);
        assertNotNull(headerId);
        assertEquals(8, headerId.length());
        assertEquals(headerId, seenInChain.get(0), "요청 처리 중 MDC의 ID와 응답 헤더의 ID가 같아야 한다");
    }

    @Test
    void clearsMdcAfterTheRequestSoThreadReuseDoesNotLeakTheId() throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/x"), new MockHttpServletResponse(), (req, res) -> {
        });

        assertNull(TraceId.current(), "톰캣 스레드는 재사용되므로 요청이 끝나면 MDC가 비어 있어야 한다");
    }

    @Test
    void clearsMdcEvenWhenTheChainThrows() {
        FilterChain failing = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        assertThrows(IllegalStateException.class,
                () -> filter.doFilter(new MockHttpServletRequest("GET", "/x"), new MockHttpServletResponse(), failing));
        assertNull(TraceId.current());
    }

    @Test
    void eachRequestGetsADifferentId() throws Exception {
        MockHttpServletResponse first = new MockHttpServletResponse();
        MockHttpServletResponse second = new MockHttpServletResponse();
        FilterChain noop = (req, res) -> {
        };
        filter.doFilter(new MockHttpServletRequest("GET", "/x"), first, noop);
        filter.doFilter(new MockHttpServletRequest("GET", "/x"), second, noop);

        assertNotEquals(first.getHeader(TraceId.HEADER), second.getHeader(TraceId.HEADER));
    }

    @Test
    void ignoresAnIdSentByTheClient() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader(TraceId.HEADER, "fake\nINFO injected log line");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        String id = response.getHeader(TraceId.HEADER);
        assertTrue(id.matches("[0-9a-f]{8}"), "외부 입력이 아니라 서버가 만든 ID여야 한다: " + id);
    }

    @Test
    void errorBodiesBuiltByFiltersCarryTheCurrentId() {
        MDC.put(TraceId.MDC_KEY, "abc12345");
        try {
            assertEquals("{\"error\":\"로그인이 필요합니다.\",\"traceId\":\"abc12345\"}", ErrorBodies.json("로그인이 필요합니다."));
        } finally {
            MDC.remove(TraceId.MDC_KEY);
        }
        assertEquals("{\"error\":\"x\"}", ErrorBodies.json("x"), "요청 밖에서는 traceId 없이 error만");
    }
}
