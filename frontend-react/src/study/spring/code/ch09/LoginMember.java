package com.example.reservation.member;

import java.io.Serializable;

// 세션에 넣어 둘 로그인 정보. 비밀번호 같은 민감한 값은 넣지 않는다.
// 세션을 파일·DB·Redis에 저장하는 환경(서버 여러 대)에서도 쓸 수 있게 Serializable로 만든다.
public record LoginMember(Long id, String name) implements Serializable {

    // 세션에 저장할 때 쓰는 이름. 여러 곳에서 같은 글자를 쓰므로 상수로 둔다.
    public static final String SESSION_KEY = "LOGIN_MEMBER";
}
