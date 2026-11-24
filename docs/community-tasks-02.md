# 기업 커뮤니티 구현 작업 T-02

> 전체 진행 상황: [기업 커뮤니티 구현 로드맵](./community-tasks.md)

## 1. 작업 개요

- 유형: `FEATURE`
- 요구사항 문서: `community-requirements.md`
- 기술 설계 문서: `design.md`
- 선행 작업: T-01 기업 채팅방 식별자와 공고 연결
- 목표: 로그인 사용자가 공고를 통해 기업 채팅방에 중복 없이 참여한다.
- 작업 범위: 참여 관계 영속 모델, 기존 작성자 참여 이관, 참여 서비스와 단일 PUT API
- 제외 범위: 참여 방 목록, 메시지 조회·전송, 나가기, WebSocket, Redis Pub/Sub, 프론트엔드

## 2. Task

| ID | Task | 선행 작업 | 상태 |
| --- | --- | --- | --- |
| T-02 | 공고를 통한 기업 채팅방 참여 API | T-01 | DONE |

## 3. 상세 Task

### T-02. 공고를 통한 기업 채팅방 참여 API

**상태:** DONE

**목표**

`PUT /api/jobs/{jobId}/community-membership` 요청으로 인증 사용자를 공고의 기업 채팅방에 참여시키고, 반복 요청에도 참여 관계를 한 건만 유지한다.

**관련 요구사항 및 설계**

- 요구사항: 공고를 통한 참여 및 입장, 중복 참여 방지, 기존 작성자 참여 관계 이관
- 설계: 5.3 `community_room_members`, 6.1 공고를 통한 참여, 9.1 공고에서 참여

**작업 내용**

- [x] 참여 관계 테이블과 기존 작성자 backfill 마이그레이션 추가
- [x] 참여 관계 엔티티와 Repository 추가
- [x] 인증·공고 확인 후 멱등 참여하는 서비스 추가
- [x] 참여 PUT API와 응답 DTO 추가
- [x] 서비스 단위 테스트와 실제 API 통합 검증
- [x] 백엔드 문서 갱신

**예상 변경 범위**

- Backend: Community Controller·Service·DTO, 참여 Entity·Repository, 단위 테스트
- Frontend: 없음
- Database: `community_room_members`
- 기타: `BACKEND.md`, OpenAPI 설명

**완료 조건 (Acceptance Criteria)**

- [x] 인증된 사용자는 공고가 연결된 기업 방에 참여하고 방 ID와 기업명을 받는다.
- [x] 같은 요청을 반복해도 `(user_id, room_id)` 참여 관계가 한 건이다.
- [x] 같은 기업의 다른 공고로 요청해도 같은 참여 관계와 방을 반환한다.
- [x] 인증되지 않은 요청은 401, 없는 공고는 404를 반환한다.
- [x] 기존 `user_id`가 있는 메시지 작성자는 해당 기업 방 참여자로 이관된다.
- [x] 기존 백엔드 테스트가 회귀 없이 통과한다.

**검증 방법**

- [x] `cd backend && ./gradlew test`
- [x] 임시 MySQL에서 Flyway V1→V3 및 기존 작성자 backfill 검증
- [x] 임시 애플리케이션에서 로그인 후 PUT API 반복 호출 및 DB 중복 확인

**실행 결과**

- 변경 내용: 참여 관계 테이블·엔티티·Repository, 기존 작성자 이관, 멱등 참여 서비스와 PUT API, 응답 DTO와 문서를 추가했다.
- 검증 결과: 백엔드 테스트 7개 통과. V1→V3에서 동일 기업의 기존 작성자는 최초 메시지 시각으로 참여 관계 1건이 생성됐다. 최초·반복·동일 기업의 다른 공고 참여가 모두 같은 방을 반환했고 DB 참여 관계는 1건이었다. 인증 없음은 401, 없는 공고는 404였다.
- 남은 문제: Flyway가 MySQL 8.4를 공식 검증한 최신 범위보다 새 버전이라는 경고가 있으나 마이그레이션과 Hibernate 검증은 성공했다.
