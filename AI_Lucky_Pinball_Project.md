# AI Lucky Pinball

## 1. 프로젝트 개요

AI를 활용한 웹 기반 생존형 핀볼 게임

참가자는 이름 또는 별칭과 생년월일을 입력하여 게임에 참여한다.

AI는 참가자의 정보를 분석하여 오늘의 운세를 생성하고, 운세가 좋지 않은 참가자에게는 "행운 버프(Lucky Buff)"를 제공한다.

게임은 물리 엔진 기반으로 진행되며, 마지막까지 살아남은 참가자가 최종 당첨자가 된다.

---

## 2. 프로젝트 목적

### 개발 목적

- Java Spring Boot 기반 REST API 개발
- JavaScript Canvas 활용
- Matter.js 물리 엔진 활용
- OpenAI API 연동
- AI 분석 결과를 게임 로직에 반영
- 캐싱 및 비용 최적화 경험 확보
- GitHub 기반 배포 경험 확보

### 포트폴리오 포인트

- 단순 OpenAI API 호출이 아닌 게임 로직과 AI 분석 연계
- 물리 시뮬레이션 구현
- AI 결과 기반 동적 게임 밸런스 조정
- 캐시 및 DB 활용을 통한 토큰 비용 절감

---

## 3. 게임 컨셉

### 슬로건

"오늘 운이 없는 사람에게 작은 행운을"

### 핵심 아이디어

운세가 좋은 사람은 그대로 게임 참여

운세가 좋지 않은 사람은 AI가 행운 버프를 제공

즉,

운이 나쁜 사람에게 추가 기회를 주는 컨셉

---

## 4. 게임 진행 방식

### 참가자 등록

최대 8명

입력 항목

- 이름 또는 별칭
- 생년월일

### AI 분석

입력 정보 기반

- 오늘의 운세 점수
- 행운의 숫자
- 행운 메시지

### 버프 적용

운세 점수 기준

- 80~100 : 버프 없음
- 60~79 : 버프 레벨 1
- 40~59 : 버프 레벨 2
- 20~39 : 버프 레벨 3
- 0~19 : 버프 레벨 4

---

## 5. 버프 시스템

### 시작 위치 보정

- 버프 없음 : Y = 0
- 버프 1 : Y = 50
- 버프 2 : Y = 100
- 버프 3 : Y = 150
- 버프 4 : Y = 200

### 체력 증가

- 기본 HP : 100
- 버프 1 : 110
- 버프 2 : 120
- 버프 3 : 140
- 버프 4 : 160

### 충돌 저항

- 기본 데미지 : 10
- 버프 4 적용 시 : 5

---

## 6. 게임 룰

게임 시작
→ 구슬 생성
→ 중력 적용
→ 구슬 충돌
→ 벽 충돌
→ HP 감소
→ HP 0 시 탈락
→ 마지막 남은 참가자 우승

---

## 7. 기술 스택

### Frontend

- HTML5
- CSS3
- JavaScript
- Canvas API
- Matter.js

### Backend

- Java 17
- Spring Boot

### Database

- MySQL 또는 H2

### Cache

- Redis

### AI

- OpenAI API

### 배포

- Frontend : GitHub Pages
- Backend : Render / Railway

---

## 8. 시스템 아키텍처

사용자
→ Frontend
→ Spring Boot API
→ Redis 조회
→ DB 조회
→ 없으면 OpenAI 호출
→ 결과 저장
→ 응답

---

## 9. AI 비용 절감 전략

### 캐시 구조

Redis
→ DB
→ OpenAI

### 저장 데이터

- 이름
- 생년월일
- 운세 점수
- 행운 메시지
- 생성일

### 정책

- 같은 날짜 재조회 시 Redis 또는 DB 사용
- 다음 날 재분석

---

## 10. API 설계

### 참가자 등록

POST /api/player

### 운세 분석

POST /api/fortune

### 게임 생성

POST /api/game/create

### 게임 시작

POST /api/game/start

### 결과 조회

GET /api/game/result/{gameId}

---

## 11. DB 설계

### PLAYER

- player_id
- name
- birth_date
- created_at

### FORTUNE_RESULT

- fortune_id
- player_id
- fortune_score
- fortune_message
- lucky_number
- created_date

### GAME_RESULT

- game_id
- winner_name
- participant_count
- created_at

---

## 12. 향후 확장

### AI 캐릭터 생성

예시:

"버그를 사냥하는 전설의 개발자"

### AI 응원 메시지

게임 시작 전 AI 응원 멘트 생성

### AI 이벤트

랜덤 행운 이벤트 발생

예시:

"행운의 별이 나타났습니다"

HP +20

---

## 13. 기대 효과

- OpenAI API 활용 경험
- Spring Boot REST API 개발 경험
- Redis 캐시 활용 경험
- Canvas 기반 게임 개발 경험
- Matter.js 물리 엔진 활용 경험
- AI 결과를 실제 게임 로직에 적용한 프로젝트 경험
- 금융권 및 SI 프로젝트 면접 시 설명 가능한 구조 확보
