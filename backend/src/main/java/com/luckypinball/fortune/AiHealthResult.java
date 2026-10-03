package com.luckypinball.fortune;

import java.time.Instant;

/**
 * 관리자 화면 "AI 연결 확인" 결과. 관리자 전용 응답이라 사용자용 오류 문구보다 원인과 조치를 구체적으로 적는다.
 * 다만 Claude의 원문 오류 본문이나 키 값은 싣지 않는다.
 *
 * @param status         OK | KEY_MISSING | AUTH_FAILED | CREDIT_EXHAUSTED | RATE_LIMITED | TIMEOUT | UNREACHABLE | UPSTREAM_ERROR | ERROR
 * @param upstreamStatus Anthropic이 돌려준 HTTP 상태 코드(응답을 받지 못했으면 null)
 */
public record AiHealthResult(
        boolean ok,
        String status,
        String message,
        Integer upstreamStatus,
        long latencyMillis,
        String model,
        long timeoutSeconds,
        boolean fallbackEnabled,
        Instant checkedAt
) {
}
