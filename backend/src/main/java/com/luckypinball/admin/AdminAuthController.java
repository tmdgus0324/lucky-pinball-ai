package com.luckypinball.admin;

import com.luckypinball.common.ApiException;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 로그인/로그아웃. 계정이 하나뿐이고(환경변수로 설정) 회원가입 기능이 없어서
 * DB에 사용자 테이블을 두지 않았다 — {@link AdminSessionStore}에 발급한 토큰만 기억한다.
 */
@RestController
public class AdminAuthController {

    private final String adminUsername;
    private final String adminPassword;
    private final AdminSessionStore sessionStore;

    public AdminAuthController(@Value("${admin.username}") String adminUsername,
                                @Value("${admin.password}") String adminPassword,
                                AdminSessionStore sessionStore) {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.sessionStore = sessionStore;
    }

    @PostMapping("/api/admin/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        // adminPassword가 빈 문자열(=ADMIN_PASSWORD 미설정)이면 무조건 실패시킨다 — 배포 시
        // 환경변수를 깜빡해도 "비밀번호 없이 로그인 가능" 사고로 이어지지 않게 하는 안전장치.
        boolean match = !adminPassword.isBlank()
                && adminUsername.equals(request.username())
                && adminPassword.equals(request.password());

        if (!match) {
            throw ApiException.unauthorized("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = sessionStore.issue();
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/api/admin/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        sessionStore.revoke(AdminAuthFilter.extractToken(authorization));
        return ResponseEntity.noContent().build();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }
}
