package com.luckypinball;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.luckypinball.admin.AdminController;
import com.luckypinball.fortune.FortuneResultEntity;
import com.luckypinball.fortune.FortuneResultJpaRepository;
import com.luckypinball.player.PlayerController;
import com.luckypinball.player.PlayerEntity;
import com.luckypinball.player.PlayerJpaRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 참가자 목록 API가 참가자 수와 상관없이 같은 개수의 쿼리로 끝나는지 확인한다(N+1 방지).
 * 예전에는 참가자마다 운세 이력을 따로 조회해서, 참가자가 10명 늘면 쿼리도 10개 늘었다.
 * `/api/players/reusable`은 슬립 방지 cron이 주기적으로 부르는 공개 주소라 특히 중요하다.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class PlayerListQueryCountTest {

    @Autowired
    PlayerController playerController;

    @Autowired
    AdminController adminController;

    @Autowired
    PlayerJpaRepository playerJpaRepository;

    @Autowired
    FortuneResultJpaRepository fortuneResultJpaRepository;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private long countStatements(Runnable action) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        action.run();
        return statistics.getPrepareStatementCount();
    }

    private void addPlayersWithHistory(String prefix, int count) {
        for (int i = 0; i < count; i++) {
            LocalDate birthDate = LocalDate.of(1990, 1, 1).plusDays(i);
            PlayerEntity player = playerJpaRepository.save(new PlayerEntity(prefix + i, birthDate));
            fortuneResultJpaRepository.save(new FortuneResultEntity(player.getId(), player.getName(), birthDate,
                    70, "테스트", 7, LocalDate.now(), FortuneResultEntity.SOURCE_AI));
        }
    }

    @Test
    void reusablePlayersUsesTheSameNumberOfQueriesRegardlessOfPlayerCount() {
        addPlayersWithHistory("재사용A", 2);
        long before = countStatements(playerController::reusablePlayers);

        addPlayersWithHistory("재사용B", 10);
        long after = countStatements(playerController::reusablePlayers);

        assertEquals(before, after, "참가자가 10명 늘어도 쿼리 수는 같아야 한다");
    }

    @Test
    void adminPlayersUsesTheSameNumberOfQueriesRegardlessOfPlayerCount() {
        addPlayersWithHistory("관리C", 2);
        long before = countStatements(adminController::players);

        addPlayersWithHistory("관리D", 10);
        long after = countStatements(adminController::players);

        assertEquals(before, after, "참가자가 10명 늘어도 쿼리 수는 같아야 한다");
    }
}
