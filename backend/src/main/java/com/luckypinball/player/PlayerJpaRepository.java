package com.luckypinball.player;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerJpaRepository extends JpaRepository<PlayerEntity, Long> {
}
