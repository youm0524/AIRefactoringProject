# Issue #7 — 5단계: 생성·목록 조회 API 구현

- 기준: [3단계 계약](Issue-%237-Feature-Step-3.md), [4단계 DB 기반](Issue-%237-Feature-Step-4.md)
- 상태: 계획 검토 대기 — 사용자 확인 후 구현 시작

## 목표와 범위

공고에서 작성 시작 시 초안을 생성하거나 기존 초안을 반환하고, 본인 초안 목록을 제공한다. 상세 GET·수정·삭제는 6단계에서 진행한다.

## 작업 순서

- [ ] 요청·상세·문항·목록 DTO와 Controller를 추가한다.
- [ ] 기존 인증과 공고 조회를 재사용해 생성·기존 초안 반환을 구현한다.
- [ ] 동시 생성의 중복 충돌은 실패 트랜잭션 종료 후 기존 초안을 조회한다.
- [ ] 사용자별 목록 페이지와 최근 수정 순 정렬을 연결한다.
- [ ] 오류 형식·OpenAPI 인증 표시와 사용자 응답의 `id`를 추가한다.
- [ ] 서비스·Controller·실제 MySQL 동시 생성 테스트를 실행한다.
- [ ] 검증 결과와 백엔드 안내 문서를 갱신한다.

## 구현할 API

| 기능 | 요청 | 응답 |
| --- | --- | --- |
| 공고별 초안 생성·기존 초안 반환 | `POST /api/cover-letters`, 본문 `{jobId}` | 신규 `201`, 기존 `200` + 상세 |
| 본인 초안 목록 | `GET /api/cover-letters?page=0&size=20` | `200` + `{items, page, size, totalElements}` |

- 두 API 모두 Bearer 인증을 사용하고, 사용자 ID는 세션에서 확인한다.
- 목록은 기업명·공고명·최종 수정 시각을 포함하고 `updatedAt DESC, id DESC`로 정렬한다. 기본 20개, 최대 100개를 조회한다.
- 생성 응답은 질문·답변을 포함한다. 기존 초안 반환 시 내용을 변경하지 않는다.
- 오류는 `{code, message}`로 반환하며 입력 오류 `400`, 인증 오류 `401`, 공고 없음 `404`, 서버 오류 `500`을 구분한다.

## 추가·수정할 파일

Java 경로는 `backend/src/main/java/com/careerpick/` 기준이다.

| 대상 | 역할 |
| --- | --- |
| `controller/CoverLetterController.java` | 생성·목록 엔드포인트 |
| `service/CoverLetterService.java` | 인증·공고 확인, 생성·목록 조회, 트랜잭션 처리 |
| `dto/`의 자기소개서 요청·상세·문항·목록 DTO | 입력 검증과 응답 분리 |
| `controller/CoverLetterExceptionHandler.java`, `dto/ApiErrorResponse.java` | 자기소개서 API의 오류 형식 통일 |
| `dto/UserView.java`, `service/AuthService.java` | 로그인·내 정보 응답에 사용자 `id` 추가 |
| `backend/src/test/java/com/careerpick/` | 서비스·Controller·동시 생성 테스트, 기존 인증 테스트 갱신 |
| `backend/BACKEND.md` | API와 검증 방법 안내 |

## 검증할 내용

- 신규 생성, 반복 요청 시 동일 초안 반환, 기존 답변·수정 시각 유지.
- 본인 목록만 조회, 빈 목록, 정렬·페이지 경계 및 잘못된 입력 처리.
- 미인증·만료·잘못된 공고·서버 오류의 상태 코드와 응답 형식.
- 실제 MySQL에서 동시 생성 후 초안 1개·문항 2개 유지, 다른 사용자·공고는 독립 생성.
- 기존 로그인·내 정보 응답의 사용자 ID와 기존 테스트 호환성 확인.

## 완료 기준

생성은 `201`, 기존 초안 반환은 `200`이며 기존 답변·수정 시각을 유지한다. 목록은 본인 데이터만 반환하고, 페이지 입력·인증·공고 없음 오류를 구분한다. 동시 요청에도 초안 1개와 문항 2개만 남아야 한다.
