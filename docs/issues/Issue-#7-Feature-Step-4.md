# Issue #7 — 4단계: DB 저장 기반 구현

- 전체 계획: [Issue-#7-Feature.md](Issue-%237-Feature.md)
- 선행 결정: [3단계 데이터·API 계약](Issue-%237-Feature-Step-3.md)
- 상태: 완료 — DB 기반 구현 및 MySQL 검증 완료 (2026-10-30)

## 1. 작업 목표와 범위

초안과 문항·답변을 MySQL에 저장·조회·수정·삭제할 기반을 만든다. 이번 단계는 마이그레이션·Entity·Repository와 DB 검증까지이며, API·서비스·화면은 다음 단계에서 구현한다.

## 2. 추가·수정할 파일

백엔드 경로는 `backend/` 기준이다.

| 파일 | 역할 |
| --- | --- |
| `src/main/resources/db/migration/V2__create_cover_letter_tables.sql` | 초안·문항 테이블과 제약조건 추가. 구현 직전 버전 중복 확인 |
| `src/main/java/com/careerpick/entity/CoverLetter.java` | 소유자·공고·문항 관계와 시각 관리 |
| `src/main/java/com/careerpick/entity/CoverLetterItem.java` | 질문 스냅샷·순서·제한·답변 보관 |
| `src/main/java/com/careerpick/repository/CoverLetterRepository.java` | 사용자별 목록·초안 조회 |
| `src/test/java/com/careerpick/repository/CoverLetterRepositoryTest.java` | 실제 MySQL 저장·제약조건·삭제 검증 |
| `src/test/resources/application-test.properties` | 별도 테스트 DB 연결 설정, 환경변수로 접속정보 주입 |
| `BACKEND.md` | 새 테이블과 테스트 실행 방법 안내 |

## 3. 구현할 저장 구조

아래는 3단계 계약에 맞춘 구현안이다. 컬럼은 모두 NOT NULL이며 PK는 자동 증가다.

| 테이블 | 컬럼·타입 |
| --- | --- |
| `cover_letters` | `id`, `user_id`, `job_id`: BIGINT / `created_at`, `updated_at`: DATETIME(6) |
| `cover_letter_items` | `id`, `cover_letter_id`: BIGINT / `question_key`: VARCHAR(50) / `question_text`: TEXT / `display_order`, `max_length`: INT / `answer`: TEXT |

- UNIQUE: 초안의 `(user_id, job_id)`, 문항의 `(cover_letter_id, question_key)` 및 `(cover_letter_id, display_order)`.
- FK: 사용자·공고 삭제는 RESTRICT, 초안 삭제 시 문항은 ON DELETE CASCADE.
- 목록 인덱스: `(user_id, updated_at, id)`. 조회 정렬은 `updatedAt DESC, id DESC`다.
- 문항 순서는 양수, 글자 제한도 양수로 검사한다. 답변은 빈 문자열을 허용하며 Java에서 UTF-16 기준 길이를 검사한다.
- Entity는 사용자·공고를 LAZY 참조하고, 부모가 문항을 소유해 함께 저장한다. 사용자·공고로 삭제 cascade를 전파하지 않는다.
- 부모의 문항 컬렉션은 `displayOrder`로 정렬한다. 부모 삭제 시 JPA 관계와 DB 삭제가 일치하도록 매핑한다.
- 생성 시 두 시각을 함께 설정한다. 답변 저장 시 부모 수정 시각을 명시적으로 갱신하고, 단순 조회·기존 초안 열기에서는 바꾸지 않는다.

## 4. 작업 순서

- [x] **테스트 환경 준비:** 개발 DB와 분리된 MySQL 테스트 DB를 사용한다. 기존 데이터·볼륨을 초기화하지 않는다.
- [x] **SQL 추가:** 기존 V1을 유지하고 새 버전으로 테이블·FK·UNIQUE·인덱스를 추가한다.
- [x] **Entity 구현:** 두 기존 질문을 복사해 빈 답변으로 생성하고, 답변 변경·문항 순서·시각을 관리한다.
- [x] **Repository 구현:** `findByUserIdAndJobId`, `findByIdAndUserId`, 사용자별 페이지 조회를 추가한다. 문항 전용 Repository는 만들지 않는다.
- [x] **조회 전략 확인:** 목록은 공고 정보, 상세는 문항을 조회한다. 페이지 조회에 문항 컬렉션 fetch join을 사용하지 않는다.
- [x] **MySQL 통합 테스트:** 아래 검증을 추가하고, `flush`·영속성 컨텍스트 초기화 후 재조회해 실제 저장 결과를 확인한다.
- [x] **문서 정리:** 테스트 결과와 실행 방법을 기록하고 5단계로 넘어간다.

## 5. 검증 및 완료 기준

- 빈 DB와 V1만 적용된 테스트 DB에서 V2 적용이 성공하고 Hibernate 매핑 검증이 통과한다.
- 초안과 두 문항을 저장·재조회할 수 있고, 답변·문항 순서·시각이 유지된다.
- 동일 사용자·공고 중복, 중복 문항 키·순서, 없는 사용자·공고 참조가 DB에서 거절된다.
- 사용자별 목록·상세 조회가 구분되고, 페이지 크기와 최근 수정 순 정렬이 맞는다.
- 답변 수정 후 재조회 값과 부모 수정 시각이 갱신된다. 조회만 하면 수정 시각은 유지된다.
- 부모 삭제 후 문항이 남지 않고, 같은 사용자·공고로 다시 생성할 수 있다. 사용자·공고는 삭제되지 않는다.
- 초안이 있는 사용자·공고 삭제는 FK로 거절된다. JPA 부모 삭제와 SQL 직접 삭제의 문항 정리도 각각 확인한다.
- `backend`에서 `./gradlew test`를 실행한다. 통합 테스트에는 테스트 프로필과 전용 MySQL 연결을 적용하고, 미실행 항목은 이유를 기록한다.

## 6. 실행 결과

- 개발 DB와 분리된 MySQL 8.4 전용 컨테이너(3308)에서 검증했다. 기존 개발 DB·볼륨은 변경하지 않았다.
- 전체 테스트 13개 통과: 새 DB 통합 테스트 8개와 기존 서비스 테스트 5개. 건너뛴 테스트 없음.
- 빈 DB에 V1·V2 적용 및 Hibernate 매핑 검증, 별도 DB의 V1 → V2 업그레이드와 기존 사용자 보존을 확인했다.
- 저장·재조회·답변 검증·수정 시각·사용자별 페이지·UNIQUE·FK·CHECK·JPA/SQL 삭제와 재생성을 검증했다.
- 기존 `JobServiceTest`의 저장되지 않은 공고에 ID가 없어 발생한 실패는 테스트 데이터에 ID를 부여해 수정했다. 제품 공고 코드는 변경하지 않았다.
- 구현과 검증을 완료했다. 다음은 5단계 생성·목록 조회 API 구현이다.
