package com.luckypinball.player;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerJpaRepository extends JpaRepository<PlayerEntity, Long> {

    List<PlayerEntity> findAllByOrderByCreatedAtDesc();
}
