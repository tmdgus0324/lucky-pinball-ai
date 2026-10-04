package com.luckypinball.fortune;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FortuneResultJpaRepository extends JpaRepository<FortuneResultEntity, Long> {
    List<FortuneResultEntity> findByPlayerIdOrderByCreatedDateAsc(Long playerId);

    /**
     * 참가자 목록 화면용 — 전체 이력을 한 번에 읽어 호출하는 쪽에서 playerId별로 묶는다.
     * 참가자마다 위의 조회를 부르면 참가자 수만큼 쿼리가 나간다(N+1). createdDate는 날짜까지만이라
     * 같은 날 행끼리의 순서를 id로 고정한다.
     */
    List<FortuneResultEntity> findAllByOrderByCreatedDateAscIdAsc();

    /**
     * 위 조회를 playerId별로 묶은 결과. 각 목록은 오래된 순이다.
     * groupingBy는 키가 null이면 예외를 던지므로, playerId가 없는 행은 뺀다(어차피 어느 참가자에도 안 보인다).
     */
    default Map<Long, List<FortuneResultEntity>> findAllGroupedByPlayerId() {
        return findAllByOrderByCreatedDateAscIdAsc().stream()
                .filter(result -> result.getPlayerId() != null)
                .collect(Collectors.groupingBy(FortuneResultEntity::getPlayerId));
    }

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
