package com.luckypinball;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luckypinball.fortune.FortuneResult;
import com.luckypinball.fortune.FortuneService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * 실제 포트로 서버를 띄우고 HTTP로 호출하는 통합 테스트. 필터(추적 ID·요청 제한·관리자 인증), 컨트롤러,
 * 예외 처리, JPA 저장까지 화면이 실제로 지나는 길을 그대로 지난다. Claude만 가짜로 바꾼다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "admin.password=test-pass")
class ApiFlowIntegrationTest {

    /** 점수를 고정해 돌려주고 호출 횟수를 세는 가짜 Claude. 동시 요청 테스트를 위해 조금 느리게 응답한다. */
    static class CountingFortuneService implements FortuneService {
        final AtomicInteger calls = new AtomicInteger();
        volatile long delayMillis = 0;

        @Override
        public FortuneResult analyze(String name, LocalDate birthDate) {
            calls.incrementAndGet();
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return new FortuneResult(77, 7, "테스트 운세");
        }
    }

    @TestConfiguration
    static class FakeAiConfig {
        @Bean
        @Primary
        CountingFortuneService countingFortuneService() {
            return new CountingFortuneService();
        }
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    CountingFortuneService ai;

    private RestClient client;

    record Response(int status, Map<String, Object> body, String traceId) {
        Object get(String key) {
            return body == null ? null : body.get(key);
        }
    }

    @BeforeEach
    void setUp() {
        client = RestClient.builder().baseUrl("http://localhost:" + port).build();
        ai.calls.set(0);
        ai.delayMillis = 0;
    }

    @SuppressWarnings("unchecked")
    private Response call(HttpMethod method, String path, Object body, String token) {
        RestClient.RequestBodySpec spec = client.method(method).uri(path);
        if (token != null) {
            spec = spec.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            spec = spec.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return spec.exchange((request, response) -> {
            Map<String, Object> parsed = null;
            String contentType = String.valueOf(response.getHeaders().getContentType());
            if (contentType.contains("json")) {
                parsed = response.bodyTo(Map.class);
            }
            return new Response(response.getStatusCode().value(), parsed, response.getHeaders().getFirst("X-Trace-Id"));
        }, false);
    }

    private Response post(String path, Object body) {
        return call(HttpMethod.POST, path, body, null);
    }

    private long register(String name, String birthDate) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("birthDate", birthDate);
        Response r = post("/api/player", body);
        assertEquals(201, r.status(), String.valueOf(r.body()));   // 등록은 201 Created
        return ((Number) r.get("playerId")).longValue();
    }

    @Test
    void theWholeGameFlowWorksOverHttp() {
        long alice = register("흐름앨리스", "1990-01-01");
        long bob = register("흐름밥", null);   // 생년월일 없이 참여

        Response fortune = post("/api/fortune", Map.of("playerId", alice));
        assertEquals(200, fortune.status());
        assertEquals("AI", fortune.get("source"));
        assertEquals(77, fortune.get("fortuneScore"));
        assertNotNull(fortune.traceId(), "모든 응답에 추적 ID 헤더가 붙는다");

        Response created = post("/api/game/create", Map.of("playerIds", List.of(alice, bob)));
        assertEquals(201, created.status());
        String gameId = (String) created.get("gameId");
        assertEquals(2, ((List<?>) created.get("participants")).size());
        assertEquals(1, ai.calls.get(), "게임 생성은 방금 받은 운세(캐시)를 재사용해서 AI를 다시 부르지 않는다");

        Response started = post("/api/game/start", Map.of("gameId", gameId));
        assertEquals(200, started.status());
        assertEquals("STARTED", started.get("status"));

        Response result = post("/api/game/result", Map.of("gameId", gameId, "finishOrder", List.of(bob, alice)));
        assertEquals(200, result.status());
        assertEquals("흐름앨리스", result.get("selectedName"), "가장 늦게 도착한 사람이 당첨");

        Response fetched = call(HttpMethod.GET, "/api/game/result/" + gameId, null, null);
        assertEquals(200, fetched.status());
        assertEquals("흐름앨리스", fetched.get("selectedName"));
    }

