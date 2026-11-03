# 회사 엔티티 분리 및 관계 정규화 T-10

> 전체 진행 상황: [기업 커뮤니티 구현 로드맵](./community-tasks.md)

## 작업 개요

- 관련 이슈: #12
- 유형: `REFACTOR`
- 선행 작업: T-04 방 나가기와 재참여, T-07 REST 기반 커뮤니티 화면
- 상태: `DONE`
- 목표: 회사명 문자열과 공고의 중복 회사 정보에 의존하는 관계를 독립 `Company` FK 기반으로 변경한다.
- 제외 범위: 회사 등록·수정 API와 관리자 화면, 커뮤니티 API URL 변경, WebSocket, Redis Pub/Sub, 프론트엔드 화면 변경

## 사용자가 확인할 결과

1. 공고 조회와 커뮤니티 참여·목록 응답에 방 ID와 별개인 안정적인 `companyId`가 포함되며 기존 필드는 유지된다.
2. 같은 회사의 여러 공고는 하나의 `companyId`를 참조하고 회사당 기존 커뮤니티 방 하나를 유지한다.
3. 기존 방 ID, 참여 관계와 메시지는 회사 엔티티 이관 전후에 바뀌거나 유실되지 않는다.
4. 회사명이 바뀌어도 공고와 커뮤니티 관계는 문자열 비교 없이 FK로 유지된다.
5. 같은 회사의 기존 공고에 서로 다른 로고나 색상이 있으면 임의로 선택하지 않고 마이그레이션 검증이 실패한다.

## 작업 내용

- [x] `companies` 테이블을 추가하고 기존 방과 공고의 회사 정보를 이관하는 Flyway V5 마이그레이션 작성
- [x] `jobs.company_id`와 `community_rooms.company_id` FK 및 회사당 방 하나를 보장하는 UNIQUE 제약 추가
- [x] 이관 완료 후 `jobs.company`, `jobs.logo`, `jobs.color`, `jobs.community_room_id`, `community_rooms.company_name` 중복 컬럼 제거
- [x] `Company` 엔티티와 Repository 추가, `Job`과 `CommunityRoom` 관계 변경
- [x] 공고 검색·응답과 커뮤니티 참여·목록 로직이 연결된 회사 정보를 사용하도록 변경
- [x] 공고 조회와 커뮤니티 참여·목록 응답에 `companyId` 추가
- [x] 개발 초기 데이터가 회사를 먼저 생성한 뒤 방과 공고를 연결하도록 변경
- [x] 서비스 단위 테스트와 실제 MySQL 마이그레이션·API 회귀 검증 추가
- [x] `BACKEND.md`, README와 관련 구현 문서를 최종 스키마에 맞게 갱신

## 예상 변경 범위

- Backend: `Company`, `Job`, `CommunityRoom`, 관련 Repository·Service·초기 데이터와 테스트
- Frontend: 없음. 기존 필드를 유지하고 API 응답에 `companyId`만 추가
- Database: `companies`, `jobs.company_id`, `community_rooms.company_id`, 기존 중복 회사 컬럼 제거
- 기타: Flyway 검증 스크립트, `BACKEND.md`, README

## 완료 조건

- [x] 모든 공고가 non-null `company_id`를 가지며 하나의 회사만 참조한다.
- [x] 모든 커뮤니티 방이 non-null `company_id`를 가지고 회사당 방이 최대 한 개다.
- [x] 회사와 방의 ID는 독립적으로 유지되고 숫자 일치를 전제로 사용하지 않는다.
- [x] 마이그레이션 전후 공고·방·참여 관계·메시지 수와 기존 ID가 보존된다.
- [x] 회사명이 같은 기존 공고의 로고·색상 충돌과 공고 없는 방은 이관 전에 탐지된다.
- [x] 공고 조회와 참여·목록 응답의 `companyId`가 같은 회사를 가리키고, 기존 API 필드와 상태 코드가 유지된다.
- [x] 기존 관련 테스트와 새 테스트가 모두 통과한다.

## 검증 방법

- [x] `cd backend && ./gradlew test`
- [x] `cd backend && ./scripts/with-verification-db.sh flyway-smoke`
- [x] `cd backend && ./scripts/with-verification-db.sh company-migration`
- [x] `cd backend && ./scripts/with-verification-db.sh company-conflict`
- [x] `cd backend && ./scripts/with-verification-db.sh company-orphan`
- [x] 임시 MySQL에서 V1~V5 적용 후 FK, UNIQUE, 제거 컬럼과 데이터 수·ID 보존 확인
- [x] 동일 회사 공고 2건이 같은 회사와 방을 참조하고 다른 회사 공고는 분리되는지 확인
- [x] 회사 정보 충돌 데이터에서 마이그레이션이 실패하는지 확인
- [x] 로그인 후 참여 → 방 목록 → 메시지 조회 → 나가기 → 재참여 API 회귀 확인
- [x] `cd frontend && npm test && npm run build`

## 실행 결과

- 변경 내용: 독립 회사 엔티티와 V5 이관을 추가하고 공고·방 관계를 회사 FK로 정규화했다. 공고·참여·목록 API는 연결된 회사의 `companyId`와 표시 정보를 응답한다.
- 검증 결과: 백엔드 테스트 12개와 프론트엔드 테스트 3개·빌드가 통과했다. 임시 MySQL에서 V1~V5 기동, 기존 데이터 수·ID 보존, 회사 정보 충돌·공고 없는 방의 이관 실패, 참여·조회·나가기·재참여 API를 확인했다. 공고·참여·목록 응답의 `companyId`가 일치함도 통합 검증했다.
- 남은 문제: MySQL 8.4에 대한 Flyway 지원 범위 경고는 기존과 동일하다.
