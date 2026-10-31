# FRONTEND.md

## 1. 문서 목적과 범위

- 범위: Careerpick React 프론트엔드 구현 기준
- 목적: 실제 기술 스택, 화면 책임, 소스 구조와 API 연동 원칙을 일관되게 유지한다.

## 2. 기술 기준선

현재 `package.json`과 `scripts`에 선언된 구성을 프로젝트 기준선으로 사용한다.

| 구분 | 기술 | 기준 |
| --- | --- | --- |
| Language | JavaScript | ES Modules, JSX |
| UI | React | 19.1.1 |
| DOM | React DOM | 19.1.1 |
| Build | esbuild | 0.25.12 |
| Styling | CSS | 단일 `App.css` |
| HTTP | Browser Fetch API | 별도 HTTP 라이브러리 없음 |
| State | React `useState`, `useEffect` | 별도 전역 상태 라이브러리 없음 |
| Browser storage | `localStorage` | 토큰, 자기소개서 초안 |
| Test | Node.js `node:test` | `node:assert` 사용 |
| Dev server | Node.js HTTP server | `localhost:5173` |

현재 Vite, React Router, TypeScript, Redux와 CSS 프레임워크는 사용하지 않는다. 개발 서버와 프로덕션 번들은 프로젝트의 esbuild 스크립트가 담당한다.

## 3. 책임과 경계

프론트엔드는 다음을 담당한다.

- 채용공고 검색, 분야 필터와 상세 화면 표현
- 로그인 입력과 로그인 상태 표시
- 공고별 지원자 채팅 UI
- 서버 자기소개서 목록·페이지 조회와 확인 후 삭제
- 기존 자기소개서 작성과 브라우저 임시 저장(서버 저장 연결은 Step 8에서 진행)
- 화면 전환과 모달 상태 관리
- 반응형 레이아웃, 키보드 조작과 접근성 속성
- API 연결 실패 시 MVP 데모 데이터 표시

프론트엔드는 다음의 최종 책임자가 아니다.

- 인증과 권한 판정
- 입력값의 최종 검증
- 채용공고와 채팅 데이터의 정합성
- 세션 만료와 데이터 공개 범위의 최종 판정

## 4. 현재 소스 구조

화면 조립, 사용자 기능과 공통 통신 코드를 구분한 계층형 구조를 사용한다. 코드가 없는 디렉터리는 미리 만들지 않는다.

```text
frontend/
├── index.html
├── package.json
├── scripts/
│   ├── build-config.mjs
│   ├── build.mjs
│   └── dev.mjs
├── src/
│   ├── main.jsx
│   ├── app/
│   │   ├── App.jsx
│   │   └── App.css
│   ├── pages/
│   │   ├── CoverLetterListPage.jsx
│   │   ├── CoverLetterPage.jsx
│   │   └── JobsPage.jsx
│   ├── features/
│   │   ├── auth/
│   │   │   ├── LoginModal.jsx
│   │   │   └── sessionRequestScope.js
│   │   ├── coverLetters/
│   │   │   ├── pagination.js
│   │   │   └── deleteCoverLetterWithConfirmation.js
│   │   ├── chat/JobChat.jsx
│   │   └── jobs/
│   │       ├── JobCard.jsx
│   │       ├── JobDetail.jsx
│   │       └── sampleJobs.js
│   └── shared/
│       └── api/careerpickApi.js
└── test/
    ├── careerpickApi.test.js
    ├── coverLetterList.test.js
    ├── coverLetterDeletion.test.js
    ├── coverLetterPagination.test.js
    ├── sessionRequestScope.test.js
    └── filter.test.js
```

각 파일의 책임은 다음과 같다.

- `main.jsx`: React 애플리케이션 진입점
- `app`: 앱 셸, 공통 상태와 전역 스타일
- `pages`: 화면 단위 조립과 배치
- `features`: 인증, 채팅, 공고처럼 사용자가 수행하는 기능별 UI와 로직
- `shared/api`: 도메인 UI와 분리된 HTTP 요청 코드
- `test`: UI에서 분리된 순수 로직 단위 테스트

의존 방향은 `app → pages → features → shared`를 기본으로 한다. 한 feature가 다른 feature의 내부 파일을 직접 참조하지 않는다. 독립 URL이 필요해질 때만 React Router 도입을 검토한다.

## 5. 상태와 저장 기준

- 서버 데이터: 채용공고, 사용자 정보, 공고별 채팅과 자기소개서 목록
- React 상태: 현재 화면, 검색어, 분야, 모달, 선택 공고·초안 ID, 목록 페이지·응답 상태와 알림
- `localStorage`: 로그인 토큰, 공고별 자기소개서 초안
- 데모 데이터: 백엔드 연결 실패 시 공고 목록과 채팅의 제한된 대체 화면

브라우저 저장 데이터는 편의 기능일 뿐 신뢰 가능한 서버 데이터로 취급하지 않는다. 로그아웃 시 로그인 토큰과 사용자 상태를 함께 제거한다.

## 6. API와 오류 처리

모든 HTTP 요청은 `src/shared/api/careerpickApi.js`를 통해 호출한다. UI 컴포넌트에서 URL이나 원시 Fetch 설정을 새로 작성하지 않는다.

현재 API 경로는 다음과 같다.

| 기능 | Method | Path |
| --- | --- | --- |
| 공고 조회·검색 | GET | `/api/jobs` |
| 로그인 | POST | `/api/auth/login` |
| 사용자 조회 | GET | `/api/auth/me` |
| 공고 채팅 조회 | GET | `/api/jobs/{jobId}/messages` |
| 공고 채팅 전송 | POST | `/api/jobs/{jobId}/messages` |
| 자기소개서 목록 | GET | `/api/cover-letters?page=0&size=20` |
| 자기소개서 삭제 | DELETE | `/api/cover-letters/{coverLetterId}` |

