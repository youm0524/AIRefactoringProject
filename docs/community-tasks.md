# 기업 커뮤니티 구현 로드맵

## 현재 위치

- 완료: T-01 기업 채팅방 기반, [T-02 공고 참여 API](./community-tasks-02.md), [T-03 메시지 이관과 조회 API](./community-tasks-03.md), [T-04 방 나가기와 재참여](./community-tasks-04.md), [T-07 REST 기반 커뮤니티 화면과 로그인 흐름](./community-tasks-07.md), [T-10 회사 엔티티 분리와 관계 정규화](./community-tasks-10.md)
- 진행 중: 없음
- 다음 실행: T-08 상세 Task 작성과 승인

## 권장 실행 순서

T-07 → T-10 → T-08 → T-05 → T-06 → T-09

REST API 화면 흐름을 확인한 뒤 회사 관계를 정규화하고, 그 기반에서 실시간 메시지와 다중 서버 중계를 연결한다.

## 전체 로드맵

| ID | 유형 | 결과 | 선행 작업 | 상태 | 상세 문서 |
| --- | --- | --- | --- | --- | --- |
| T-01 | FEATURE | 기업별 채팅방 ID를 만들고 공고와 연결 | 없음 | DONE | [보기](./community-tasks-01.md) |
| T-02 | FEATURE | 공고를 통해 기업 방에 중복 없이 참여 | T-01 | DONE | [보기](./community-tasks-02.md) |
| T-03 | FEATURE | 메시지를 기업 방으로 이관하고 참여자 조회 API 제공 | T-02 | DONE | [보기](./community-tasks-03.md) |
| T-04 | FEATURE | 방 나가기와 재참여를 안전하게 처리 | T-02, T-03 | DONE | [보기](./community-tasks-04.md) |
| T-05 | FEATURE | WebSocket 인증과 메시지 저장·전송 | T-03, T-04, T-10 | TODO | 실행 전 작성 |
| T-06 | FEATURE | Redis Pub/Sub으로 여러 서버에 메시지 중계 | T-05 | TODO | 실행 전 작성 |
| T-07 | FEATURE | REST 기반 커뮤니티 화면과 로그인·참여 흐름 구현 | T-02, T-03, T-04 | DONE | [보기](./community-tasks-07.md) |
| T-08 | FEATURE | 모바일·접근성·연결 상태 UI 마무리 | T-07, T-10 | TODO | 실행 전 작성 |
| T-09 | FEATURE | 재연결 복구와 전체 통합 검증 | T-06, T-08 | TODO | 실행 전 작성 |
| T-10 | REFACTOR | 회사 엔티티를 분리하고 공고·커뮤니티 관계 정규화 | T-04, T-07 | DONE | [보기](./community-tasks-10.md) |

## 진행 규칙

1. 전체 로드맵에는 Task의 결과, 의존성, 상태만 기록한다.
2. 상세 문서는 다음에 검토할 Task 한 개만 작성한다.
3. 상세 Task 작성 후 사용자 승인을 기다린다.
4. 승인된 Task만 `IN_PROGRESS`로 바꾸고 실행한다.
5. 완료 조건과 검증을 모두 통과한 Task만 `DONE`으로 바꾼다.
6. 한 Task가 끝나도 다음 Task를 자동으로 작성하거나 실행하지 않는다.

Task 유형은 사용자 기능 추가 `FEATURE`, 잘못된 동작 수정 `BUGFIX`, 외부 동작을 유지한 구조 개선 `REFACTOR`로 기록한다.

## 범위 기준

- 요구사항: [community-requirements.md](./community-requirements.md)
- 기술 설계: [design.md](./design.md)
- 상태값: `TODO`, `IN_PROGRESS`, `BLOCKED`, `DONE`
