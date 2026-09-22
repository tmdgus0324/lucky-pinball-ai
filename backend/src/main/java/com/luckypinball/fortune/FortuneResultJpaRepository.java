package com.luckypinball.fortune;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FortuneResultJpaRepository extends JpaRepository<FortuneResultEntity, Long> {
    List<FortuneResultEntity> findByPlayerIdOrderByCreatedDateAsc(Long playerId);

    /** "같은 사주"(이름+생년월일) 캐시 조회 — 있으면 Claude를 다시 호출하지 않고 재사용한다. */
    Optional<FortuneResultEntity> findFirstByNameAndBirthDateOrderByCreatedDateAsc(String name, LocalDate birthDate);
}