    @Test
    void errorsComeBackWithTheRightStatusAndATraceId() {
        long a = register("오류A", null);
        long b = register("오류B", null);

        Response tooFew = post("/api/game/create", Map.of("playerIds", List.of(a)));
        assertEquals(400, tooFew.status());
        assertNotNull(tooFew.get("error"));
        assertEquals(tooFew.traceId(), tooFew.get("traceId"), "응답 본문과 헤더의 추적 ID가 같아야 로그에서 찾을 수 있다");

        assertEquals(404, post("/api/game/start", Map.of("gameId", "no-such-game")).status());
        assertEquals(404, post("/api/fortune", Map.of("playerId", 999999)).status());

        String gameId = (String) post("/api/game/create", Map.of("playerIds", List.of(a, b))).get("gameId");
        assertEquals(409, post("/api/game/result", Map.of("gameId", gameId, "finishOrder", List.of(a, b))).status(),
                "시작하지 않은 게임의 결과는 받지 않는다");

        post("/api/game/start", Map.of("gameId", gameId));
        assertEquals(400, post("/api/game/result", Map.of("gameId", gameId, "finishOrder", List.of(a, a))).status(),
                "같은 사람이 두 번 들어간 순위는 거절");
        assertEquals(200, post("/api/game/result", Map.of("gameId", gameId, "finishOrder", List.of(a, b))).status());
        assertEquals(409, post("/api/game/result", Map.of("gameId", gameId, "finishOrder", List.of(b, a))).status(),
                "결과는 한 번만");
        assertEquals(409, post("/api/game/start", Map.of("gameId", gameId)).status(), "끝난 게임은 다시 시작할 수 없다");

        Response malformed = post("/api/player", "{bad json");
        assertEquals(400, malformed.status(), "깨진 JSON은 서버 오류(500)가 아니라 400");
    }

    @Test
    void simultaneousFortuneRequestsForTheSameIdentityCallTheAiOnceEvenOverHttp() throws Exception {
        ai.delayMillis = 400;
        List<Long> players = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            players.add(register("동시HTTP", "1985-05-05"));
        }

        ExecutorService pool = Executors.newFixedThreadPool(players.size());
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Response>> futures = new ArrayList<>();
            for (long id : players) {
                futures.add(pool.submit(() -> {
                    go.await();
                    return post("/api/fortune", Map.of("playerId", id));
                }));
            }
            go.countDown();
            List<Object> sources = new ArrayList<>();
            for (Future<Response> f : futures) {
                Response r = f.get(10, TimeUnit.SECONDS);
                assertEquals(200, r.status());
                sources.add(r.get("source"));
            }
            assertEquals(1, ai.calls.get(), "같은 신원 4건이 동시에 와도 AI는 한 번");
            assertEquals(3, sources.stream().filter("CACHE"::equals).count());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void adminApisNeedALoginAndShowTheSavedGames() {
        long a = register("관리A", null);
        long b = register("관리B", null);
        String gameId = (String) post("/api/game/create", Map.of("playerIds", List.of(a, b))).get("gameId");

        assertEquals(401, call(HttpMethod.GET, "/api/admin/games", null, null).status(), "토큰 없이는 막힌다");
        assertEquals(401, post("/api/admin/login", Map.of("username", "admin", "password", "wrong")).status());

        Response login = post("/api/admin/login", Map.of("username", "admin", "password", "test-pass"));
        assertEquals(200, login.status());
        String token = (String) login.get("token");

        RestClient.RequestHeadersSpec<?> games = client.get().uri("/api/admin/games").header("Authorization", "Bearer " + token);
        List<?> list = games.retrieve().body(List.class);
        assertNotNull(list);
        assertTrue(list.stream().anyMatch(g -> g instanceof Map<?, ?> m && gameId.equals(m.get("gameId"))),
                "방금 만든 게임이 관리자 목록에 보여야 한다");
    }
}
