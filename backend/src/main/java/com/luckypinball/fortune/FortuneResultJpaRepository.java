package com.luckypinball.fortune;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FortuneResultJpaRepository extends JpaRepository<FortuneResultEntity, Long> {
    List<FortuneResultEntity> findByPlayerIdOrderByCreatedDateAsc(Long playerId);

    /**
     * "같은 사주"(이름+생년월일) 캐시 조회 — 있으면 Claude를 다시 호출하지 않고 재사용한다.
     * sources로 재사용해도 되는 출처(AI, CACHE)만 넘긴다. FALLBACK(임시 점수)이 섞이면 그 신원이
     * 계속 가짜 점수를 쓰게 되므로 반드시 걸러야 한다.
     */
    Optional<FortuneResultEntity> findFirstByNameAndBirthDateAndSourceInOrderByCreatedDateAsc(
            String name, LocalDate birthDate, Collection<String> sources);

    /** 이 참가자(playerId)가 이미 받은 임시 점수 — 게임 생성 때 같은 점수를 다시 쓰기 위한 조회. */
    Optional<FortuneResultEntity> findFirstByPlayerIdAndSourceOrderByCreatedDateDesc(Long playerId, String source);
}
