/**
 * "공부하기 > Spring Boot" 챕터 목록.
 * 정리는 notes/chNN.md, 예제 코드는 code/chNN/, 핀볼 백엔드와 연결한 설명은 project/chNN.md에 있다.
 * 예제 코드는 임시 프로젝트에서 실제로 빌드·실행해 확인했고, output.txt는 그때의 실행 결과다.
 */

/** 핀볼 백엔드에 실제로 보내 보는 요청(직접 요청 탭). 데이터를 저장하지 않는 요청만 넣는다. */
export interface LiveRequestSpec {
  label: string;
  method: 'GET' | 'POST';
  /** 백엔드 주소 뒤에 붙는 경로(/api/...) */
  path: string;
  body?: unknown;
  /** JSON이 아닌 글자를 그대로 보낼 때(깨진 JSON 예시 등). body보다 우선한다 */
  rawBody?: string;
  /** 무엇을 확인하는 요청인지 */
  note: string;
}

export interface SpringChapter {
  id: string;
  part: string;
  title: string;
  summary: string;
  /** 학습 목표: 이 챕터에서 무엇을 이해하려고 하는지(1~2줄, 챕터 화면 맨 위에 표시) */
  goal: string;
  /** 학습 결과: 다 마치면 할 수 있는 것(1~2줄). 정리 끝의 확인 문제와 맞춘다 */
  outcome: string;
  keywords: string[];
  /** 코드 탭에 보여줄 파일 순서(code/chNN/ 안의 파일 이름). 비어 있으면 아직 준비 중인 챕터 */
  files: string[];
  live?: LiveRequestSpec[];
  /** 브라우저에서 동작을 보여 주는 탭이 있는 챕터(18장의 Stream 시각화) */
  visual?: 'stream';
}

export const PARTS = ['스프링 기본', '웹 요청 처리', '데이터', '연동·운영·테스트', '자바 문법'];

