package com.luckypinball.game;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GameJpaRepository extends JpaRepository<GameEntity, String> {

    /**
     * findAll()만 쓰면 게임이 N개일 때 participants/ranks를 LAZY로 따로 불러오느라 쿼리가
     * N번(참가자) + N번(순위) 더 나가는 전형적인 N+1이 생긴다 — fetch join으로 한 번에 가져온다.
     * 두 컬렉션을 동시에 fetch join하면 SQL 레벨에서는 행이 늘어나지만(participants × ranks),
     * DISTINCT로 GameEntity 중복은 걷어내고 각 컬렉션은 Hibernate가 올바르게 조립해준다.
     */
    @Query("SELECT DISTINCT g FROM GameEntity g LEFT JOIN FETCH g.participants LEFT JOIN FETCH g.ranks")
    List<GameEntity> findAllWithDetails();
}
