package com.luckypinball.fortune;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Random;
import org.springframework.stereotype.Service;

/**
 * 실제 OpenAI 연동 전, 규칙 기반으로 오늘의 운세를 만들어내는 목(mock) 구현체.
 * name + birthDate + 오늘 날짜를 시드로 사용해서, 같은 사람이 같은 날 다시 조회하면
 * 항상 같은 결과가 나오도록 결정적으로 설계했다 (실제 캐시 키와 동일한 성질).
 */
@Service
public class MockFortuneGenerator implements FortuneService {

    private static final String[] MESSAGES = {
            "오늘은 작은 행운이 따라옵니다",
            "예상치 못한 기회가 찾아옵니다",
            "오늘은 무난하게 흘러갑니다",
            "조금 조심스러운 하루가 될 수 있어요",
            "주변 사람에게 좋은 소식이 들려올 거예요",
            "평소보다 신중한 판단이 필요한 날입니다",
            "생각보다 일이 잘 풀리는 하루입니다",
            "작은 실수를 조심하면 무사히 넘어갈 하루예요",
            "뜻밖의 응원이 큰 힘이 되는 날입니다",
            "오늘은 여유를 갖고 하루를 보내보세요"
    };

    @Override
    public FortuneResult analyze(String name, LocalDate birthDate) {
        long seed = Objects.hash(name, birthDate, LocalDate.now());
        Random random = new Random(seed);

        int fortuneScore = random.nextInt(101);
        int luckyNumber = random.nextInt(99) + 1;
        String fortuneMessage = MESSAGES[random.nextInt(MESSAGES.length)];

        return new FortuneResult(fortuneScore, luckyNumber, fortuneMessage);
    }
}
