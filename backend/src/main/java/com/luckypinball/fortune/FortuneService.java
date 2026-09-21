package com.luckypinball.fortune;

import java.time.LocalDate;

/**
 * 오늘의 운세를 만들어내는 생성기 추상화.
 * 지금은 {@link MockFortuneGenerator}가 구현하고, 나중에 실제 OpenAI 연동 구현체로 교체 가능하다.
 */
public interface FortuneService {
    FortuneResult analyze(String name, LocalDate birthDate);
}
