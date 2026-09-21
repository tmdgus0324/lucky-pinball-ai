package com.luckypinball.fortune;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FortuneResultJpaRepository extends JpaRepository<FortuneResultEntity, Long> {
    List<FortuneResultEntity> findByPlayerIdOrderByCreatedDateAsc(Long playerId);
}
