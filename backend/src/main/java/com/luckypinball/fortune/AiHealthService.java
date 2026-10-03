package com.luckypinball.fortune;

import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.NoCredentialsException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.luckypinball.common.ApiException;
import java.io.InterruptedIOException;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 관리자 화면의 "AI 연결 확인". 게임을 돌려보지 않고도 Claude API가 지금 되는지, 안 된다면 왜 안 되는지를
 * 최소 비용(출력 토큰 1개)의 실제 호출 한 번으로 확인한다.
 *
 * 버튼을 연타하면 그만큼 유료 호출이 나가므로, 점검 사이에 최소 간격을 둔다(관리자 전용이라 이 정도면 충분하다).
 */
@Service
public class AiHealthService {

    private static final Logger log = LoggerFactory.getLogger(AiHealthService.class);

    static final long MIN_INTERVAL_MILLIS = 5_000;

    private final ClaudeFortuneGenerator generator;
    private final boolean fallbackEnabled;

    private long lastCheckStartedAt;

    public AiHealthService(ClaudeFortuneGenerator generator,
                           @Value("${fortune.fallback-enabled:true}") boolean fallbackEnabled) {
        this.generator = generator;
        this.fallbackEnabled = fallbackEnabled;
    }

    public AiHealthResult check() {
        claimSlot();

        long startedAt = System.currentTimeMillis();
        try {
            pingRetryingADroppedConnectionOnce();
            long elapsed = System.currentTimeMillis() - startedAt;
            log.info("AI 연결 확인: 정상 {}ms", elapsed);
            return result(true, "OK", "Claude API가 정상 응답했습니다.", null, elapsed);
        } catch (RuntimeException e) {
            long elapsed = System.currentTimeMillis() - startedAt;
            AiHealthResult failed = classify(e, elapsed);
            log.warn("AI 연결 확인: 실패 status={} upstream={} {}ms 원인={}",
                    failed.status(), failed.upstreamStatus(), elapsed, FortuneQueryService.summarize(e));
            return failed;
        }
    }

    /**
     * 점검은 SDK 재시도를 꺼 두기 때문에(일시적 실패를 가리지 않으려고), 한동안 쉰 뒤 풀에 남은 연결이 서버 쪽에서 이미
     * 닫혀 있으면 그 한 번의 실패가 곧바로 "연결 불가"가 된다 — 실제로 멀쩡한 서비스를 장애로 오진했다.
     * 그래서 타임아웃이 아닌 연결 끊김(IO 오류)에 한해서만 새 연결로 한 번 더 시도한다. 진짜로 연결이 안 되면 두 번 다
     * 실패하고(연결 거부는 즉시 실패), 타임아웃·HTTP 오류 응답은 재시도하지 않는다.
     */
    private void pingRetryingADroppedConnectionOnce() {
        try {
            generator.ping();
        } catch (AnthropicIoException e) {
            if (isTimeout(e)) {
                throw e;
            }
            log.debug("AI 연결 확인: 연결이 끊겨 한 번 더 시도합니다 ({})", FortuneQueryService.summarize(e));
            generator.ping();
        }
    }

    /** 최소 간격 안에 다시 호출되면 거절한다. 호출 자체는 락 밖에서 하므로 느린 점검이 다른 요청을 막지 않는다. */
    private synchronized void claimSlot() {
        long now = System.currentTimeMillis();
        long wait = MIN_INTERVAL_MILLIS - (now - lastCheckStartedAt);
        if (wait > 0) {
            throw ApiException.tooManyRequests(
                    "방금 점검했습니다. " + ((wait + 999) / 1000) + "초 뒤에 다시 시도해주세요. (유료 API 호출이라 간격을 둡니다)");
        }
        lastCheckStartedAt = now;
    }

    private AiHealthResult classify(RuntimeException e, long elapsed) {
        // 서버에 API 키가 없는 경우. 처음엔 클라이언트 생성이 실패할 거라고 가정했지만, 실제로 SDK는 키가 없어도
        // 예외 없이 인증 없는 요청을 보내 Anthropic에게 401을 받는다(CI에서 발견, 실험으로 확인). 그래서 401 처리 쪽에서
        // "키가 아예 없는 경우"를 따로 구분한다. 여기는 SDK가 직접 자격 증명 없음을 알리거나 생성이 실패하는 경우용 안전망이다.
        if (e instanceof NoCredentialsException || e instanceof ApiException) {
            return keyMissing(null, elapsed);
        }
        if (e instanceof AnthropicIoException) {
            return isTimeout(e)
                    ? result(false, "TIMEOUT",
                            generator.timeoutSeconds() + "초 안에 응답이 없습니다. Anthropic 장애이거나 네트워크가 느릴 수 있습니다.", null, elapsed)
                    : result(false, "UNREACHABLE",
                            "Anthropic 서버에 연결할 수 없습니다. 서버의 네트워크 설정이나 Anthropic 장애를 확인하세요.", null, elapsed);
        }
        if (e instanceof AnthropicServiceException service) {
            int code = service.statusCode();
            if (e instanceof UnauthorizedException || e instanceof PermissionDeniedException) {
                // 키가 설정돼 있지 않은데 401이면 "키가 틀렸다"가 아니라 "키가 없다"다 — 조치가 완전히 다르다.
                if (!generator.apiKeyConfigured()) {
                    return keyMissing(code, elapsed);
                }
                return result(false, "AUTH_FAILED",
                        "API 키가 유효하지 않거나 권한이 없습니다. 키가 폐기·오타·만료되지 않았는지 확인하세요.", code, elapsed);
            }
            // 크레딧이 바닥나면 인증 오류가 아니라 400(invalid_request_error)으로 온다.
            if (code == 400 && String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT).contains("credit balance")) {
                return result(false, "CREDIT_EXHAUSTED",
                        "크레딧 잔액이 부족합니다. Anthropic 콘솔의 Billing에서 충전하세요.", code, elapsed);
            }
            if (e instanceof RateLimitException) {
                return result(false, "RATE_LIMITED",
                        "요청 한도를 초과했습니다(429). 잠시 후 다시 확인하세요.", code, elapsed);
            }
            if (code >= 500) {
                return result(false, "UPSTREAM_ERROR",
                        "Anthropic 서버 쪽 오류입니다(HTTP " + code + "). 잠시 후 다시 확인하세요.", code, elapsed);
            }
            return result(false, "ERROR", "예상하지 못한 응답을 받았습니다(HTTP " + code + "). 서버 로그를 확인하세요.", code, elapsed);
        }
        if (e instanceof AnthropicException) {
            return result(false, "ERROR", "응답을 처리하지 못했습니다. 서버 로그를 확인하세요.", null, elapsed);
        }
        return result(false, "ERROR", "점검 중 예상하지 못한 오류가 났습니다. 서버 로그를 확인하세요.", null, elapsed);
    }

    private AiHealthResult keyMissing(Integer upstreamStatus, long elapsed) {
        return result(false, "KEY_MISSING",
                "서버에 API 키가 설정되어 있지 않습니다. 서버의 ANTHROPIC_API_KEY 환경변수를 확인하세요.", upstreamStatus, elapsed);
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof InterruptedIOException) {
                return true;
            }
        }
        return false;
    }

    private AiHealthResult result(boolean ok, String status, String message, Integer upstreamStatus, long elapsed) {
        return new AiHealthResult(ok, status, message, upstreamStatus, elapsed,
                generator.model(), generator.timeoutSeconds(), fallbackEnabled, Instant.now());
    }
}
