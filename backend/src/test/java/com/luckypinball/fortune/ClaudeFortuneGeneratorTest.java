package com.luckypinball.fortune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luckypinball.common.ApiException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Claude 호출 실패가 사용자에게 의미 있는 상태 코드로 나뉘는지를, 실제 네트워크 호출로 검증한다.
 * SDK의 fromEnv()는 시스템 프로퍼티(anthropic.baseUrl / anthropic.apiKey)를 먼저 읽으므로,
 * 로컬 JDK HttpServer를 "고장난 Claude"로 세워서 그쪽을 바라보게 한다 — 진짜 API는 호출하지 않는다.
 */
class ClaudeFortuneGeneratorTest {

    private static final LocalDate BIRTH = LocalDate.of(1990, 1, 1);

    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void startFakeClaude() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // 기본 실행기는 단일 스레드라, 느린 첫 요청이 붙잡고 있는 동안 재시도 요청이 처리되지 않아 횟수를 셀 수 없다.
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.setProperty("anthropic.baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        System.setProperty("anthropic.apiKey", "test-key-not-real");
    }

    @AfterEach
    void stopFakeClaude() {
        server.stop(0);
        System.clearProperty("anthropic.baseUrl");
        System.clearProperty("anthropic.apiKey");
    }

    private void respondWith(int status, String body, long delayMillis) {
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            try {
                if (delayMillis > 0) {
                    Thread.sleep(delayMillis);
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException | IOException ignored) {
                // 클라이언트가 먼저 끊은 경우(타임아웃) — 정상적인 시나리오다.
            } finally {
                exchange.close();
            }
        });
    }

    private ApiException analyzeAndCatch(ClaudeFortuneGenerator generator) {
        return assertThrows(ApiException.class, () -> generator.analyze("테스트", BIRTH));
    }

    @Test
    void slowClaudeBecomesA504WithinTheConfiguredTimeoutInsteadOfHangingForMinutes() {
        respondWith(200, "{}", 5_000);
        ClaudeFortuneGenerator generator = new ClaudeFortuneGenerator(1, 0);

        long startedAt = System.currentTimeMillis();
        ApiException e = analyzeAndCatch(generator);
        long elapsed = System.currentTimeMillis() - startedAt;

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, e.getStatus());
        assertEquals(ClaudeFortuneGenerator.TIMEOUT_MESSAGE, e.getMessage());
        assertTrue(elapsed < 4_000, "SDK 기본값(10분)이 아니라 설정한 1초 근처에서 끊겨야 한다: " + elapsed + "ms");
        assertNotNull(e.getCause(), "원인은 로그용으로 cause에 남아 있어야 한다");
    }

    @Test
    void timeoutIsRetriedOnceWhenConfiguredSoWorstCaseIsBounded() {
        respondWith(200, "{}", 5_000);
        ClaudeFortuneGenerator generator = new ClaudeFortuneGenerator(1, 1);

        ApiException e = analyzeAndCatch(generator);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, e.getStatus());
        assertEquals(2, requests.get(), "최초 1회 + 재시도 1회");
    }

    @Test
    void rateLimitFromClaudeBecomes503WithABusyMessage() {
        respondWith(429, "{\"type\":\"error\",\"error\":{\"type\":\"rate_limit_error\",\"message\":\"slow down\"}}", 0);

        ApiException e = analyzeAndCatch(new ClaudeFortuneGenerator(5, 0));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
        assertEquals(ClaudeFortuneGenerator.BUSY_MESSAGE, e.getMessage());
    }

    @Test
    void otherUpstreamErrorsBecome502AndNeverLeakTheUpstreamBodyToTheUser() {
        respondWith(401, "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\",\"message\":\"invalid x-api-key SECRET-DETAIL\"}}", 0);

        ApiException e = analyzeAndCatch(new ClaudeFortuneGenerator(5, 0));

        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        assertEquals(ClaudeFortuneGenerator.UPSTREAM_FAILURE_MESSAGE, e.getMessage());
        assertFalse(e.getMessage().contains("SECRET-DETAIL"), "Claude의 원문 오류를 사용자 응답에 싣지 않는다");
        assertEquals(1, requests.get(), "401은 재시도해도 소용없으므로 재시도하지 않는다");
    }

    @Test
    void serverErrorsAreRetriedAccordingToMaxRetries() {
        respondWith(500, "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"boom\"}}", 0);

        ApiException e = analyzeAndCatch(new ClaudeFortuneGenerator(5, 1));

        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        assertEquals(2, requests.get(), "5xx는 재시도 대상이다: 최초 1회 + 재시도 1회");
    }

    @Test
    void missingApiKeyIsAUpstreamFailureNotAServerCrash() {
        System.clearProperty("anthropic.apiKey");
        // 환경변수에도 키가 없는 CI에서만 의미가 있다 — 개발자 PC에 ANTHROPIC_API_KEY가 있으면 이 시나리오를 만들 수 없다.
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getenv("ANTHROPIC_API_KEY") == null);

        ApiException e = analyzeAndCatch(new ClaudeFortuneGenerator(5, 0));

        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        assertEquals(ClaudeFortuneGenerator.UPSTREAM_FAILURE_MESSAGE, e.getMessage());
        assertFalse(e.getMessage().contains("ANTHROPIC"), "환경변수 이름 같은 서버 설정을 사용자에게 노출하지 않는다");
    }
}
