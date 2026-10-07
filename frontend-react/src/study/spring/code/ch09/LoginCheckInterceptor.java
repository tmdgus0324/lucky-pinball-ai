package com.example.reservation.member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// 로그인이 필요한 주소 앞에서 세션을 확인한다(5장의 인터셉터).
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoginCheckInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(LoginMember.SESSION_KEY) == null) {
            log.info("로그인 안 됨: {}", request.getRequestURI());
            // 화면(JSP·Thymeleaf)이라면 여기서 response.sendRedirect("/login")으로 로그인 화면에 보낸다.
            response.setStatus(401);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("로그인이 필요합니다");
            return false;   // 컨트롤러를 실행하지 않는다
        }
        return true;
    }
}
