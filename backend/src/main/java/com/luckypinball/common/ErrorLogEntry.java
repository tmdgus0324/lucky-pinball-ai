package com.luckypinball.common;

import java.time.Instant;

/** traceId: 이 오류가 난 요청의 추적 ID. 서버 로그의 같은 ID를 검색하면 그 요청 전체 흐름이 나온다. */
public record ErrorLogEntry(Instant timestamp, String path, String message, String traceId) {
}