export const SPRING_CHAPTERS: SpringChapter[] = [
  {
    id: 'ch01',
    part: '스프링 기본',
    title: 'Spring과 Spring Boot',
    summary: '자동 설정, starter, 내장 톰캣. JSP·WAS 배포와 무엇이 다른지 보고 첫 API를 띄웁니다.',
    goal: 'Spring과 Spring Boot의 관계, 자동 설정과 내장 톰캣이 JSP·WAS 방식과 무엇이 다른지 이해한다.',
    outcome: '실행 가능한 jar 하나로 서버를 띄우는 구조를 설명하고, 첫 API를 만들어 응답을 확인할 수 있다.',
    keywords: ['자동 설정', 'starter', '실행 가능한 jar'],
    files: ['ReservationApplication.java', 'RoomController.java', 'Room.java', 'build.gradle', 'output.txt'],
  },
  {
    id: 'ch02',
    part: '스프링 기본',
    title: '프로젝트 구조와 실행',
    summary: 'Initializr로 만든 프로젝트의 폴더 구조, Gradle 명령, application.yml.',
    goal: 'Spring Boot 프로젝트의 폴더 구조, Gradle 명령, application.yml의 역할을 익힌다.',
    outcome: '처음 받은 프로젝트에서 소스·설정·빌드 결과물의 위치를 찾고, 빌드해서 띄운 뒤 시작 로그에서 포트와 프로필을 확인할 수 있다.',
    keywords: ['Gradle', 'application.yml', 'bootRun'],
    files: ['application.yml', 'RoomController.java', 'structure.txt', 'settings.gradle', 'build.gradle', 'ReservationApplication.java', 'Room.java', 'output.txt'],
  },
  {
    id: 'ch03',
    part: '스프링 기본',
    title: 'IoC/DI와 Bean',
    summary: '객체를 스프링이 만들고 연결해 주는 방식. @Component, 생성자 주입, @Bean, 싱글톤.',
    goal: '스프링이 객체를 만들고 연결해 주는 방식(IoC, DI)과 빈의 생명주기를 이해한다.',
    outcome: '생성자 주입으로 서비스와 저장소를 연결하고, "빈을 찾을 수 없다", "같은 타입 빈이 둘이다" 오류의 원인을 찾을 수 있다.',
    keywords: ['DI', '@Service', '생성자 주입', '@Bean'],
    files: ['RoomService.java', 'RoomRepository.java', 'AppConfig.java', 'RoomController.java', 'Room.java', 'ReservationApplication.java', 'build.gradle', 'output.txt'],
  },
  {
    id: 'ch04',
    part: '스프링 기본',
    title: '설정값과 프로필',
    summary: '@Value, @ConfigurationProperties, 개발·운영 설정 분리, 환경변수로 덮어쓰기.',
    goal: '설정값을 코드 밖으로 빼고, 환경(개발·운영)마다 다르게 적용하는 방법을 익힌다.',
    outcome: '설정 파일, 프로필, 환경변수, 실행 인자 중 어느 값이 적용되는지 판단하고, 비밀번호를 환경변수로 넣을 수 있다.',
    keywords: ['@Value', 'profile', '환경변수'],
    files: ['application.yml', 'application-dev.yml', 'application-prod.yml', 'ReservationProperties.java', 'PolicyController.java', 'ReservationApplication.java', 'build.gradle', 'output.txt'],
  },
  {
    id: 'ch05',
    part: '웹 요청 처리',
    title: '요청 처리 흐름',
    summary: '요청이 필터, DispatcherServlet, 인터셉터를 거쳐 컨트롤러까지 가는 길. "어디서 막혔나" 찾기.',
    goal: '요청이 필터, DispatcherServlet, 인터셉터를 거쳐 컨트롤러까지 가는 길을 이해한다.',
    outcome: '필터와 인터셉터 중 알맞은 쪽을 고르고, 405처럼 컨트롤러 로그가 찍히지 않는 요청이 어디서 막혔는지 찾을 수 있다.',
    keywords: ['DispatcherServlet', '필터', '인터셉터'],
    files: ['RequestLogFilter.java', 'TimingInterceptor.java', 'WebConfig.java', 'RoomController.java', 'Room.java', 'ReservationApplication.java', 'build.gradle', 'output.txt'],
  },
  {
    id: 'ch06',
    part: '웹 요청 처리',
    title: 'REST 컨트롤러',
    summary: 'URL과 HTTP 메서드를 메서드에 연결하고, 요청 값을 받고, 상태 코드를 정해 응답합니다.',
    goal: 'URL과 HTTP 메서드로 API를 만들고, 요청 값을 받아 알맞은 상태 코드로 응답하는 방법을 익힌다.',
    outcome: '주소 값, 쿼리 파라미터, JSON 본문을 받는 API를 만들고, 400이 나는 이유(타입 불일치, 필드 이름)를 설명할 수 있다.',
    keywords: ['@GetMapping', '@PathVariable', '@RequestBody', 'ResponseEntity'],
    files: ['ReservationController.java', 'CreateReservationRequest.java', 'Reservation.java', 'ReservationApplication.java', 'build.gradle', 'output.txt'],
    live: [
      {
        label: '조회: 재사용할 수 있는 참가자 목록',
        method: 'GET',
        path: '/api/players/reusable',
        note: '정상 조회는 200과 JSON 배열입니다. 데이터를 바꾸지 않는 GET 요청입니다.',
      },
      {
        label: '없는 자원: 없는 게임의 결과',
        method: 'GET',
        path: '/api/game/result/no-such-game',
        note: '주소의 {gameId} 자리에 없는 값을 넣으면 404입니다.',
      },
      {
        label: '깨진 JSON으로 참가자 등록',
        method: 'POST',
        path: '/api/player',
        rawBody: '{"name":',
        note: 'JSON이 중간에 끊겼습니다. 본문을 객체로 바꾸지 못해 컨트롤러가 실행되지 않고 400이 납니다(저장되지 않음).',
      },
    ],
  },
  {
    id: 'ch07',
    part: '웹 요청 처리',
    title: '요청 검증',
    summary: '@Valid와 Bean Validation으로 잘못된 입력을 컨트롤러 앞에서 막습니다.',
    goal: '잘못된 입력을 컨트롤러 앞에서 막는 Bean Validation을 익힌다.',
    outcome: '@NotBlank, @Size 같은 규칙을 골라 붙이고 @Valid로 적용하며, 애너테이션으로 할 수 없는 업무 검증은 서비스로 나눌 수 있다.',
    keywords: ['@Valid', '@NotBlank', '@Size'],
    files: ['CreateReservationRequest.java', 'ReservationController.java', 'build.gradle', 'ReservationApplication.java', 'output.txt'],
    live: [
      {
        label: '빈 이름',
        method: 'POST',
        path: '/api/player',
        body: { name: '' },
        note: '@NotBlank 위반. 검증에 실패하면 컨트롤러가 실행되지 않아 저장되지 않습니다.',
      },
      {
        label: '21자 이름',
        method: 'POST',
        path: '/api/player',
        body: { name: '가나다라마바사아자차카타파하가나다라마바사' },
        note: '@Size(max = 20) 위반.',
      },
      {
        label: '미래의 생년월일',
        method: 'POST',
        path: '/api/player',
        body: { name: '테스트', birthDate: '2999-01-01' },
        note: '@Past 위반. 응답 문구가 영어인 것은 message를 따로 적지 않아 서버 언어를 따랐기 때문입니다.',
      },
    ],
  },
  {
    id: 'ch08',
    part: '웹 요청 처리',
    title: '예외 처리',
    summary: '@RestControllerAdvice로 오류 응답 형식을 한곳에서 맞춥니다.',
    goal: '오류 응답을 한곳에서 같은 모양으로 맞추는 공통 예외 처리를 익힌다.',
    outcome: '@RestControllerAdvice로 400, 404, 500을 나눠 응답하고, 스택 트레이스가 남도록 오류 로그를 쓸 수 있다.',
    keywords: ['@ExceptionHandler', '공통 오류 응답'],
    files: ['GlobalExceptionHandler.java', 'ReservationException.java', 'ErrorResponse.java', 'ReservationController.java', 'CreateReservationRequest.java', 'Reservation.java', 'ReservationApplication.java', 'build.gradle', 'output.txt'],
    live: [
      {
        label: '업무 예외: 없는 게임',
        method: 'GET',
        path: '/api/game/result/no-such-game',
        note: '서비스가 던진 ApiException을 GlobalExceptionHandler가 404와 {error, traceId}로 바꿉니다.',
      },
      {
        label: '형식 오류: 있을 수 없는 날짜',
        method: 'POST',
        path: '/api/player',
        rawBody: '{"name":"테스트","birthDate":"1990-13-45"}',
        note: '13월 45일은 날짜로 바꿀 수 없습니다. HttpMessageNotReadableException을 따로 잡아 500이 아니라 400으로 응답합니다.',
      },
      {
        label: '없는 주소',
        method: 'GET',
        path: '/api/nothing',
        note: 'NoResourceFoundException을 따로 잡아 본문 없는 404로 조용히 응답합니다(봇 요청이 오류 로그를 채우지 않게).',
      },
    ],
  },
  {
    id: 'ch09',
    part: '웹 요청 처리',
    title: '세션·쿠키 로그인',
    summary: 'HttpSession과 쿠키로 하는 로그인, 로그인 확인 인터셉터. 토큰 방식과 비교합니다.',
    goal: '세션과 쿠키로 로그인 상태를 유지하는 방식과 그 한계를 이해한다.',
    outcome: '세션 로그인과 인터셉터 로그인 확인을 만들고, 서버가 여러 대일 때 로그인이 풀리는 이유를 설명할 수 있다.',
    keywords: ['HttpSession', '쿠키', '인터셉터'],
    files: ['LoginController.java', 'LoginCheckInterceptor.java', 'WebConfig.java', 'MyReservationController.java', 'LoginMember.java', 'application.yml', 'ReservationApplication.java', 'build.gradle', 'output.txt'],
  },
  {
    id: 'ch10',
    part: '데이터',
    title: 'MyBatis 기초',
    summary: 'Mapper 인터페이스와 XML, #{}와 ${}의 차이, 동적 SQL. 운영에서 가장 많이 보는 SQL 다루기.',
    goal: 'Mapper 인터페이스와 XML로 SQL을 다루는 MyBatis의 기본과, #{}와 ${}의 차이를 익힌다.',
    outcome: '메서드가 실행하는 SQL을 XML에서 찾고 동적 SQL(<where>, <if>, <foreach>)을 읽으며, ${}의 SQL 인젝션 위험을 설명할 수 있다.',
    keywords: ['Mapper XML', '#{} vs ${}', '<if>', '<foreach>'],
    files: ['ReservationMapper.xml', 'ReservationMapper.java', 'ReservationSearch.java', 'Reservation.java', 'ReservationController.java', 'application.yml', 'schema.sql', 'data.sql', 'build.gradle', 'ReservationApplication.java', 'output.txt'],
  },
  {
    id: 'ch11',
    part: '데이터',
    title: 'JPA 기초',
    summary: 'Entity와 Repository, 쿼리 메서드. MyBatis와 무엇이 다른지 비교합니다.',
    goal: 'Entity와 Repository로 DB를 다루는 JPA의 기본과, MyBatis와 무엇이 다른지 이해한다.',
    outcome: '쿼리 메서드 이름이 어떤 SQL이 되는지 읽고, save 없이 UPDATE가 나가는 이유(변경 감지)를 실행 SQL 로그로 확인할 수 있다.',
    keywords: ['@Entity', 'JpaRepository', '쿼리 메서드'],
    files: ['Reservation.java', 'ReservationRepository.java', 'ReservationService.java', 'ReservationController.java', 'application.yml', 'build.gradle', 'ReservationApplication.java', 'output.txt'],
  },
  {
    id: 'ch12',
    part: '데이터',
    title: '트랜잭션',
    summary: '@Transactional 기본과 롤백 규칙. checked 예외와 같은 클래스 안 호출에서 생기는 함정.',
    goal: '@Transactional이 언제 롤백되고 언제 커밋되는지, 같은 클래스 안에서 부르면 왜 동작하지 않는지 이해한다.',
    outcome: 'checked 예외에서 데이터가 남는 이유를 설명하고, rollbackFor나 클래스 분리로 고칠 수 있다.',
    keywords: ['@Transactional', '롤백 규칙', '프록시'],
    files: ['ReservationService.java', 'PaymentException.java', 'TransactionController.java', 'Reservation.java', 'ReservationRepository.java', 'application.yml', 'build.gradle', 'ReservationApplication.java', 'output.txt'],
  },
  {
    id: 'ch13',
    part: '데이터',
    title: '연관관계와 N+1',
    summary: '@ManyToOne, @OneToMany와 지연 로딩, 쿼리가 반복되는 N+1 문제와 해결.',
    goal: '엔티티 연관관계와 지연 로딩, 쿼리가 반복되는 N+1 문제를 이해한다.',
    outcome: 'SQL 로그에서 N+1을 알아보고 fetch join이나 EntityGraph로 고치며, LazyInitializationException의 원인을 설명할 수 있다.',
    keywords: ['@ManyToOne', '지연 로딩', 'fetch join'],
    files: ['Reservation.java', 'Room.java', 'ReservationRepository.java', 'ReservationService.java', 'ReservationController.java', 'RoomRepository.java', 'application.yml', 'build.gradle', 'ReservationApplication.java', 'output.txt'],
  },
  {
    id: 'ch14',
    part: '연동·운영·테스트',
    title: '외부 API 호출',
    summary: 'RestClient로 다른 서버를 호출하고, 타임아웃과 실패를 처리합니다(결제·PMS 연동).',
    goal: 'RestClient로 다른 서버를 호출하고, 상대 서버가 느리거나 죽었을 때를 대비하는 방법을 익힌다.',
    outcome: '연결·응답 타임아웃을 정하고, 4xx, 5xx, 연결 실패, 시간 초과를 나눠 처리할 수 있다.',
    keywords: ['RestClient', 'RestTemplate', '타임아웃'],
    files: ['PaymentClient.java', 'PaymentException.java', 'PaymentController.java', 'FakePaymentGatewayController.java', 'application.yml', 'build.gradle', 'ReservationApplication.java', 'output.txt'],
  },
  {
    id: 'ch15',
    part: '연동·운영·테스트',
    title: '로깅과 운영',
    summary: '로그 레벨과 파일 로그, 롤링, 프로필별 설정, Actuator 헬스 체크, jar 배포와 장애 때 로그 읽기.',
    goal: '로그 레벨과 파일 롤링, 요청 추적 ID, Actuator, jar 배포와 종료처럼 운영에 필요한 기본을 익힌다.',
    outcome: '추적 ID와 ERROR 로그로 장애 원인 줄을 찾고, 재시작 없이 로그 레벨을 바꾸며, 서버를 정상 종료할 수 있다.',
    keywords: ['logback', '롤링', 'Actuator', '배포'],
    files: ['application.yml', 'TraceIdFilter.java', 'ReservationController.java', 'build.gradle', 'ReservationApplication.java', 'output.txt'],
  },
  {
    id: 'ch16',
    part: '연동·운영·테스트',
    title: '서버 화면 맛보기',
    summary: 'Thymeleaf와 JSP로 서버에서 HTML을 만드는 방식. 폼 처리, 이스케이프, JSP를 jar로 배포할 때의 함정.',
    goal: 'Thymeleaf와 JSP로 서버에서 HTML을 만드는 방식과 폼 처리 흐름을 이해한다.',
    outcome: 'Thymeleaf로 목록과 입력 폼(검증, 저장 후 리다이렉트)을 만들고, JSP와의 차이(이스케이프, jar 배포)를 설명할 수 있다.',
    keywords: ['Thymeleaf', 'JSP', '폼 처리', 'XSS'],
    files: ['ReservationPageController.java', 'reservation-list.html', 'reservation-form.html', 'reservation-list.jsp', 'ReservationForm.java', 'ReservationStore.java', 'Reservation.java', 'build.gradle', 'ReservationApplication.java', 'output.txt'],
  },
  {
    id: 'ch17',
    part: '연동·운영·테스트',
    title: '테스트',
    summary: 'JUnit과 Mockito로 단위 테스트, @WebMvcTest와 MockMvc로 컨트롤러 테스트, @SpringBootTest로 통합 테스트를 만듭니다.',
    goal: '단위 테스트, 컨트롤러 테스트, 통합 테스트의 차이와 쓰임을 이해한다.',
    outcome: 'Mockito와 고정된 시계로 단위 테스트를, MockMvc로 컨트롤러 테스트를 만들고, 실패 메시지에서 무엇이 틀렸는지 읽을 수 있다.',
    keywords: ['JUnit', 'Mockito', 'MockMvc', '@SpringBootTest'],
    files: ['RoomServiceTest.java', 'RoomControllerTest.java', 'RoomPriceIntegrationTest.java', 'RoomService.java', 'RoomController.java', 'RoomRepository.java', 'AppConfig.java', 'Room.java', 'build.gradle', 'ReservationApplication.java', 'output.txt'],
  },
  {
    id: 'ch18',
    part: '자바 문법',
    title: '람다식과 Stream',
    summary: '함수형 인터페이스, 람다 문법, 메서드 참조, Stream의 filter·map·collect, Optional. Stream 처리 순서를 화면에서 한 단계씩 봅니다.',
    goal: '람다식, 메서드 참조, Stream, Optional을 읽고 쓰는 데 필요한 규칙을 익힌다.',
    outcome: 'filter·map·collect로 목록을 거르고 묶으며, Stream의 처리 순서와 toMap 키 중복 같은 자주 나는 예외를 설명할 수 있다.',
    keywords: ['람다', 'Stream', 'Optional'],
    files: ['LambdaBasics.java', 'StreamBasics.java', 'StreamOrder.java', 'OptionalBasics.java', 'output.txt'],
    visual: 'stream',
  },
];

export function findSpringChapter(id: string | undefined): SpringChapter | undefined {
  return SPRING_CHAPTERS.find((chapter) => chapter.id === id);
}

export function isReady(chapter: SpringChapter): boolean {
  return chapter.files.length > 0;
}
