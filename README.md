# Careerpick MVP

React 프론트엔드와 Spring Boot 백엔드로 분리한 채용 플랫폼 MVP입니다.

## 실행

```bash
docker compose up -d

cd backend
./gradlew bootRun
```

새 터미널에서:

```bash
cd frontend
npm install
npm run dev
```

- 프론트엔드: http://localhost:5173
- 백엔드 API: http://localhost:8080/api/jobs
- Swagger UI: http://localhost:8080/swagger-ui.html
- MySQL: localhost:3307 (`careerpick` / `careerpick`)
- Redis: localhost:6379

데모 로그인 계정은 `demo@careerpick.kr` / `career1234`입니다. 공고별 커뮤니티 메시지 작성은 로그인이 필요합니다.

## 테스트

```bash
cd backend && ./gradlew test
cd frontend && npm test
```

백엔드의 첫 실행에서 Gradle 8.14.6을 내려받고 체크섬을 검증합니다. 시스템에 Gradle을 별도로 설치할 필요는 없습니다.

사용자, 채용공고, 공고별 채팅은 MySQL에 저장되고 로그인 세션은 Redis에서 24시간 유지됩니다.
첫 실행 시 Flyway가 `users`, `jobs`, `job_tags`, `chat_messages` 테이블을 생성하고 애플리케이션이 데모 데이터를 추가합니다.
이후 스키마 변경은 `backend/src/main/resources/db/migration`에 다음 버전의 SQL 파일로 추가해야 합니다.

기존에 Hibernate가 만든 로컬 테이블이 있다면 필요한 데이터를 먼저 백업한 뒤 최초 전환 시 볼륨을 초기화하세요. 아래 명령은 로컬 MySQL과 Redis 데이터를 모두 삭제합니다.

```bash
docker compose down -v
docker compose up -d
```

운영 환경에서는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` 환경변수를 설정하세요.