- 성공하지 않은 HTTP 응답은 오류로 처리한다.
- 공고 조회 실패 시 동일한 검색어와 분야를 샘플 데이터에도 적용한다.
- 내 정보·자기소개서 요청이 현재 토큰의 `401`로 실패하면 인증·선택 상태를 비우고 로그인 창을 연다. 서버·네트워크 오류에서는 토큰을 유지하고 재시도한다.
- 채팅 전송 실패 시 작성 중인 입력을 임의로 제거하지 않는다.
- API 응답 필드는 백엔드 DTO와 동일하게 유지한다.
- 오류는 `status`, `code`, `message`를 보존하며 삭제 성공 `204`에서는 JSON 파싱을 하지 않는다.
- 로그인·내 정보·목록·삭제는 취소 신호를 지원한다. 세션 교체·화면 이탈 후 늦은 응답을 적용하지 않는다.

현재 API 주소는 `http://localhost:8080/api`로 고정되어 있다. 배포 전에는 빌드 환경변수로 분리해야 한다.

## 7. 컴포넌트 원칙

- 서버 통신과 브라우저 저장 로직은 가능한 한 상위 컴포넌트 또는 전용 모듈에 둔다.
- 자식 컴포넌트는 필요한 값과 이벤트만 props로 받는다.
- 파생 가능한 값은 별도 상태로 중복 저장하지 않는다.
- 리스트 항목에는 서버 ID처럼 안정적인 key를 사용한다.
- 버튼, 입력창, 다이얼로그에는 키보드 조작과 접근 가능한 이름을 제공한다.
- 기능이 한 곳에서만 사용될 때 공용 컴포넌트나 훅으로 성급하게 추출하지 않는다.

## 8. 실행과 검증

JSX는 ESLint로 자식 태그 줄바꿈과 2칸 들여쓰기를 검사한다. 텍스트만 있는 태그는 한 줄을 허용한다.
`frontend`에서 `npm run lint`로 검사하고 `npm run lint:fix`로 자동 수정한다.
VS Code에서는 저장소 루트 폴더를 열고 ESLint 확장(`dbaeumer.vscode-eslint`)을 설치하면 공유된 `.vscode/settings.json`으로 JSX 저장 시 자동 수정한다. 의존성은 먼저 `frontend`에서 `npm ci`로 설치한다.

```bash
cd frontend
npm install
npm run dev
```

개발 서버는 `http://localhost:5173`에서 실행된다. 파일 변경은 자동으로 다시 빌드되지만 HMR과 브라우저 자동 새로고침은 제공하지 않는다.

```bash
npm test
npm run build
```

프로덕션 결과물은 `frontend/dist`에 생성된다. 검색·필터처럼 UI와 분리 가능한 로직은 순수 함수로 유지하고 `node:test` 단위 테스트를 추가한다.

## 9. 현재 한계와 변경 기준

- API 주소 환경변수화가 필요하다.
- 로그인 토큰을 `localStorage`에 저장하므로 운영 인증 방식 확정 시 보안 정책을 다시 검토한다.
- 화면 전환은 문자열 상태로 처리하므로 URL 기반 이동이 필요해지면 React Router를 도입한다.
- 인증·자기소개서 요청은 취소·최신 응답 검사를 적용했다. 공고 검색은 별도로 최신 요청 보장이 필요하다.
- 목록 표시·버튼 이벤트는 서버 렌더링 기반 테스트로 확인한다. 실제 DOM·브라우저 E2E 자동화는 아직 없다.

## 10. 자기소개서 목록 화면 (Issue #7 Step 7)

- 상단·하단 자기소개서 탭은 `cover-list`를 연다. 본인 초안만 기본 20개씩 조회하고 서버의 최근 수정 순 정렬을 사용한다.
- 기업명·공고명·한국 시간의 수정 시각·전체 개수와 이전/다음 페이지를 표시한다. 로그인 확인 중·로딩·빈 목록·오류를 구분한다.
- 삭제는 확인 후 실행한다. 취소 시 요청하지 않고, 요청 중 항목·페이지 버튼을 비활성화한다. 실패 시 항목을 유지한다.
- 성공 또는 이미 없는 대상의 `404`에서는 목록을 재조회한다. 결과가 줄어 현재 페이지가 비면 마지막 유효 페이지로 이동한다.
- 로그아웃·재로그인 시 선택 초안·페이지·목록을 초기화한다. 취소·교체된 인증 요청은 현재 사용자 정보를 바꾸지 않는다.
- ‘이어서 작성’은 초안 ID로 `cover-detail`에 이동하지만 현재 제목·목록 복귀 골격만 있다. 실제 상세 작성·저장 연결은 Step 8 범위다.
- 공고에서 연 기존 작성 화면은 여전히 `localStorage`에 저장한다. 여기서 저장한 내용은 서버 목록에 나타나지 않으며 기존 로컬 초안을 자동 이전하지 않는다.
- 목록·삭제 테스트용 서버 초안은 Swagger의 POST로 생성할 수 있다. 삭제 검증에는 삭제해도 되는 테스트 초안을 사용한다.
- 2026-10-31 기준 테스트 26개·빌드 통과. 에이전트 브라우저는 연결되지 않았지만, 이후 사용자가 브라우저에서 목록·페이지 이동·삭제·로그인 정상 동작을 확인했다. 전체 예외·모바일 회귀는 후속 검증에서 이어간다.
