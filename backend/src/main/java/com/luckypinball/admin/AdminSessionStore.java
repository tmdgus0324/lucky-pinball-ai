package com.luckypinball.admin;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 로그인 성공 시 발급하는 토큰을 메모리에 보관한다 — DB에 세션 테이블을 두거나 JWT처럼
 * 서명·검증 로직을 따로 둘 만큼 복잡할 필요가 없다(관리자 계정이 하나뿐이고, 인스턴스도
 * 하나뿐이다). 서버가 재시작되면 토큰이 전부 사라져서 다시 로그인해야 하는데, 관리자용
 * 기능이라 크게 불편하지 않다.
 *
 * RateLimitFilter와 같은 이유로 인메모리 Map을 쓴다 — 서버를 여러 대로 늘리면 이 방식은
 * 안 통하고, 그때는 Redis 등 공유 저장소로 옮겨야 한다.
 */
@Component
public class AdminSessionStore {

    private static final long TTL_MILLIS = 12 * 60 * 60 * 1000L; // 12시간

    private final Map<String, Instant> expiryByToken = new ConcurrentHashMap<>();

    public String issue() {
        String token = UUID.randomUUID().toString();
        expiryByToken.put(token, Instant.now().plusMillis(TTL_MILLIS));
        return token;
    }

    public boolean isValid(String token) {
        if (token == null) return false;
        Instant expiry = expiryByToken.get(token);
        if (expiry == null) return false;
        if (Instant.now().isAfter(expiry)) {
            expiryByToken.remove(token);
            return false;
        }
        return true;
    }

    public void revoke(String token) {
        if (token != null) expiryByToken.remove(token);
    }
}
