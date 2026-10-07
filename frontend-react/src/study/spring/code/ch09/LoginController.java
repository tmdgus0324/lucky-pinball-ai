package com.example.reservation.member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    public record LoginRequest(String loginId, String password) {
    }

    // 예제용 회원 한 명. 실제로는 DB에서 찾고, 비밀번호는 BCrypt 같은 해시로 저장해 비교한다.
    private static final Map<String, String> PASSWORDS = Map.of("guest1", "pass1234");
    private static final Map<String, LoginMember> MEMBERS = Map.of("guest1", new LoginMember(1L, "김민수"));

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (!request.password().equals(PASSWORDS.get(request.loginId()))) {
            return ResponseEntity.status(401).body("아이디 또는 비밀번호가 맞지 않습니다");
        }
        // 세션을 만든다(없으면 새로 만들고, 있으면 그것을 쓴다).
        // 톰캣이 세션 ID를 만들어 JSESSIONID 쿠키로 브라우저에 보낸다.
        HttpSession session = httpRequest.getSession();
        session.setAttribute(LoginMember.SESSION_KEY, MEMBERS.get(request.loginId()));
        return ResponseEntity.ok(MEMBERS.get(request.loginId()).name() + "님 로그인");
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);   // false: 없으면 새로 만들지 않는다
        if (session != null) {
            session.invalidate();   // 서버에서 세션을 지운다. 같은 쿠키로 다시 와도 로그인 안 된 상태
        }
        return ResponseEntity.noContent().build();
    }
}
