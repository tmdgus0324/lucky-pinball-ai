package com.luckypinball.common;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * 요청 하나를 따라다니는 추적 ID. TraceIdFilter가 요청마다 만들어 MDC에 넣으면, 같은 요청에서 찍힌
 * 모든 로그(logging.pattern.level 참고)와 에러 응답이 같은 ID를 갖는다 — 사용자가 화면에 뜬
 * "오류 ID"를 알려주면 서버 로그에서 그 요청의 흐름을 바로 찾을 수 있다.
 */
public final class TraceId {

    public static final String MDC_KEY = "traceId";
    public static final String HEADER = "X-Trace-Id";

    private TraceId() {
    }

    /** 8자리 16진수 — 소리 내어 읽어줄 수 있을 만큼 짧고, 이 규모의 로그에선 겹칠 일이 거의 없다. */
    static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /** 현재 스레드가 처리 중인 요청의 추적 ID. 요청 밖(시작 시점 등)에서는 null. */
    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
