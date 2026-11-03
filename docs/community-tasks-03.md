# 기업 커뮤니티 구현 작업 T-03

> 전체 진행 상황: [기업 커뮤니티 구현 로드맵](./community-tasks.md)

## 작업 개요

- 유형: `FEATURE`
- 선행 작업: T-02 공고 참여 API
- 상태: `DONE`
- 목표: 기존 메시지를 기업 방 기준으로 안전하게 이관하고 참여자 전용 조회 기능을 완성한다.
- 제외 범위: 메시지 전송, 나가기, WebSocket, Redis Pub/Sub, 프론트엔드

## 작업 내용

- [x] `chat_messages`에 `room_id`, `source_job_id`, `client_message_id` 구조 반영
- [x] 기존 메시지를 공고의 `community_room_id`로 유실 없이 이관
- [x] 메시지 Entity와 Repository를 기업 방 기준으로 변경
- [x] `GET /api/community-rooms/{roomId}/messages` 최신 50개 조회 추가
- [x] `GET /api/community-rooms` 참여 방 목록과 최근 활동 순 정렬 추가
- [x] 미참여 사용자의 목록 외 방 메시지 접근 차단
- [x] 기존 공고별 메시지 API 제거 및 백엔드 문서 갱신

## 예상 변경 범위

- Database: 새 Flyway 마이그레이션, `chat_messages` 관계와 인덱스
- Backend: Chat Entity·Repository·Service·Controller, Community Service·Controller·DTO
- Tests: 메시지 이관, 권한, 50개 제한, 목록 정렬
- Frontend: 변경하지 않음

현재 `JobChat`은 제거될 공고별 메시지 API를 호출하므로 T-03 완료 후 프론트엔드 채팅은 새 UI가 구현될 때까지 동작하지 않는다. 임시 호환 API는 참여자 전용 보안 규칙과 충돌하므로 만들지 않는다.

## 완료 조건

- [x] 마이그레이션 전후 메시지 수, ID, 본문, 작성자와 작성 시각이 같다.
- [x] 기존 `job_id`는 nullable `source_job_id`로 보존된다.
- [x] 같은 기업의 여러 공고 메시지는 같은 `room_id`를 가진다.
- [x] 서로 다른 기업의 메시지는 섞이지 않는다.
- [x] 참여자는 최신 메시지 최대 50개를 오래된 순서로 받는다.
- [x] 미참여자는 메시지를 조회할 수 없고 빈 방은 빈 배열을 반환한다.
- [x] 참여 방만 목록에 나오며 마지막 메시지 시각 기준으로 정렬된다.
- [x] 메시지가 없는 방은 목록 뒤에서 참여 시각 기준으로 정렬된다.
- [x] 기존 백엔드 테스트와 새 테스트가 모두 통과한다.

## 검증 방법

- [x] `cd backend && ./gradlew test`
- [x] 임시 MySQL에서 Flyway V1부터 신규 버전까지 적용
- [x] 이관 전후 메시지 비교와 같은 기업 병합 확인
- [x] 로그인 사용자·미참여 사용자로 두 조회 API 검증
- [x] 51개 이상 메시지의 제한·정렬 검증

## 실행 결과

- 변경 내용: 메시지의 기업 방 이관, 참여 방 목록과 최신 50개 메시지 조회, 참여자 권한 검사를 구현하고 기존 공고별 메시지 API를 제거했다.
- 검증 결과: 백엔드 테스트 9개 통과. 임시 MySQL에서 V1~V4 적용 후 기존 메시지 3건의 필드 보존과 같은 기업 방 병합을 확인했다. 실제 API에서 참여 방 정렬, 50개 제한·오래된 순 정렬, 빈 방, 403·404 응답을 확인했다.
- 남은 문제: MySQL 8.4에서 Flyway의 지원 범위 경고가 발생한다. 현재 프론트엔드 `JobChat`은 제거된 API를 사용하므로 T-07/T-08 전까지 동작하지 않는다.
