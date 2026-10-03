package com.luckypinball.fortune;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.luckypinball.common.ApiException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Claude API(Haiku 4.5)로 실제 오늘의 운세를 생성하는 구현체.
 * {@link FortuneService} 인터페이스만 지키면 되므로, 이 클래스가 호출부(FortuneQueryService)
 * 코드를 전혀 바꾸지 않고 {@link MockFortuneGenerator}를 대체할 수 있었다 — 애초에 이 교체를
 * 위해 인터페이스를 분리해둔 것 (plan/01_mvp-plan.md §3.1 참고).
 *
 * ANTHROPIC_API_KEY 환경변수가 필요하다. 클라이언트는 최초 호출 시점에 지연 생성해서,
 * 키가 없어도 서버 자체는 정상적으로 뜨고 실제 운세를 조회하는 순간에만 명확한 에러를 낸다.
 */
@Service
public class ClaudeFortuneGenerator implements FortuneService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeFortuneGenerator.class);

    private static final String MODEL = "claude-haiku-4-5";

    private static final String UPSTREAM_FAILURE_MESSAGE = "AI 운세 서비스에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.";

    private static final String[] ZODIAC = {
            "원숭이띠", "닭띠", "개띠", "돼지띠", "쥐띠", "소띠", "호랑이띠", "토끼띠", "용띠", "뱀띠", "말띠", "양띠"
    };

    @JsonClassDescription("한 사람의 이름과 생년월일을 바탕으로 재미로 보는 오늘의 운세")
    public record AiFortune(
            @JsonPropertyDescription("행운 점수. 0~100 사이 정수이며 높을수록 좋은 운세. 여러 사람을 연달아 생성하더라도 매번 비슷한 점수대(예: 70점대)로 몰리지 않도록 0~100 전 구간을 폭넓게 활용할 것")
            int score,
            @JsonPropertyDescription("행운의 숫자. 1~99 사이 정수") int luckyNumber,
            @JsonPropertyDescription(
                    "오늘의 운세를 설명하는 한국어 한 문장(20~40자, 존댓말). "
                            + "\"새로운 기회가 찾아오는 날입니다\" 같은 뻔하고 일반적인 문구는 피하고, "
                            + "함께 제공되는 띠/태어난 계절/오늘 요일 중 한두 가지를 자연스럽게 녹여서 "
                            + "그 사람만을 위한 것처럼 구체적이고 개성 있게 작성할 것"
            )
            String message
    ) {
    }

    private AnthropicClient client;

    private synchronized AnthropicClient client() {
        if (client == null) {
            try {
                client = AnthropicOkHttpClient.fromEnv();
            } catch (RuntimeException e) {
                // 환경변수 이름 같은 서버 설정을 사용자 응답에 싣지 않는다. 원인(키 없음)은 cause로 넘겨서
                // GlobalExceptionHandler의 ERROR 로그("Caused by")에서 확인한다.
                throw ApiException.upstreamFailure(UPSTREAM_FAILURE_MESSAGE, e);
            }
        }
        return client;
    }

    /**
     * 서버가 완전히 뜬 직후에 클라이언트(OkHttp 커넥션 풀 등)를 미리 만들어둔다 — Anthropic에
     * 실제 요청을 보내는 게 아니라 로컬에서 통신 준비만 해두는 것이라 토큰 비용은 전혀 없다.
     * 이렇게 안 하면 이 비용이 배포 후 첫 실제 사용자의 첫 운세 요청에 고스란히 붙는다.
     * 키가 없으면 조용히 넘어가고(로그만 남김), 실제 요청 시점에 client()가 같은 안내 메시지로
     * 다시 실패한다 — 키 없이도 서버가 정상적으로 뜬다는 기존 동작은 그대로 유지한다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpClient() {
        try {
            client();
            log.info("Claude 클라이언트 준비 완료");
        } catch (ApiException e) {
            // ANTHROPIC_API_KEY 미설정 — 로컬 개발 등에서는 정상적인 상황이지만, 배포 환경에서 이 줄이
            // 보이면 환경변수를 빠뜨린 것이라 로그로는 남긴다.
            log.warn("ANTHROPIC_API_KEY 미설정 — 서버는 정상 기동하지만 실제 운세 조회 시 실패합니다");
        }
    }

    @Override
    public FortuneResult analyze(String name, LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        int digitSeed = digitSumOf(birthDate);
        String prompt = """
                이름: %s
                생년월일: %s (%s, %s, %s 출생) — 생년월일 숫자를 모두 더한 값: %d
                오늘 날짜: %s (%s)

                위 정보를 참고해서 이 사람의 오늘 운세를 재미로 봐줘. 아래 조건을 지켜줘.
                - 점수와 행운의 숫자는 위에 계산해둔 "생년월일 숫자 합"(%d)을 재료로 삼아서 계산하듯 정해줘.
                  이 값이 다르면 결과도 뚜렷하게 달라져야 하고, 여러 사람을 연달아 봐주더라도 특정 점수대나
                  특정 숫자(예: 항상 70점대, 항상 47)를 습관처럼 반복하면 안 돼 — 0~100(점수), 1~99(행운의 숫자)
                  전 구간을 실제로 다양하게 써줘.
                - 운세 메시지는 뻔한 문구를 피하고, 이 사람의 띠·태어난 계절·오늘 요일 중 한두 가지를 자연스럽게 녹여서 그 사람만을 위한 것처럼 구체적이고 개성 있게 써줘.
                """.formatted(
                name, birthDate, koreanDayOfWeek(birthDate), zodiacOf(birthDate), seasonOf(birthDate), digitSeed,
                today, koreanDayOfWeek(today), digitSeed);

        StructuredMessageCreateParams<AiFortune> params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(1024L)
                .outputConfig(AiFortune.class)
                .addUserMessage(prompt)
                .build();

        AiFortune result;
        long startedAt = System.currentTimeMillis();
        try {
            StructuredMessage<AiFortune> response = client().messages().create(params);
            result = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .findFirst()
                    .map(StructuredTextBlock::text)
                    .orElseThrow(() -> ApiException.upstreamFailure("Claude가 운세 결과를 반환하지 않았습니다."));
            // 이름·생년월일이 담긴 프롬프트와 응답 본문은 남기지 않는다 — 호출 사실과 걸린 시간만.
            log.info("Claude 호출 완료: model={} {}ms", MODEL, System.currentTimeMillis() - startedAt);
        } catch (AnthropicServiceException e) {
            // Claude의 원문 오류(예: "API key is invalid")는 사용자 응답에 싣지 않고, 원인으로만 넘긴다 —
            // GlobalExceptionHandler가 남기는 ERROR 로그에 "Caused by"로 실제 응답 코드가 보인다.
            throw ApiException.upstreamFailure(UPSTREAM_FAILURE_MESSAGE, e);
        }

        // AI 응답값이 프롬프트에 적힌 범위를 벗어나더라도(모델이 완벽히 지시를 따르지 않을 수 있음)
        // BuffCalculator가 안전하게 동작하도록 방어적으로 범위를 강제한다.
        int score = clamp(result.score(), 0, 100);
        int luckyNumber = clamp(result.luckyNumber(), 1, 99);
        return new FortuneResult(score, luckyNumber, result.message());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 이름+생년월일만으로는 사람마다 다른 내용을 써줄 "재료"가 부족해서 Claude가 매번 비슷한
     * 문구로 수렴하는 경향이 있었다 (실제 테스트로 확인됨). 그래서 띠/계절/요일처럼 사람마다
     * 달라지는 구체적인 정보를 함께 던져줘서, 이걸 실마리 삼아 더 다양한 메시지를 쓰도록 유도한다.
     */
    private static String koreanDayOfWeek(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
    }

    private static String zodiacOf(LocalDate birthDate) {
        int index = ((birthDate.getYear() % 12) + 12) % 12;
        return ZODIAC[index];
    }

    private static String seasonOf(LocalDate birthDate) {
        return switch (birthDate.getMonthValue()) {
            case 3, 4, 5 -> "봄";
            case 6, 7, 8 -> "여름";
            case 9, 10, 11 -> "가을";
            default -> "겨울";
        };
    }

    /**
     * "무작위로 다양하게 정해줘" 같은 지시만으로는 Claude가 실제로는 특정 숫자(예: 매번 47)로
     * 수렴하는 경향이 실제 테스트로 확인됐다 — 서로 다른 API 호출은 매번 독립적이라 "다른
     * 사람과 겹치지 않게"를 스스로 판단할 근거가 없기 때문으로 보인다. 그래서 생년월일 숫자를
     * 실제로 계산한 값(사람마다 기계적으로 달라짐)을 프롬프트에 재료로 던져줘서, "이 숫자를
     * 바탕으로 계산하라"는 구체적인 절차를 줬다 — 행운의 숫자 다양성은 눈에 띄게 좋아졌지만,
     * 점수는 여전히 60~70점대로 쏠리는 경향이 남아있다(devhelp 참고, 순수 프롬프트 개선의 한계).
     */
    private static int digitSumOf(LocalDate birthDate) {
        return birthDate.toString().replace("-", "").chars()
                .filter(Character::isDigit)
                .map(Character::getNumericValue)
                .sum();
    }
}
