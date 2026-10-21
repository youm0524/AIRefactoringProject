# Careerpick MVP

React 프론트엔드와 Spring Boot 백엔드로 분리한 채용 플랫폼 MVP입니다.

## 실행

```bash
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

데모 로그인 계정은 `demo@careerpick.kr` / `career1234`입니다. 공고별 커뮤니티 메시지 작성은 로그인이 필요합니다.

## 테스트

```bash
cd backend && ./gradlew test
cd frontend && npm test
```

백엔드의 첫 실행에서 Gradle 8.14.6을 내려받고 체크섬을 검증합니다. 시스템에 Gradle을 별도로 설치할 필요는 없습니다.

채팅은 MVP에 맞춰 서버 메모리에 저장되며 서버를 재시작하면 초기화됩니다.
