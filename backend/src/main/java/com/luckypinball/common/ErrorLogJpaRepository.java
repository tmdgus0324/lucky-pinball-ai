package com.luckypinball.common;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ErrorLogJpaRepository extends JpaRepository<ErrorLogEntity, Long> {

    /** 최신순. 개수는 Pageable로 제한한다. */
    List<ErrorLogEntity> findAllByOrderByIdDesc(Pageable pageable);

    /** 이 id 이하(=오래된 것)를 지운다. id는 IDENTITY라 시간순으로 증가하므로 개수를 세지 않고 한 번에 정리할 수 있다. */
    // @Query로 직접 만든 수정 쿼리는 save() 같은 기본 메서드와 달리 트랜잭션이 자동으로 붙지 않는다 — 호출하는 쪽이
    // 트랜잭션 안에 있다고 가정하지 않도록 여기서 직접 건다.
    @Transactional
    @Modifying
    @Query("delete from ErrorLogEntity e where e.id <= :maxIdToDelete")
    int deleteOlderThanOrEqual(long maxIdToDelete);
}
