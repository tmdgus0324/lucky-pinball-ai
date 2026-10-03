package com.luckypinball.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class GlobalExceptionHandlerTest {

    private final ErrorLogStore store = new ErrorLogStore();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(store);
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/player");

    private ListAppender<ILoggingEvent> appender;
    private Logger handlerLogger;

    @BeforeEach
    void setUp() {
        MDC.put(TraceId.MDC_KEY, "abc12345");
        handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        appender = new ListAppender<>();
        appender.start();
        handlerLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        handlerLogger.detachAppender(appender);
        MDC.remove(TraceId.MDC_KEY);
    }

    @Test
    void unexpectedExceptionReturnsGenericMessageAndTraceIdWithoutLeakingTheCause() {
        ResponseEntity<Map<String, String>> response =
                handler.handleUnexpected(new RuntimeException("DB password is hunter2"), request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Unexpected error", response.getBody().get("error"));
        assertEquals("abc12345", response.getBody().get("traceId"));
        assertFalse(response.getBody().toString().contains("hunter2"), "예외 메시지가 응답에 새면 안 된다");
    }

    @Test
    void unexpectedExceptionIsLoggedAsErrorWithStackTraceAndTraceId() {
        handler.handleUnexpected(new RuntimeException("boom"), request);

        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertNotNull(event.getThrowableProxy(), "스택트레이스가 로그에 남아야 한다");
        assertEquals("boom", event.getThrowableProxy().getMessage());
        assertEquals("abc12345", event.getMDCPropertyMap().get(TraceId.MDC_KEY), "로그 줄에 같은 traceId가 붙어야 한다");
    }

    @Test
    void errorLogStoreKeepsTheTraceIdSoAdminCanLookItUp() {
        handler.handleUnexpected(new RuntimeException("boom"), request);

        ErrorLogEntry entry = store.recent().get(0);
        assertEquals("abc12345", entry.traceId());
        assertEquals("/api/player", entry.path());
    }

    @Test
    void clientErrorsAreWarningsWithoutStackTrace() {
        handler.handleApiException(ApiException.notFound("Player not found: 99"), request);

        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertNull(event.getThrowableProxy(), "4xx는 스택트레이스로 로그를 어지럽히지 않는다");
    }

    @Test
    void upstreamFailureIsLoggedAsErrorWithTheOriginalCauseChained() {
        ApiException failure = ApiException.upstreamFailure("Claude API 호출에 실패했습니다", new IllegalStateException("401 invalid key"));

        ResponseEntity<Map<String, String>> response = handler.handleApiException(failure, request);

        assertEquals(502, response.getStatusCode().value());
        assertEquals("abc12345", response.getBody().get("traceId"));
        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals("401 invalid key", event.getThrowableProxy().getCause().getMessage(), "원인 예외가 Caused by로 남아야 한다");
    }

    @Test
    void malformedJsonIsA400NotA500() {
        ResponseEntity<Map<String, String>> response = handler.handleMalformedRequest(
                new HttpMessageNotReadableException("JSON parse error", new MockHttpInputMessage(new byte[0])), request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("요청 형식이 올바르지 않습니다.", response.getBody().get("error"));
        assertEquals(Level.WARN, appender.list.get(0).getLevel());
    }

    @Test
    void wrongMethodIs405AndDoesNotFillTheAdminErrorLog() {
        ResponseEntity<Map<String, String>> response =
                handler.handleMethodNotAllowed(new HttpRequestMethodNotSupportedException("DELETE"), request);

        assertEquals(405, response.getStatusCode().value());
        assertTrue(store.recent().isEmpty(), "봇이 잘못된 메서드로 찔러본 요청이 관리자 오류 로그를 채우면 안 된다");
    }

    @Test
    void responseOmitsTraceIdWhenThereIsNoCurrentRequestId() {
        MDC.remove(TraceId.MDC_KEY);

        ResponseEntity<Map<String, String>> response = handler.handleUnexpected(new RuntimeException("x"), request);

        assertFalse(response.getBody().containsKey("traceId"));
    }
}
