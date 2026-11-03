# 기업 커뮤니티 구현 작업

> 전체 진행 상황: [기업 커뮤니티 구현 로드맵](./community-tasks.md)

## 1. 작업 개요

- 유형: `FEATURE`
- 요구사항 문서: `community-requirements.md`
- 기술 설계 문서: `design.md`
- 목표: 기업별 커뮤니티 기능을 작고 검증 가능한 단위로 한 개씩 구현한다.
- 현재 작업 범위: 기업 채팅방 식별자와 공고 연결 기반
- 제외 범위: 참여 관계, 커뮤니티 API, 메시지 병합, WebSocket, Redis Pub/Sub, 프론트엔드 화면

## 2. Task 목록

| ID | Task | 선행 작업 | 상태 |
| --- | --- | --- | --- |
| T-01 | 기업 채팅방 식별자와 공고 연결 기반 추가 | 없음 | DONE |

상태는 `TODO`, `IN_PROGRESS`, `BLOCKED`, `DONE`을 사용한다. 다음 Task는 현재 Task가 끝난 뒤 한 개만 추가한다.

## 3. 상세 Task

### T-01. 기업 채팅방 식별자와 공고 연결 기반 추가

**상태:** DONE

**목표**

기존 공고에 안정적인 기업 채팅방 ID를 연결하고, 같은 기업명의 공고가 동일한 방을 재사용할 수 있는 영속 모델을 만든다.

**관련 요구사항 및 설계**

- 요구사항: 기업당 하나의 채팅방, 동일 기업의 모든 공고가 같은 채팅방 공유
- 설계: 3.1 기업 채팅방 식별, 5.1 `community_rooms`, 5.2 `jobs` 변경

**선행 작업**

- 없음

**작업 내용**

- [x] `community_rooms` 테이블과 기존 공고 backfill 마이그레이션 추가
- [x] `CommunityRoom` 엔티티와 Repository 추가
- [x] `Job`이 필수 기업 채팅방을 참조하도록 변경
- [x] 개발 초기 데이터와 기존 테스트 fixture 변경
- [x] 백엔드 아키텍처 문서 갱신

**예상 변경 범위**

- Backend: `CommunityRoom`, `Job`, `DataInitializer`, 관련 Repository와 테스트
- Frontend: 없음
- Database: `community_rooms`, `jobs.community_room_id`
- 기타: `BACKEND.md`

**완료 조건 (Acceptance Criteria)**

- [x] 기존 공고마다 `community_room_id`가 채워진다.
- [x] 기업명이 정확히 같은 공고는 같은 방을 참조한다.
- [x] 기업명이 다른 공고는 자동 병합되지 않는다.
- [x] 빈 DB 초기 데이터 생성과 기존 공고 마이그레이션을 모두 지원한다.
- [x] 기존 공고 조회와 공고별 채팅 테스트가 회귀 없이 통과한다.

**검증 방법**

- [x] `cd backend && ./gradlew test`
- [x] 실제 MySQL에서 Flyway V1 → V2 적용 및 Hibernate 매핑 검증
- [x] 마이그레이션 후 공고 수, 방 수와 누락된 방 참조 확인

**실행 결과**

- 변경 내용: 기업 방 엔티티·Repository, V2 마이그레이션, 공고의 필수 방 관계, 빈 DB 초기화와 관련 문서를 추가했다.
- 검증 결과: 백엔드 테스트 5개 통과. 기존 DB V1→V2와 Hibernate 검증 성공. 동일 `SameCo` 2건은 방 1개를 공유하고 `sameco`는 별도 방으로 유지됐다. 빈 DB에서 공고 9개, 방 9개, 메시지 18개가 생성됐고 누락·오연결은 0건이었다.
- 남은 문제: Flyway가 MySQL 8.4를 공식 검증한 최신 범위보다 새 버전이라는 경고가 있으나 마이그레이션은 성공했다.

---

## 4. 전체 완료 기준

- 현재 문서는 T-01만 추적한다.
- 후속 Task는 T-01 완료 후 요구사항과 설계에서 다음 최소 단위를 선택해 추가한다.
