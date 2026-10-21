# BACKEND.md

## 1. 문서 목적과 범위

- 범위: Careerpick Spring Boot 백엔드 구현 기준
- 목적: 기술 기준선, 계층별 책임, 패키지 구조와 데이터 관리 원칙을 일관되게 유지한다.

## 2. 기술 기준선

현재 `build.gradle`, `gradlew`, `compose.yaml`에 선언된 버전을 프로젝트 기준선으로 사용한다.

| 구분 | 기술 | 기준 |
| --- | --- | --- |
| Language | Java | 17 LTS |
| Framework | Spring Boot | 3.5.7 |
| Build | Gradle | 8.14.6 |
| API | Spring MVC, Bean Validation | REST/JSON |
| API docs | springdoc-openapi, Swagger UI | OpenAPI 3, springdoc 2.8.17 |
| Persistence | Spring Data JPA, Hibernate | 엔티티 매핑과 Repository |
| Database | MySQL | 8.4 |
| Session store | Redis | 7 |
| Migration | Flyway | Spring Boot dependency management |
| Password | Spring Security Crypto | BCrypt |
| Test | JUnit 5, AssertJ, Mockito | `spring-boot-starter-test` |

전체 Spring Security 인증 체계는 아직 사용하지 않는다. 로그인 성공 시 생성한 opaque token을 Redis에 24시간 저장하고 비밀번호 검증에 BCrypt를 사용한다.

## 3. 책임과 경계

백엔드는 다음을 담당한다.

- 로그인 인증과 세션 검증
- 채용공고 조회, 검색, 분야별 필터링
- 공고별 채팅 조회와 메시지 저장
- 입력 검증, 트랜잭션과 데이터 정합성
- MySQL 영속화와 Flyway 스키마 이력 관리

백엔드는 화면 구성, 브라우저 상태와 자기소개서 편집 UI를 담당하지 않는다.

## 4. 패키지 구조

기능별 패키지가 아닌 계층형 패키지를 사용한다.

```text
com.careerpick
├── CareerpickApplication.java
├── config
│   ├── AppConfig.java
│   ├── DataInitializer.java
│   └── OpenApiConfig.java
├── controller
│   ├── AuthController.java
│   ├── ChatController.java
│   └── JobController.java
├── dto
│   ├── ChatMessage.java
│   ├── JobResponse.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── SendMessageRequest.java
│   └── UserView.java
├── entity
│   ├── ChatMessageEntity.java
│   ├── Job.java
│   └── User.java
├── repository
│   ├── ChatMessageRepository.java
│   ├── JobRepository.java
│   └── UserRepository.java
└── service
    ├── AuthService.java
    ├── ChatService.java
    └── JobService.java
```

테스트 패키지는 대상 클래스와 동일한 계층 구조를 따른다.

## 5. 계층 원칙

- `controller`: HTTP 요청 검증과 응답 전달만 담당한다.
- `service`: Use Case, 비즈니스 규칙과 트랜잭션 경계를 담당한다.
- `repository`: Spring Data JPA를 통한 영속성 접근만 담당한다.
- `entity`: MySQL 영속 모델을 표현하며 DTO를 참조하지 않는다.
- `dto`: API 요청과 응답 모델을 표현하며 엔티티를 외부에 직접 노출하지 않는다.
- `config`: 기술 설정과 개발용 초기 데이터 구성을 담당한다.
- Controller가 Repository를 직접 사용하지 않는다.
- 한 도메인의 Service가 다른 도메인의 Repository를 직접 참조하지 않고 해당 Service를 통해 협력한다.
- 사용하지 않는 계층이나 빈 패키지를 미리 만들지 않으며 역방향·순환 의존을 허용하지 않는다.

기본 의존 방향은 다음과 같다.

```text
controller → service → repository → entity
     │           │
     └── dto  ←──┘
```

## 6. 데이터 저장 기준

MySQL에는 다음 테이블을 저장한다.

- `users`: 사용자와 BCrypt 비밀번호 해시
- `jobs`: 채용공고
- `job_tags`: 공고별 태그
- `chat_messages`: 공고별 채팅 메시지

Redis에는 `auth:session:{token}` 형식으로 사용자 ID를 저장하며 TTL은 24시간이다.

## 7. 데이터베이스 마이그레이션

Hibernate는 `ddl-auto=validate`로 매핑만 검증한다. 테이블 생성과 변경은 Flyway만 담당한다.

```text
src/main/resources/db/migration/
└── V1__create_core_tables.sql
```

적용된 마이그레이션 파일은 수정하지 않는다. 변경이 필요하면 `V2__설명.sql`, `V3__설명.sql`처럼 새 파일을 추가한다.

## 8. 로컬 실행과 검증

프로젝트 루트에서 MySQL과 Redis를 먼저 실행한다.

```bash
docker compose up -d
cd backend
./gradlew bootRun
```

기본 로컬 포트는 API `8080`, MySQL `3307`, Redis `6379`이다. MySQL 컨테이너 내부 포트는 `3306`이며 로컬 MySQL과 충돌하지 않도록 호스트에 `3307`로 노출한다. 운영 환경에서는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`를 설정한다.

OpenAPI 명세와 Swagger UI는 백엔드 실행 후 다음 주소에서 확인한다.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

Swagger UI의 `Authorize`에는 로그인 응답의 token 값만 입력한다. 배포 환경에서 문서를 공개하지 않으려면 `OPENAPI_ENABLED=false`를 설정한다.

테스트는 다음 명령으로 실행한다.

```bash
./gradlew test
```

새로운 서비스 로직에는 단위 테스트를 추가하고, DB 매핑이나 마이그레이션 변경에는 실제 MySQL을 사용하는 통합 테스트를 추가한다.
