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
  keywords: string[];
  /** 코드 탭에 보여줄 파일 순서(code/chNN/ 안의 파일 이름). 비어 있으면 아직 준비 중인 챕터 */
  files: string[];
  live?: LiveRequestSpec[];
}

export const PARTS = ['스프링 기본', '웹 요청 처리', '데이터', '연동·운영·테스트', '자바 문법'];

export const SPRING_CHAPTERS: SpringChapter[] = [
  {
    id: 'ch01',
    part: '스프링 기본',
    title: 'Spring과 Spring Boot',
    summary: '자동 설정, starter, 내장 톰캣. JSP·WAS 배포와 무엇이 다른지 보고 첫 API를 띄웁니다.',
    keywords: ['자동 설정', 'starter', '실행 가능한 jar'],
    files: ['ReservationApplication.java', 'RoomController.java', 'Room.java', 'build.gradle', 'output.txt'],
  },
  {
    id: 'ch02',
    part: '스프링 기본',
    title: '프로젝트 구조와 실행',
    summary: 'Initializr로 만든 프로젝트의 폴더 구조, Gradle 명령, application.yml.',
    keywords: ['Gradle', 'application.yml', 'bootRun'],
    files: ['application.yml', 'RoomController.java', 'structure.txt', 'settings.gradle', 'build.gradle', 'ReservationApplication.java', 'Room.java', 'output.txt'],
  },
  {
    id: 'ch03',
    part: '스프링 기본',
    title: 'IoC/DI와 Bean',
    summary: '객체를 스프링이 만들고 연결해 주는 방식. @Component, 생성자 주입, @Bean, 싱글톤.',
    keywords: ['DI', '@Service', '생성자 주입', '@Bean'],
    files: ['RoomService.java', 'RoomRepository.java', 'AppConfig.java', 'RoomController.java', 'Room.java', 'ReservationApplication.java', 'build.gradle', 'output.txt'],
  },
  {
    id: 'ch04',
    part: '스프링 기본',
    title: '설정값과 프로필',
    summary: '@Value, @ConfigurationProperties, 개발·운영 설정 분리, 환경변수로 덮어쓰기.',
    keywords: ['@Value', 'profile', '환경변수'],
    files: ['application.yml', 'application-dev.yml', 'application-prod.yml', 'ReservationProperties.java', 'PolicyController.java', 'ReservationApplication.java', 'build.gradle', 'output.txt'],
  },
  {
    id: 'ch05',
    part: '웹 요청 처리',
    title: '요청 처리 흐름',
    summary: '요청이 필터, DispatcherServlet, 인터셉터를 거쳐 컨트롤러까지 가는 길. "어디서 막혔나" 찾기.',
    keywords: ['DispatcherServlet', '필터', '인터셉터'],
    files: ['RequestLogFilter.java', 'TimingInterceptor.java', 'WebConfig.java', 'RoomController.java', 'Room.java', 'ReservationApplication.java', 'build.gradle', 'output.txt'],
  },
  {
    id: 'ch06',
    part: '웹 요청 처리',
    title: 'REST 컨트롤러',
    summary: 'URL과 HTTP 메서드를 메서드에 연결하고, 요청 값을 받고, 상태 코드를 정해 응답합니다.',
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
    keywords: ['HttpSession', '쿠키', '인터셉터'],
    files: ['LoginController.java', 'LoginCheckInterceptor.java', 'WebConfig.java', 'MyReservationController.java', 'LoginMember.java', 'application.yml', 'ReservationApplication.java', 'build.gradle', 'output.txt'],
  },
  {
    id: 'ch10',
    part: '데이터',
    title: 'MyBatis 기초',
    summary: 'Mapper 인터페이스와 XML, #{}와 ${}의 차이, 동적 SQL. 운영에서 가장 많이 보는 SQL 다루기.',
    keywords: ['Mapper XML', '#{} vs ${}', '<if>', '<foreach>'],
    files: [],
  },
  {
    id: 'ch11',
    part: '데이터',
    title: 'JPA 기초',
    summary: 'Entity와 Repository, 쿼리 메서드. MyBatis와 무엇이 다른지 비교합니다.',
    keywords: ['@Entity', 'JpaRepository', '쿼리 메서드'],
    files: [],
  },
  {
    id: 'ch12',
    part: '데이터',
    title: '트랜잭션',
    summary: '@Transactional 기본과 롤백 규칙. checked 예외와 같은 클래스 안 호출에서 생기는 함정.',
    keywords: ['@Transactional', '롤백 규칙', '프록시'],
    files: [],
  },
  {
    id: 'ch13',
    part: '데이터',
    title: '연관관계와 N+1',
    summary: '@ManyToOne, @OneToMany와 지연 로딩, 쿼리가 반복되는 N+1 문제와 해결.',
    keywords: ['@ManyToOne', '지연 로딩', 'fetch join'],
    files: [],
  },
  {
    id: 'ch14',
    part: '연동·운영·테스트',
    title: '외부 API 호출',
    summary: 'RestClient로 다른 서버를 호출하고, 타임아웃과 실패를 처리합니다(결제·PMS 연동).',
    keywords: ['RestClient', 'RestTemplate', '타임아웃'],
    files: [],
  },
  {
    id: 'ch15',
    part: '연동·운영·테스트',
    title: '로깅과 운영',
    summary: '로그 레벨과 파일 로그, 롤링, 프로필별 설정, Actuator 헬스 체크, jar 배포와 장애 때 로그 읽기.',
    keywords: ['logback', '롤링', 'Actuator', '배포'],
    files: [],
  },
  {
    id: 'ch16',
    part: '연동·운영·테스트',
    title: '서버 화면 맛보기',
    summary: 'JSP와 Thymeleaf로 서버에서 HTML을 만드는 방식. JSP 경험과 비교합니다.',
    keywords: ['Thymeleaf', 'JSP', 'Model'],
    files: [],
  },
  {
    id: 'ch17',
    part: '연동·운영·테스트',
    title: '테스트',
    summary: 'JUnit 5, @SpringBootTest, @WebMvcTest와 MockMvc로 API를 테스트합니다.',
    keywords: ['JUnit 5', 'MockMvc', '@SpringBootTest'],
    files: [],
  },
  {
    id: 'ch18',
    part: '자바 문법',
    title: '람다식과 Stream',
    summary: '함수형 인터페이스, 람다 문법, 메서드 참조, Stream의 filter·map·collect, Optional.',
    keywords: ['람다', 'Stream', 'Optional'],
    files: [],
  },
];

export function findSpringChapter(id: string | undefined): SpringChapter | undefined {
  return SPRING_CHAPTERS.find((chapter) => chapter.id === id);
}

export function isReady(chapter: SpringChapter): boolean {
  return chapter.files.length > 0;
}
