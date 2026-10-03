package com.luckypinball.fortune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luckypinball.common.ApiException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * 관리자 "AI 연결 확인"이 실패 원인을 제대로 구분해서 알려주는지, 가짜 Claude 서버(JDK HttpServer)로 실제 호출해 검증한다.
 * 진짜 API는 호출하지 않는다.
 */
class AiHealthServiceTest {

    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    private final CopyOnWriteArrayList<String> requestBodies = new CopyOnWriteArrayList<>();

    @BeforeEach
    void startFakeClaude() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
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
                requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                if (delayMillis > 0) {
                    Thread.sleep(delayMillis);
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException | IOException ignored) {
                // 클라이언트가 먼저 끊은 경우(타임아웃)
            } finally {
                exchange.close();
            }
        });
    }

    private static String error(String type, String message) {
        return "{\"type\":\"error\",\"error\":{\"type\":\"" + type + "\",\"message\":\"" + message + "\"}}";
    }

    private AiHealthService service(long timeoutSeconds, boolean fallbackEnabled) {
        // maxRetries를 크게 줘도 점검은 재시도하지 않아야 한다 — 아래 테스트가 요청 횟수 1로 그걸 확인한다.
        return new AiHealthService(new ClaudeFortuneGenerator(timeoutSeconds, 2), fallbackEnabled);
    }

    @Test
    void healthyClaudeIsReportedOkWithLatencyAndSettings() {
        respondWith(200, "{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-haiku-4-5\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"h\"}],\"stop_reason\":\"max_tokens\",\"stop_sequence\":null,"
                + "\"usage\":{\"input_tokens\":8,\"output_tokens\":1}}", 0);

        AiHealthResult result = service(5, true).check();

        assertTrue(result.ok(), result.message());
        assertEquals("OK", result.status());
        assertNull(result.upstreamStatus());
        assertEquals("claude-haiku-4-5", result.model());
        assertEquals(5, result.timeoutSeconds());
        assertTrue(result.fallbackEnabled());
        assertTrue(result.latencyMillis() >= 0);
    }

    @Test
    void theCheckIsAMinimalCallThatCostsAlmostNothing() {
        respondWith(200, "{\"id\":\"m\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-haiku-4-5\","
                + "\"content\":[],\"stop_reason\":\"max_tokens\",\"stop_sequence\":null,"
                + "\"usage\":{\"input_tokens\":8,\"output_tokens\":1}}", 0);

        service(5, true).check();

        assertEquals(1, requestBodies.size());
        assertTrue(requestBodies.get(0).contains("\"max_tokens\":1"), "출력 토큰 1개로 제한해야 한다: " + requestBodies.get(0));
    }

    @Test
    void invalidKeyIsAuthFailedAndIsNotRetried() {
        respondWith(401, error("authentication_error", "invalid x-api-key SECRET-DETAIL"), 0);

        AiHealthResult result = service(5, true).check();

        assertFalse(result.ok());
        assertEquals("AUTH_FAILED", result.status());
        assertEquals(401, result.upstreamStatus());
        assertFalse(result.message().contains("SECRET-DETAIL"), "원문 오류 본문을 응답에 싣지 않는다");
        assertEquals(1, requests.get());
    }

    @Test
    void exhaustedCreditIsToldApartFromAnAuthProblem() {
        // 크레딧이 바닥나면 인증 오류가 아니라 400으로 온다 — 관리자가 "키 문제"로 오해하면 엉뚱한 곳을 뒤지게 된다.
        respondWith(400, error("invalid_request_error",
                "Your credit balance is too low to access the Anthropic API. Please go to Plans & Billing."), 0);

        AiHealthResult result = service(5, true).check();

        assertEquals("CREDIT_EXHAUSTED", result.status());
        assertEquals(400, result.upstreamStatus());
        assertTrue(result.message().contains("Billing"));
    }

    @Test
    void otherBadRequestsAreGenericErrorsNotCreditProblems() {
        respondWith(400, error("invalid_request_error", "messages: field required"), 0);

        assertEquals("ERROR", service(5, true).check().status());
    }

    @Test
    void rateLimitIsReported() {
        respondWith(429, error("rate_limit_error", "slow down"), 0);

        AiHealthResult result = service(5, true).check();

        assertEquals("RATE_LIMITED", result.status());
        assertEquals(1, requests.get(), "점검은 재시도로 일시적 실패를 가리지 않는다");
    }

    @Test
    void upstreamServerErrorIsReportedWithItsStatusAndNotRetried() {
        respondWith(500, error("api_error", "boom"), 0);

        AiHealthResult result = service(5, true).check();

        assertEquals("UPSTREAM_ERROR", result.status());
        assertEquals(500, result.upstreamStatus());
        assertEquals(1, requests.get());
    }

    @Test
    void slowClaudeIsReportedAsTimeoutNearTheConfiguredLimit() {
        respondWith(200, "{}", 5_000);

        long startedAt = System.currentTimeMillis();
        AiHealthResult result = service(1, true).check();
        long elapsed = System.currentTimeMillis() - startedAt;

        assertEquals("TIMEOUT", result.status());
        assertNull(result.upstreamStatus());
        assertTrue(result.message().contains("1초"));
        assertTrue(elapsed < 4_000, "재시도 없이 설정한 시간 근처에서 끝나야 한다: " + elapsed + "ms");
    }

    @Test
    void aDroppedConnectionIsRetriedOnceSoAStalePooledConnectionIsNotMistakenForAnOutage() {
        // 한동안 쉰 뒤 점검하면 풀에 남아 있던 연결이 서버 쪽에서 이미 닫혀 있을 수 있다. 점검은 SDK 재시도를 꺼 두므로
        // (일시적 실패를 가리지 않으려고) 그대로 두면 멀쩡한 서비스를 "연결 불가"로 오진한다. 첫 요청에서 연결을 끊어 재현한다.
        server.createContext("/", exchange -> {
            int n = requests.incrementAndGet();
            try {
                exchange.getRequestBody().readAllBytes();
                if (n == 1) {
                    return; // 응답 없이 연결만 닫는다(finally)
                }
                byte[] bytes = ("{\"id\":\"m\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-haiku-4-5\","
                        + "\"content\":[],\"stop_reason\":\"max_tokens\",\"stop_sequence\":null,"
                        + "\"usage\":{\"input_tokens\":8,\"output_tokens\":1}}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (IOException ignored) {
                // 무시
            } finally {
                exchange.close();
            }
        });

        AiHealthResult result = service(5, true).check();

        assertTrue(result.ok(), result.status() + ": " + result.message());
        assertEquals(2, requests.get());
    }

    @Test
    void unreachableClaudeIsToldApartFromATimeout() {
        server.stop(0);

        AiHealthResult result = service(5, true).check();

        assertEquals("UNREACHABLE", result.status());
        assertNull(result.upstreamStatus());
    }

    @Test
    void missingApiKeyIsToldApartFromAWrongKey() {
        // SDK는 키가 없어도 예외 없이 인증 없는 요청을 보내고 Anthropic이 401을 돌려준다(실제 동작). 그 401을
        // "키가 틀렸다(AUTH_FAILED)"로 안내하면 관리자가 키를 새로 발급받는 등 엉뚱한 조치를 하게 된다.
        System.clearProperty("anthropic.apiKey");
        // 개발자 PC에 키 환경변수가 있으면 이 상황을 만들 수 없다(CI에서는 실행된다).
        Assumptions.assumeTrue(System.getenv("ANTHROPIC_API_KEY") == null && System.getenv("ANTHROPIC_AUTH_TOKEN") == null);
        respondWith(401, error("authentication_error", "x-api-key header is required"), 0);

        AiHealthResult result = service(5, true).check();

        assertEquals("KEY_MISSING", result.status());
        assertEquals(401, result.upstreamStatus());
        assertTrue(result.message().contains("ANTHROPIC_API_KEY"));
    }

    @Test
    void aConfiguredButRejectedKeyIsStillAuthFailed() {
        respondWith(401, error("authentication_error", "invalid x-api-key"), 0);   // setUp에서 apiKey 프로퍼티를 설정해 둠

        assertEquals("AUTH_FAILED", service(5, true).check().status());
    }

    @Test
    void pressingTheButtonRepeatedlyIsRejectedBecauseEachCheckIsAPaidCall() {
        respondWith(200, "{}", 0);
        AiHealthService service = service(5, true);
        service.check();

        ApiException e = assertThrows(ApiException.class, service::check);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
        assertEquals(1, requests.get(), "두 번째 호출은 Claude까지 가지 않아야 한다");
    }

    @Test
    void reportsWhetherFallbackIsEnabledSoAdminKnowsWhatUsersWillSee() {
        respondWith(401, error("authentication_error", "x"), 0);

        assertFalse(service(5, false).check().fallbackEnabled());
    }
}
