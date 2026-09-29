package com.luckypinball.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AdminAuthFilterTest {

    private final FilterChain noop = (req, res) -> {
    };

    @Test
    void blocksAdminPathWithoutToken() throws Exception {
        AdminSessionStore store = new AdminSessionStore();
        AdminAuthFilter filter = new AdminAuthFilter(store);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/players");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200); // chain이 실제로는 아무 것도 안 하므로, 통과했을 때의 기본값을 미리 세팅

        filter.doFilter(request, response, noop);

        assertEquals(401, response.getStatus());
    }

    @Test
    void allowsAdminPathWithValidToken() throws Exception {
        AdminSessionStore store = new AdminSessionStore();
        AdminAuthFilter filter = new AdminAuthFilter(store);
        String token = store.issue();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/players");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, noop);

        assertEquals(200, response.getStatus());
    }

    @Test
    void blocksAdminPathWithInvalidToken() throws Exception {
        AdminSessionStore store = new AdminSessionStore();
        AdminAuthFilter filter = new AdminAuthFilter(store);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/players");
        request.addHeader("Authorization", "Bearer not-a-real-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, noop);

        assertEquals(401, response.getStatus());
    }

    @Test
    void loginPathIsNeverBlocked() throws Exception {
        AdminSessionStore store = new AdminSessionStore();
        AdminAuthFilter filter = new AdminAuthFilter(store);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, noop);

        assertEquals(200, response.getStatus());
    }

    @Test
    void corsPreflightIsNeverBlockedEvenWithoutToken() throws Exception {
        // 브라우저가 실제 요청 전에 토큰 없이 보내는 CORS preflight(OPTIONS)를 여기서 막으면,
        // 스프링의 CORS 응답 헤더 처리까지 못 가서 실제 요청이 브라우저 단에서 통째로
        // 실패한다(Failed to fetch) — 실제로 겪은 버그라 회귀 방지용으로 남겨둔다.
        AdminSessionStore store = new AdminSessionStore();
        AdminAuthFilter filter = new AdminAuthFilter(store);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/admin/players");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, noop);

        assertEquals(200, response.getStatus());
    }

    @Test
    void nonAdminPathIsNeverBlocked() throws Exception {
        AdminSessionStore store = new AdminSessionStore();
        AdminAuthFilter filter = new AdminAuthFilter(store);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/players/reusable");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, noop);

        assertEquals(200, response.getStatus());
    }
}
