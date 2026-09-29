package com.luckypinball.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luckypinball.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class AdminAuthControllerTest {

    @Test
    void loginWithCorrectCredentialsIssuesAValidToken() {
        AdminSessionStore store = new AdminSessionStore();
        AdminAuthController controller = new AdminAuthController("admin", "secret", store);

        ResponseEntity<java.util.Map<String, String>> response =
                controller.login(new AdminAuthController.LoginRequest("admin", "secret"));

        String token = response.getBody().get("token");
        assertTrue(store.isValid(token));
    }

    @Test
    void loginWithWrongPasswordFails() {
        AdminSessionStore store = new AdminSessionStore();
        AdminAuthController controller = new AdminAuthController("admin", "secret", store);

        ApiException ex = assertThrows(ApiException.class,
                () -> controller.login(new AdminAuthController.LoginRequest("admin", "wrong")));

        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void loginFailsWhenAdminPasswordIsNotConfigured() {
        // ADMIN_PASSWORD를 안 정한 배포 환경을 흉내낸다 — 빈 비밀번호로는 절대 로그인이
        // 성공하면 안 된다(누구나 관리자 화면에 들어가는 사고 방지).
        AdminSessionStore store = new AdminSessionStore();
        AdminAuthController controller = new AdminAuthController("admin", "", store);

        assertThrows(ApiException.class, () -> controller.login(new AdminAuthController.LoginRequest("admin", "")));
    }

    @Test
    void logoutRevokesTheToken() {
        AdminSessionStore store = new AdminSessionStore();
        AdminAuthController controller = new AdminAuthController("admin", "secret", store);
        String token = controller.login(new AdminAuthController.LoginRequest("admin", "secret")).getBody().get("token");

        controller.logout("Bearer " + token);

        assertFalse(store.isValid(token));
    }
}
