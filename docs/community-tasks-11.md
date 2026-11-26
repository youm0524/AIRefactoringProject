# WebSocket Handler 책임 분리 T-11

> 전체 진행 상황: [기업 커뮤니티 구현 로드맵](./community-tasks.md)

## 작업 개요

- 관련 이슈: #15
- 유형: `REFACTOR`
- 선행 작업: T-05 WebSocket 실시간 메시지 송수신
- 상태: `DONE`
- 목표: WebSocket의 프로토콜 처리, 연결 관리, 실시간 전달 책임을 분리해 기존 동작을 유지하면서 각 클래스의 변경 이유를 하나로 줄인다.
- 제외 범위: STOMP 전환, Redis Pub/Sub, WebSocket JSON 계약 변경, 인증 정책 변경, DB·프론트엔드 변경, 구현이 하나뿐인 클래스의 형식적인 인터페이스 추가

## 사용자가 확인할 결과

1. 기존과 동일하게 인증 후 참여 중인 방에서 메시지를 보내고 현재 참여자에게 실시간으로 전달된다.
2. `CommunityWebSocketHandler`는 프레임 해석과 인증·전송 흐름 조립만 담당해 주요 처리 순서를 위에서 아래로 읽을 수 있다.
3. 연결 종료, 세션 만료, 미참여, 중복 전송과 저장 실패의 기존 결과가 바뀌지 않는다.

## 작업 내용

- [x] 현재 Handler의 연결 상태·타이머·전송·전달 책임과 호출 관계를 테스트 기준으로 고정
- [x] 현재 서버의 연결 등록·인증 정보·타이머·안전한 전송·종료를 `CommunityWebSocketConnections`로 분리
- [x] 현재 접속 사용자 중 방 참여자를 선택해 `MESSAGE_CREATED`를 보내는 책임을 `CommunityWebSocketBroadcaster`로 분리
- [x] 참여자 ID 조회가 `CommunityMessageService`를 거쳐 Repository에 도달하도록 의존 방향 정리
- [x] Handler에는 JSON 파싱, 프레임 분기, 입력 검증, 인증·저장·전달 호출과 오류 변환만 유지
- [x] 중복된 프레임 생성·연결 정리 코드를 공통 메서드로 합치고 사용하지 않는 상태 제거
- [x] 커밋 이후 전달, 나간 사용자 재확인과 동시 전송처럼 코드만으로 이유가 드러나지 않는 부분에 짧은 주석 추가
- [x] 연결 관리, 참여자 전달과 기존 Handler 계약 테스트를 책임별로 재배치·보강
- [x] `BACKEND.md` 패키지 구조와 책임 설명 갱신

## 예상 변경 범위

- Backend: WebSocket Handler, 연결 관리·실시간 전달 컴포넌트, 메시지 Service와 관련 테스트
- Frontend: 없음
- Database: 없음
- 기타: `BACKEND.md`, 커뮤니티 Task 문서

## 완료 조건

- [x] Handler가 연결 Map, 스케줄 작업과 참여자 Repository를 직접 관리하지 않는다.
- [x] 연결 상태는 `CommunityWebSocketConnections`, 참여자 실시간 전달은 `CommunityWebSocketBroadcaster`에서만 관리한다.
- [x] Repository는 참여 관계 조회만 담당하고 WebSocket 세션·프레임을 알지 않는다.
- [x] 최초 AUTH 제한, 세션 TTL 종료, 메시지 입력 검증과 오류 코드가 T-05 계약과 동일하다.
- [x] 메시지는 DB 저장 커밋 후에만 전달되고 전달 직전 현재 참여 여부를 다시 확인한다.
- [x] 같은 `clientMessageId` 재전송은 기존 메시지를 반환하고 DB 중복을 만들지 않는다.
- [x] 새 추상화와 주석은 실제 책임 분리에 필요한 범위만 추가한다.
- [x] 기존 관련 테스트와 새 테스트가 모두 통과한다.

## 검증 방법

- [x] `cd backend && ./gradlew test`
- [x] `cd frontend && npm test && npm run build`
- [x] 인증 전 프레임, AUTH 시간 초과·세션 만료와 잘못된 입력의 기존 오류·종료 결과 확인
- [x] 같은 방의 두 연결 수신, 미참여·나간 사용자 차단과 중복 전송 결과 확인
- [x] 저장 실패 시 실시간 이벤트가 전송되지 않는지 확인
- [x] `git diff --check`

## 실행 결과

- 변경 내용: Handler에서 연결 Map·타이머·동시 전송을 `CommunityWebSocketConnections`로, 참여자 필터링과 실시간 전달을 `CommunityWebSocketBroadcaster`로, 응답 프레임 생성을 `CommunityWebSocketFrameFactory`로 분리했다. 참여자 조회는 `CommunityMessageService`를 거쳐 Repository에 도달한다.
- 검증 결과: 백엔드 테스트 26개와 프론트엔드 테스트 6개·빌드가 통과했다. 임시 MySQL에서 Flyway 3개 적용과 기존 참여·목록·조회·나가기·재참여 API 회귀를 확인했고 `git diff --check`가 통과했다.
- 남은 문제: T-06의 Redis 발행 추상화는 실제 두 번째 구현이 생길 때 도입한다.
