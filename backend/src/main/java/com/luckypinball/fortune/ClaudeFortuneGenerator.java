package com.luckypinball.fortune;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.luckypinball.common.ApiException;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    // 사용자 응답에는 실패 유형별 일반 문구만 싣는다. 원인(응답 코드, 예외 체인)은 cause로 넘겨 로그에만 남는다.
    static final String UPSTREAM_FAILURE_MESSAGE = "AI 운세 서비스에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.";
    static final String TIMEOUT_MESSAGE = "AI 응답이 늦어지고 있습니다. 잠시 후 다시 시도해주세요.";
    static final String BUSY_MESSAGE = "AI 서비스에 요청이 몰려 있습니다. 잠시 후 다시 시도해주세요.";

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

    private final Duration timeout;
    private final int maxRetries;

    /**
     * SDK 기본값은 요청 타임아웃 10분, 재시도 2회라서 Claude가 멈추면 서블릿 스레드 하나가 최대 수십 분을
     * 붙잡힌다(타임아웃·연결 오류도 재시도 대상). 평소 응답이 4초 안팎이라 10초 + 재시도 1회로 줄여서
     * 최악의 대기를 약 20초로 묶는다.
     */
    public ClaudeFortuneGenerator(@Value("${anthropic.timeout-seconds:10}") long timeoutSeconds,
                                   @Value("${anthropic.max-retries:1}") int maxRetries) {
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.maxRetries = maxRetries;
    }

    private AnthropicClient client;

    private synchronized AnthropicClient client() {
        if (client == null) {
            try {
                client = AnthropicOkHttpClient.builder().fromEnv().timeout(timeout).maxRetries(maxRetries).build();
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

    /**
     * 관리자 화면의 "AI 연결 확인"용 최소 호출(출력 토큰 1개). 키·네트워크·크레딧·응답 속도를 실제 요청으로 확인한다.
     * 재시도는 끈다 — 점검은 "지금 한 번 시도해서 되는가"를 보는 것이고, 재시도가 일시적 실패를 가려버리면 안 된다.
     * 클라이언트를 만들지 못하면(키 없음) ApiException, 호출이 실패하면 SDK의 AnthropicException이 그대로 올라온다.
     */
    public void ping() {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(1L)
                .addUserMessage("ping")
                .build();
        client().withOptions(options -> options.maxRetries(0)).messages().create(params);
    }

    public String model() {
        return MODEL;
    }

    public long timeoutSeconds() {
        return timeout.toSeconds();
    }

    /** Claude 호출 실패를 사용자에게 의미 있는 상태 코드로 나눈다: 타임아웃 504, 요청 과다(429) 503, 그 외 502. */
    static ApiException translate(AnthropicException e) {
        if (e instanceof AnthropicIoException && isTimeout(e)) {
            return ApiException.upstreamTimeout(TIMEOUT_MESSAGE, e);
        }
        if (e instanceof RateLimitException) {
            return ApiException.upstreamUnavailable(BUSY_MESSAGE, e);
        }
        return ApiException.upstreamFailure(UPSTREAM_FAILURE_MESSAGE, e);
    }

    /** 읽기/연결/전체 호출 타임아웃은 모두 InterruptedIOException(SocketTimeoutException 포함)으로 올라온다. */
    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof InterruptedIOException) {
                return true;
            }
        }
        return false;
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
        } catch (AnthropicException e) {
            // 응답 코드뿐 아니라 타임아웃·연결 오류·응답 파싱 실패까지 전부 여기서 잡는다(이전엔 서비스 오류만
            // 잡아서 나머지는 500으로 떨어졌다). Claude의 원문 오류는 사용자 응답에 싣지 않고 원인으로만 넘긴다.
            throw translate(e);
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
