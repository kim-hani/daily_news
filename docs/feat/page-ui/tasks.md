# 뉴스레터 웹 페이지 UI 개선 — 작업 목록 (Tasks)

> 기준: [`spec.md`](./spec.md), [`plan.md`](./plan.md) · 브랜치: `feat/page-ui` · 작성일: 2026-11-02
> 기존 시스템: [`../../feature/daily-news/tasks.md`](../../feature/daily-news/tasks.md)

## Summary
| 그룹 | 이름 | 작업 범위 | 복잡도 | 작업 수 |
|---|---|---|---|---|
| G1 | 빌드·디자인 기반 | T-001 ~ T-004 | Medium | 4 |
| G2 | 21st.dev 원본 확보 | T-005 | Low | 1 |
| G3 | 공통 레이아웃·테마 | T-006 ~ T-008 | Medium | 3 |
| G4 | 뉴스피드 | T-009 ~ T-013 | High | 5 |
| G5 | 캘린더 | T-014 ~ T-019 | High | 6 |
| G6 | 홈 | T-020 ~ T-021 | Medium | 2 |
| G7 | 파이프라인 연결·검증 | T-022 ~ T-025 | Medium | 4 |
| G8 | 배포 | T-026 ~ T-027 | Medium | 2 |
| G9 | 선택·후속 | T-028 ~ T-030 | Low | 3 |

- **총 30개 작업** / 필수 27개 / 선택 3개
- 진행 원칙: 작업 하나 = 테스트나 점검으로 확인할 수 있는 결과 하나. 모든 작업은 `feat/page-ui` 브랜치에서 하고, main에는 PR로만 반영한다.
- 수집·선정·요약 로직과 `news/` Markdown, README 출력 형식은 바꾸지 않는다(spec C4).
- 컴포넌트를 옮기는 작업(T-008, T-011~T-013, T-016, T-020)은 끝날 때마다 `design-sources.md`의 "바꾼 점"을 채운다(FR-5.3).
- 공통 완료 조건: `./gradlew test` 통과

## Progress
| ID | Task | Group | Required | Status | Notes |
|---|---|---|---|---|---|
| T-001 | Tailwind standalone CLI 다운로드·SHA-256 검증 | G1 | Yes | Done | 2026-11-03: v4.3.4(2026-11-02 릴리스)로 고정. 해시는 릴리스 asset digest와 sha256sums.txt가 일치함을 확인. 검증: 첫 실행 다운로드·검증 성공, `--help` 실행, 두 번째 UP-TO-DATE, 해시 변조 시 실패·파일 삭제, `-PtailwindBinary`(있음=사용, 없음=실패), configuration cache 호환, `./gradlew test` 통과. buildCss용 `ext.tailwindExecutable` 공개. 지원 안 하는 플랫폼은 실행 시 실패하고 `-PtailwindBinary`를 안내 |
| T-002 | buildCss 태스크·app.css 입력 골격 | G1 | Yes | Done | 2026-11-03: `buildCss`(Exec) 추가. 입력 app.css·templates·app.js·Tailwind 버전, 출력 `build/generated/css/static/app.css`(--minify), `processResources`가 의존. 확인: 템플릿에 `text-red-500` 임시 추가 → classpath app.css에 규칙 생김 → 제거 후 재실행, 다시 실행하면 UP-TO-DATE |
| T-003 | Pretendard Variable 셀프호스트 | G1 | Yes | Done | 2026-11-03: npm `pretendard@1.3.9` tarball(sha512 무결성 일치)에서 동적 서브셋 woff2 92개 + `pretendard.css`(url을 같은 폴더 상대 경로로) + `LICENSE`. **plan과 다른 점**: 폰트 CSS를 app.css에 @import하지 않고 별도 `<link>`로 둠(55KB라 app.css 예산과 분리, 상대 경로 유지). `.gitattributes`에 `*.woff2 binary` |
| T-004 | 디자인 토큰·White/Dark 테마 CSS | G1 | Yes | Done | 2026-11-03: 토큰·`@custom-variant dark`·기본 스타일·reduced-motion. Dark 값은 `:root` 안 `@variant dark { }`에 한 번만 적고 Tailwind가 `:root.dark`와 다크 기기 `:root.no-js`로 내보낸다. 대비 계산은 design-sources.md 표 + `ThemeTokensTest`. **추가**: `accent-foreground` 토큰(White에서 강조색 위 #FAFAFA가 4.49:1이라). 컴포넌트 CSS는 utilities보다 우선하도록 cascade layer 밖에 둠 |
| T-005 | 21st.dev 컴포넌트 9개 원본 확보·design-sources.md | G2 | Yes | Done | 2026-11-02: 무료 한도 하루 2회. 9개 demo id 확인, #3 히어로·#9 달력 원본 확보. 나머지 7개 대기(사용자 복사 또는 다음 날 조회). 히어로 동작이 줄 hover 방식으로 확인돼 plan §6.2.1·T-021 수정. 2026-11-03: 9개 원본 모두 확보(#4·#8 magic MCP, 나머지 사용자 복사). 라이선스 확인 완료(MIT 5개, 21st.dev 표기 unknown/no-license 4개는 작성자 원 저장소 MIT. #6·#8·#9는 같은 파일 미확인 → design-sources.md 라이선스 절). #9 링크를 `@edwinvakayil`로 고침 |
| T-006 | OutputPaths 확장·Chrome·layout(head/footer) | G3 | Yes | Done | 2026-11-03: `OutputPaths`에 siteToday·calendarPage·dataDir·archiveJson·fontsDir. `Chrome` record + `Chrome.Nav`(중첩 enum으로 둠). `layout.html`은 `head(chrome, digest)`·`footer(chrome)`, favicon은 `data:,`(404 방지). 검증은 `LayoutTest`(OutputPaths 경로, head 순서·defer, CSP, color-scheme) |
| T-007 | theme-init.js·app.js 골격(배경 전환 로직) | G3 | Yes | Done | 2026-11-03: `theme-init.js`(gzip 443B), `app.js` 골격(훅별 init, reducedMotion·canHover). 브라우저 확인: 저장값 없음→기기 설정, Dark 선택 후 다른 페이지 유지, `dn-theme=abc`→System, 저장소 불가 환경에서 오류 없음, System에서 기기 설정 변경 즉시 반영. 두 파일에 인라인 핸들러·eval 없음. app.js도 `no-js`→`js`를 바꿔 theme-init 실패에 대비 |
| T-008 | Navbar + ThemeSwitcher 조각 | G3 | Yes | Done | 2026-11-03: `fragments/icons.html`(Lucide 1.67.0, 9개), `fragments/nav.html`(navigation-menu-05 밑줄 + theme-switcher-1). 360px에서 로고는 `DN` 마크만, 아이콘 숨김. 스위처 순서는 spec대로 White·Dark·System. `LayoutTest`가 Nav 4가지 aria-current와 라디오 3개를 검증 |
| T-009 | FeedRenderer·FeedPage·today.html | G4 | Yes | Done | 2026-11-03: `HtmlRenderer`→`FeedRenderer`(`render(newsletter, archive)`), `FeedPage`·`Stat`·`Section(count)`·`Card(rank)`. 출력 `today.html`·`archive/날짜.html`, `dn:*` 메타, 자원 복사는 `SiteAssets`(폰트 목록은 pretendard.css에서 읽음). `HtmlRendererTest`→`FeedRendererTest`(기존 검증 유지 + 15개) |
| T-010 | 로컬 미리보기(previewSite) | G4 | Yes | Done | 2026-11-03: `PreviewSite`(테스트 소스) + `previewSite` 태스크. 10-15~11-02(10-18·10-25 없음), 옛 디자인 10-14, 빈 IT, XSS 제목, 홀수 날 실패 피드. 네트워크 없이 성공, 브라우저에서 CSS·폰트·JS 404 없음. 사용법은 plan §9.2(포트만 다르게 써도 됨) |
| T-011 | FeedHeader: 통계 카운트업·키워드 배지 | G4 | Yes | Done | 2026-11-03: `fragments/feed.html`의 `stats`·`keywords`. 숫자 묶음 `aria-hidden` + `p.stats` 문장. 카운트업 0.9초 한 번, reduced-motion이면 최종 숫자만(브라우저 확인). 선정 숫자는 강조색 |
| T-012 | CategoryTabs(animated-navigation-tabs) | G4 | Yes | Done | 2026-11-03: `fragments/feed.html`의 `tabs`. tabs.js 로직을 app.js로 옮김 + `hashchange` 대응 + 탭 바꿀 때 패널 처음으로 스크롤. 활성 막대는 `--indicator-x/-w` + 350ms. sticky `top-14`. JS 없으면 정적 밑줄·모든 섹션 표시(sandbox iframe으로 확인) |
| T-013 | ArticleCard(spotlight-card)·빈 카테고리 | G4 | Yes | Done | 2026-11-03: `fragments/feed.html`의 `card`. 순위·제목·메타 배지·요약·원문 URL(rel noopener noreferrer)·같은 사안. 모바일 1열·lg 2열·최대 680px. 360/768/1280 가로 스크롤 없음, White·Dark 테두리 구분 확인 |
| T-014 | DayDigestReader | G5 | Yes | Done | 2026-11-03: `DayDigest`·`DayDigestReader`. 새 메타 → 옛 마크업(`ul.chips li`, `article.card`) → 실패 시 0건·키워드 없음+경고. fixture `fixtures/archive/old-design.html`(지금 2026-10-31 페이지 복사) |
| T-015 | 캘린더 모델(CalendarPage) 생성 | G5 | Yes | Done | 2026-11-03: `CalendarModel`(CalendarPage/Month/Day는 그 안의 public record). 최신 날 aria-label은 "…, 최신 뉴스레터, 선정 N건, 키워드 …", 읽지 못한 날은 "…, 뉴스레터 있음". `CalendarModelTest` 7개 |
| T-016 | CalendarRenderer·calendar.html(new-calendar) | G5 | Yes | Done | 2026-11-03: `CalendarRenderer`·`calendar.html`·`fragments/calendar.html`. 없는 날은 숨김 문장으로 이름을 준다. 360px 칸 최소 45px(점검 중 40px을 고침). `CalendarRendererTest`, 미리보기에서 날짜 링크 404 없음 |
| T-017 | archive.json·archive.html 리다이렉트 | G5 | Yes | Done | 2026-11-03: jackson-databind 명시, `data/archive.json`(한 줄 JSON + 줄바꿈), `redirect.html`→`archive.html`(meta refresh·canonical·링크). `ArchiveRenderer`·`ArchiveRendererTest`·`templates/archive.html` 삭제, 검증은 `CalendarRendererTest`로 |
| T-018 | 달력 월 이동 JS | G5 | Yes | Done | 2026-11-03: app.js `initCalendar`. 해시·최신 달, 이전/다음, 경계 `disabled`+`aria-disabled`, 제목 `aria-live`, `history.replaceState`. 점검 중 경계에서 포커스가 사라지는 문제를 고침(켜지는 버튼을 먼저 켬) |
| T-019 | 날짜별 뉴스피드 DayNav | G5 | Yes | Done | 2026-11-03: 날짜별 페이지만 `nav[data-day-nav]`. app.js가 archive.json에서 날짜 형식이 맞는 것만 링크. 10-20에서 10-19·10-21 링크 확인. 테스트로 today.html에는 없음을 검증 |
| T-020 | HomeRenderer·home.html(히어로·버튼·Latest) | G6 | Yes | Done | 2026-11-03: `HomeRenderer`·`home.html`·`fragments/home.html`(hero·primary·secondary). `HomeRendererTest` 6개. 360×640에서 두 버튼이 첫 화면 안(아래 끝 347px) |
| T-021 | 히어로 줄 hover 굵기·기울기 효과 | G6 | Yes | Done | 2026-11-03: CSS만으로 효과, app.js는 `--i`와 터치 재생만. **plan과 다른 점**: 500ms → 400ms(NFR-5 상한). 360px hover 상태 1줄 폭 256px. 줄 높이 고정이라 CLS 0 |
| T-022 | DailyNewsPipeline 렌더 단계 교체·통합 테스트 | G7 | Yes | Done | 2026-11-03: 순서 Markdown → Feed → RetentionCleaner → Calendar → Home → README. `PipelineIntegrationTest` 새 산출물·archive.json·옛 디자인 키워드 읽기 추가(바이트 동일 테스트 유지), `PipelineFailureTest`는 today.html로. `--dry-run`은 기존 `DailyNewsAppTest`가 fixture로 검증(실제 네트워크 실행은 하지 않음). 전체 365개 통과 |
| T-023 | PageSafetyTest(전 페이지 공통 검사) | G7 | Yes | Done | 2026-11-03: `PageSafetyTest` 25개. 통합 실행 대신 `PreviewSite.build`(XSS 제목이 모든 뉴스피드에 있고 옛 디자인·두 달 포함)로 만든 사이트를 검사. `style="…"` 임시 삽입 시 실패 확인 후 제거 |
| T-024 | AssetBudgetTest(자원 용량·폰트) | G7 | Yes | Done | 2026-11-03: `AssetBudgetTest`: app.css 7.3KB, app.js 약 4.2KB, theme-init.js 0.44KB(gzip). 합계에는 pretendard.css(13.4KB)도 넣음. @font-face 92개 모두 swap, 외부 url 없음, OFL LICENSE |
| T-025 | 수동 점검 체크리스트 작성·수행 | G7 | Yes | Blocked | 2026-11-03: `qa-checklist.md` 작성·Playwright로 점검(가로 스크롤·JS 끄기·배경 전환·모션·히어로·터치 대상 통과, 점검 중 3건 수정). Lighthouse는 사용자가 DevTools로 측정: 접근성 모두 100, 캘린더 CLS 0.172 → 첫 화면 레이아웃 고정·한글 대체 글꼴 폭 맞춤으로 고쳐 재측정 통과. today LCP 3.1초는 jwebserver 무압축 영향으로 보고 배포 후 재측정. **남은 것**: 키보드 일주, Slow 4G 깜빡임, 스크린리더, Chromium 외 브라우저 |
| T-026 | CI에 Tailwind 캐시 추가 | G8 | Yes | In Progress | 2026-11-03: `ci.yml`·`daily-news.yml`에 `actions/cache@v4`(build/tools/tailwind) 추가. PR을 열어야 CI 로그(linux-x64 해시 검증, 두 번째 실행 캐시 적중)를 확인할 수 있음 |
| T-027 | PR·병합·workflow_dispatch 배포·배포 후 점검 | G8 | Yes | Todo | 2026-11-03: 사용자 확인 필요: PR 생성·병합·workflow_dispatch 실행은 외부에 영향을 주므로 승인 후 진행 |
| T-028 | 카드 등장 애니메이션(FR-3.8) | G9 | No | Done | 2026-11-03: `[data-reveal]` + IntersectionObserver. 첫 화면 밖 요소에만 `pending`을 붙여 JS 실패·reduced-motion이면 처음부터 보임(`html.js` 대신 이 방식을 씀: theme-init만 성공하고 app.js가 실패해도 숨지 않게). CLS 0 |
| T-029 | 캘린더 키워드 팝오버(FR-4.8) | G9 | No | Done | 2026-11-03: 캘린더 칸 키워드 3개 팝오버(CSS `:hover`/`:focus-visible`, `aria-hidden`). 첫·마지막 열은 칸 가장자리에 맞춤, 터치 기기는 띄우지 않음. 360px 가로 스크롤 없음 |
| T-030 | 전환 기간 종료 후 옛 자원 정리 | G9 | No | Todo | D+30 이후(새 디자인 첫 실행일 기준) 별도 브랜치에서 진행 |

---

## G1. 빌드·디자인 기반 (Medium)

### T-001 Tailwind standalone CLI 다운로드·SHA-256 검증
- **Purpose**: Node 없이 Gradle에서 Tailwind를 실행할 수 있게 하고, 내려받은 실행 파일을 검증해 공급망 위험을 막는다(plan §8 공급망).
- **Required**: Yes · **Depends**: — · **Ref**: plan §2, §10.1, §11
- **작업**:
  - `gradle/tailwind.properties`에 Tailwind v4 최신 안정판 버전과 플랫폼별(linux-x64, windows-x64, macos-arm64, macos-x64) SHA-256을 적는다.
  - `build.gradle`에 `downloadTailwind` 태스크를 만든다. `os.name`·`os.arch`로 플랫폼을 고르고, GitHub Releases에서 `build/tools/tailwind/<버전>/`로 내려받은 뒤 해시를 비교한다. 다르면 빌드를 실패시키고 파일을 지운다. 실행 권한을 준다.
  - `-PtailwindBinary=<경로>`를 주면 내려받지 않고 그 파일을 쓴다(오프라인 개발용).
  - 이미 받은 파일이 있고 해시가 맞으면 다시 받지 않는다(up-to-date).
- **완료 조건**: Windows에서 `./gradlew downloadTailwind`가 성공하고 `tailwindcss --help`가 실행된다. 두 번째 실행은 up-to-date다. properties의 해시를 일부러 바꾸면 빌드가 실패한다.

### T-002 buildCss 태스크·app.css 입력 골격
- **Purpose**: 템플릿과 JS에 쓰인 Tailwind 클래스만 담긴 `app.css`를 빌드 때 만들어 classpath에 넣는다.
- **Required**: Yes · **Depends**: T-001 · **Ref**: plan §3.1, §10.1
- **작업**:
  - `src/main/frontend/app.css`를 만든다: `@import "tailwindcss" source(none);`, `@source "../resources/templates";`, `@source "../resources/static/app.js";`, 비어 있는 `@theme {}`.
  - `buildCss`(Exec) 태스크: 입력은 `app.css`·`templates/`·`static/app.js`, 출력은 `build/generated/css/static/app.css`(`--minify`).
  - `sourceSets.main.resources.srcDir(build/generated/css)`, `processResources`가 `buildCss`에 의존하게 한다.
  - `app.css` 생성물은 커밋하지 않는다(`build/` 아래).
- **완료 조건**: 템플릿에 `class="text-red-500"`를 임시로 넣고 `./gradlew processResources`를 실행하면 `build/resources/main/static/app.css`에 그 규칙이 있다(확인 후 제거). 템플릿을 고치면 `buildCss`가 다시 실행되고, 고치지 않으면 up-to-date다. `./gradlew test`가 통과한다.

### T-003 Pretendard Variable 셀프호스트
- **Purpose**: 한글과 히어로 가변 굵기 효과에 쓸 웹폰트를 외부 요청 없이 제공한다(NFR-11, plan §6.2.1).
- **Required**: Yes · **Depends**: T-002 · **Ref**: plan §2, §6.1, §10.1, §11
- **작업**:
  - Pretendard Variable 1.3.9 동적 서브셋 woff2 파일들을 `src/main/resources/static/fonts/`에 넣는다. `LICENSE`(OFL-1.1)도 함께 넣는다.
  - `fonts/pretendard.css`: 서브셋마다 `@font-face`(`font-weight: 45 920`, `unicode-range`, `font-display: swap`, 상대 경로 `url(...)`).
  - `app.css`에서 폰트 CSS를 불러오고, 폰트 대체 목록 `"Pretendard Variable", system-ui, "Apple SD Gothic Neo", "Malgun Gothic", sans-serif`를 `@theme`에 넣는다.
  - `.gitattributes`에 `*.woff2 binary`를 추가한다.
- **완료 조건**: 폰트 파일과 `LICENSE`가 classpath `static/fonts/`에 있다. `pretendard.css`의 모든 `@font-face`에 `font-display: swap`이 있다. 외부 URL(`http`)이 없다.

### T-004 디자인 토큰·White/Dark 테마 CSS
- **Purpose**: 세 화면이 함께 쓰는 색·타이포·간격·반경을 한곳에 두고, White/Dark 전환과 JS 없는 기기 설정 연동을 CSS로 만든다(FR-5.4, FR-7, NFR-4).
- **Required**: Yes · **Depends**: T-002 · **Ref**: plan §6.4, §6.3-3
- **작업**:
  - `@theme`에 shadcn 호환 토큰(`background`, `foreground`, `card`, `muted`, `muted-foreground`, `border`, `ring`, `primary`, `primary-foreground`, `accent`, `spotlight`, `cat-*`)과 Display 크기·반경을 정의한다. 값은 plan §6.4 표를 따른다.
  - `:root`(White), `:root.dark`(Dark), `@media (prefers-color-scheme: dark) { :root.no-js }`(JS 없음 + 다크 기기)의 CSS 변수를 정의한다. Dark 값은 두 선택자를 묶어 한 번만 쓴다. `color-scheme`도 맞춘다.
  - `@custom-variant dark { … .dark … / @media … .no-js … }`를 정의한다.
  - 기본 스타일: `word-break: keep-all; overflow-wrap: anywhere`, 보이는 포커스 링(`focus-visible`), `.visually-hidden`, `@media (prefers-reduced-motion: reduce)`에서 transition·animation 끄기.
  - 텍스트로 쓰는 토큰 조합의 대비를 계산해 `design-sources.md`(T-005에서 만들어진 뒤라면) 또는 이 작업 Notes에 적는다.
- **완료 조건**: 생성된 `app.css`에 `:root.dark`와 `.no-js` 다크 규칙, `dark:` 변형이 있다. `foreground`/`background`, `muted-foreground`/`background`, `muted-foreground`/`muted`, `primary-foreground`/`primary`가 White·Dark 모두 4.5:1 이상이다(계산 결과 기록, SC9).

## G2. 21st.dev 원본 확보 (Low)

### T-005 21st.dev 컴포넌트 9개 원본 확보·design-sources.md
- **Purpose**: 확정된 컴포넌트의 원본 코드와 출처·라이선스를 확보해 이식의 기준으로 삼는다(FR-5.1, FR-5.3, SC11).
- **Required**: Yes · **Depends**: — · **Ref**: spec FR-5.1, plan §3.4, §6.2
- **작업**:
  - magic MCP `search`로 9개 컴포넌트의 demo id를 찾고, `get_component`로 **컴포넌트당 한 번만** 받는다. `get_component`는 조회 횟수를 쓰므로 실패하거나 잠긴 결과는 사용자에게 알리고, 사용자가 21st.dev에서 코드를 복사해 주는 방법으로 대신한다.
  - 받은 원본을 `docs/feat/page-ui/sources/<컴포넌트>.tsx`에 참고용으로 저장한다(빌드에 쓰지 않는다).
  - `docs/feat/page-ui/design-sources.md`를 만든다. 컴포넌트마다 이름, 21st.dev URL, 작성자, 라이선스, 원본 의존성(framer-motion, Radix, react-day-picker 등), 쓰는 화면·조각, 바꾼 점(빈칸으로 시작)을 적는다. Pretendard(OFL-1.1)와 Lucide(ISC) 고지도 넣는다.
- **완료 조건**: 9개 모두 원본 파일과 표 항목이 있다. 라이선스를 확인할 수 없거나 재사용을 허락하지 않는 컴포넌트가 있으면 이 작업을 `Blocked`로 두고 사용자에게 교체 여부를 묻는다.

## G3. 공통 레이아웃·테마 (Medium)

### T-006 OutputPaths 확장·Chrome·layout(head/footer)
- **Purpose**: 모든 새 페이지가 함께 쓰는 경로·`<head>`·푸터를 만든다(FR-1.5, FR-6.4, NFR-8, NFR-10).
- **Required**: Yes · **Depends**: T-003, T-004 · **Ref**: plan §4.1, §5.1, §5.2.1, §6.1, §8
- **작업**:
  - `OutputPaths`에 `siteToday()`, `calendarPage()`, `dataDir()`, `archiveJson()`, `fontsDir()`를 추가한다.
  - `Chrome(title, description, base, current, generatedAt, repositoryUrl)` record와 `Nav { HOME, TODAY, CALENDAR, NONE }` enum을 만든다.
  - `layout.html`을 다시 쓴다.
    - `<html lang="ko" class="no-js">`
    - `head(chrome)`: charset, viewport, `<meta name="color-scheme" content="light dark">`, CSP 메타(plan §8), referrer 메타, description, `theme-init.js`(동기, 스타일시트보다 먼저), `app.css`, `app.js`(`defer`).
    - `footer(chrome)`: 인용·저작권 고지, 생성 시각, GitHub 링크(지금 내용 유지).
  - 옛 `newsletter.html`/`archive.html`이 쓰던 조각 이름과 겹치지 않게 정리하고, 지금 렌더러 테스트가 깨지지 않게 한다.
- **완료 조건**: `OutputPathsTest`가 새 경로를 검증한다. head 조각을 렌더한 결과에서 `theme-init.js`가 첫 `<link rel=stylesheet>`보다 앞이고 `defer`·`async`가 없다. `app.js`에는 `defer`가 있다. CSP 메타와 `color-scheme=light dark`가 있다.

### T-007 theme-init.js·app.js 골격(배경 전환 로직)
- **Purpose**: 첫 화면을 그리기 전에 배경을 정하고, 스위처로 고른 값을 저장·적용한다(FR-7.1~7.7, SC12).
- **Required**: Yes · **Depends**: T-006 · **Ref**: plan §5.2, §5.2.1, §7.2
- **작업**:
  - `static/theme-init.js`(약 20줄): `localStorage['dn-theme']`를 읽는다. `light`/`dark`/`system`이 아니거나 없으면 `system`이다. `system`이면 `matchMedia('(prefers-color-scheme: dark)')`로 판단한다. 다크면 `<html>`에 `dark`를 붙이고 `no-js`를 `js`로 바꾼다. `localStorage` 오류는 삼킨다.
  - `static/app.js` 골격: `DOMContentLoaded`에서 `data-*` 훅별 초기화 함수를 부르는 구조, `prefersReducedMotion()`·`canHover()` 도우미.
  - 배경 전환: `[data-theme-switcher]` 라디오를 저장값에 맞춰 체크하고 `hidden`을 푼다. 바뀌면 저장하고 클래스를 다시 계산한다. `system`일 때만 `matchMedia` `change`를 듣는다.
  - 렌더러가 `theme-init.js`, `app.js`를 `site/assets/`로 복사하게 한다.
- **완료 조건**: 간단한 테스트 페이지(미리보기 또는 임시 HTML)에서 확인한다. 저장값이 없으면 기기 설정을 따른다. Dark를 고르고 새로고침하면 Dark가 유지된다. `dn-theme=abc`이면 System처럼 동작한다. 사생활 보호 모드에서 오류 없이 동작한다. 두 파일에 인라인 이벤트 핸들러나 `eval`이 없다.

### T-008 Navbar + ThemeSwitcher 조각
- **Purpose**: 모든 페이지 상단에 홈·Today·Calendar 이동과 배경 전환 버튼을 둔다(FR-1.4, FR-7.3).
- **Required**: Yes · **Depends**: T-005, T-007 · **Ref**: plan §6.2 표(Navbar, ThemeSwitcher), §6.3
- **작업**:
  - `fragments/icons.html`: 쓰는 Lucide 아이콘(arrow-right, newspaper, calendar, chevron-left/right, external-link, sun, moon, monitor)을 인라인 SVG로 만든다. 장식용은 `aria-hidden="true"`.
  - `fragments/nav.html`: `navigation-menu-05`의 겉모양을 옮긴다. 로고/이름(→홈), `Today`, `Calendar` 링크만 두고 드롭다운은 뺀다. `Chrome.current`에 맞는 링크에 `aria-current="page"`와 활성 스타일을 붙인다. 상단 고정, 흰 배경 + 아래 테두리.
  - 같은 조각 오른쪽에 `theme-switcher-1`을 옮긴다: `<fieldset data-theme-switcher hidden>` + `<legend class="visually-hidden">배경</legend>` + 라디오 3개(`value=light|dark|system`, 아이콘, 각각 `aria-label` "White"/"Dark"/"System"). 원본 상태 로직은 T-007의 `app.js`가 대신한다.
  - 360px에서 한 줄에 들어가게 하고(스위처는 아이콘만), 누를 수 있는 요소는 44×44px 이상으로 한다.
- **완료 조건**: `Nav` 값 4가지로 조각을 렌더하는 테스트가 `aria-current` 위치(NONE이면 없음)를 검증한다. 라디오가 정확히 3개이고 값과 접근 가능한 이름이 맞다. 원본 출처·바꾼 점을 `design-sources.md`에 적었다.

## G4. 뉴스피드 (High)

### T-009 FeedRenderer·FeedPage·today.html
- **Purpose**: 지금의 뉴스레터 페이지를 새 구조로 바꾸고, 오늘의 뉴스피드를 고정 URL `today.html`로 만든다(FR-3.1, FR-3.2, FR-3.5).
- **Required**: Yes · **Depends**: T-006, T-008 · **Ref**: plan §3.2, §4.3, §5.3, §6.2 뉴스피드
- **작업**:
  - `HtmlRenderer`를 `FeedRenderer`로 바꾼다. view record `FeedPage`, `Section(count 추가)`, `Card(rank 추가)`, `Link`, `Stat`을 만든다. 표시 문자열은 Java에서 만든다.
  - 출력: 최신은 `site/today.html`(`base=""`, `Nav.TODAY`), 날짜별은 `site/archive/YYYY-MM-DD.html`(`base="../"`, `Nav.NONE`, `archive=true`).
  - `newsletter.html`을 다시 쓴다. Navbar, 머리말(날짜·제목·실패 피드 경고), 카테고리 영역, 카드 영역, Footer를 의미 클래스(`section.category`, `article.card`, `p.url a` 등)로 먼저 만들고, 꾸밈은 T-011~T-013에서 입힌다. FR-3.2의 정보 항목을 하나도 빼지 않는다.
  - `<head>`에 `dn:date`, `dn:selected`, `dn:keyword`(키워드마다 1개) 메타를 넣는다.
  - 새 자원(`app.css`, `app.js`, `theme-init.js`, `fonts/`)과 옛 자원(`style.css`, `tabs.js`)을 모두 `site/assets/`로 복사한다(plan §3.3).
  - `HtmlRendererTest`를 `FeedRendererTest`로 옮겨 기존 검증을 모두 유지하고, `index.html` 대신 `today.html`을 기대하도록 고친다. `PipelineIntegrationTest`의 기대 파일도 맞춘다(`index.html`은 T-020에서 홈으로 생긴다).
- **완료 조건**: `FeedRendererTest`가 통과한다. 섹션 5개 순서, 모든 카드의 원문 URL(`a[href^=https]`·URL 텍스트), `<script>` 제목 이스케이프, 언론사·시각·보도 수·RSS 요약·같은 사안, 빈 카테고리 문구, `dn:*` 메타, 날짜별 페이지의 `../assets/` 경로를 검증한다.

### T-010 로컬 미리보기(previewSite)
- **Purpose**: 네트워크 없이 샘플 데이터로 사이트를 만들어 디자인을 반복 확인할 수 있게 한다.
- **Required**: Yes · **Depends**: T-009 · **Ref**: plan §9.2
- **작업**:
  - 테스트 소스에 `dailynews.render.PreviewSite` 메인을 만든다. `TestNewsletters`로 **두 달에 걸친 여러 날짜**, 빈 카테고리, XSS 제목, 수집 실패 피드가 있는 날을 만든다.
  - 지금 있는 렌더러로 `build/preview/site`를 만든다. T-016, T-017, T-020에서 캘린더·홈 렌더러가 생기면 여기에 추가한다.
  - `build.gradle`에 `previewSite`(JavaExec) 태스크를 추가한다.
  - 사용법(`./gradlew previewSite` → `jwebserver -d build/preview/site -p 8000`)을 plan §9.2 또는 README 개발 메모에 적는다.
- **완료 조건**: `./gradlew previewSite`가 네트워크 없이 성공하고, `build/preview/site/today.html`과 날짜별 페이지가 생긴다. 브라우저에서 열면 CSS·폰트·JS가 404 없이 불러와진다.

### T-011 FeedHeader: 통계 카운트업·키워드 배지
- **Purpose**: 홈에서 옮겨 온 오늘의 키워드와 통계를 뉴스피드 상단에서 강조해 보여 준다(FR-3.9, NFR-5, NFR-7).
- **Required**: Yes · **Depends**: T-005, T-009 · **Ref**: plan §4.3, §5.2(`data-count-up`), §6.2 뉴스피드
- **작업**:
  - `fragments/stats.html`: `counting-number`를 옮긴다. 숫자 3개(수집·중복 제거·선정)를 크게 보여 준다. 각 숫자 요소는 `data-count-up data-value="1100"`이고 HTML 안에 최종 숫자가 들어 있다. 세는 숫자는 `aria-hidden="true"`로 두고, 옆에 `statsLine` 문장을 작은 글씨로 함께 둔다.
  - `fragments/badges.html`: `base-badge`를 옮겨 키워드 칩(최대 10개)을 만든다. 같은 배지의 작은 변형을 메타 배지용으로 준비한다(T-013에서 사용).
  - `app.js`: IntersectionObserver로 화면에 들어오면 `requestAnimationFrame`으로 0 → 값을 1초 이하에 한 번 센다. reduced-motion이면 실행하지 않는다.
  - 수집 실패 피드 경고를 머리말에 둔다.
- **완료 조건**: 테스트로 키워드 ≤10개, `[data-count-up]` 3개의 `data-value`가 표시 숫자와 같음, `statsLine` 존재, 실패 피드 경고를 검증한다. 미리보기에서 카운트업이 1초 이하로 한 번만 돌고, reduced-motion 에뮬레이션에서는 최종 숫자만 보인다.

### T-012 CategoryTabs(animated-navigation-tabs)
- **Purpose**: 카테고리를 빠르게 바꿔 볼 수 있는 탭을 새 디자인으로 만들고 지금의 접근성 동작을 유지한다(FR-3.3, FR-3.7, NFR-3).
- **Required**: Yes · **Depends**: T-005, T-009 · **Ref**: plan §5.2(tablist, `data-tabs-indicator`), §6.2 뉴스피드
- **작업**:
  - `fragments/tabs.html`: `animated-navigation-tabs`를 옮긴다. 탭 5개(이모지·이름·기사 수), `role=tablist/tab/tabpanel`, `aria-controls`, `aria-selected`, 앵커 `href="#slug"`를 유지한다.
  - 지금 `tabs.js`의 로직(클릭, ←/→/Home/End, 해시 동기화, 해시로 바로 열기)을 `app.js`로 옮긴다.
  - 활성 표시 막대: `[data-tabs-indicator]`의 위치·폭을 CSSOM으로 CSS 변수에 넣고 transition으로 옮긴다(400ms 이하). JS가 없으면 활성 탭에 정적 밑줄을 둔다.
  - 모바일에서 스크롤해도 탭이 상단(Navbar 아래)에 붙어 있게 한다(sticky). 탭이 화면보다 넓으면 가로로 스크롤되게 하되 페이지 전체에는 가로 스크롤이 생기지 않게 한다.
- **완료 조건**: 테스트로 탭 5개 순서, `aria-controls`와 패널 id 일치, 첫 탭만 `aria-selected=true`, 기사 수 표시를 검증한다. 미리보기에서 키보드 조작, `#economy`로 바로 열기, JS를 껐을 때 모든 섹션이 보이고 탭이 앵커로 동작하는지 확인한다.

### T-013 ArticleCard(spotlight-card)·빈 카테고리
- **Purpose**: 기사 카드를 제목 중심으로 훑기 쉽게 만들고 원문 링크 보장을 유지한다(FR-3.2, FR-3.4, FR-3.5, spec C5).
- **Required**: Yes · **Depends**: T-011 · **Ref**: plan §5.2(`data-spotlight`), §6.2 뉴스피드, §8 외부 링크
- **작업**:
  - `fragments/card.html`: `spotlight-card`를 옮긴다. 순위 숫자, 제목(가장 큼), 메타 배지(언론사·시각·N개 언론사 보도, T-011 배지 변형), 요약 인용과 "RSS 요약" 표시, 원문 URL, 같은 사안 링크. 사진은 쓰지 않는다.
  - 기사 원문 링크에 `rel="noopener noreferrer"`를 붙인다.
  - `app.js`: `(hover: hover)` 기기에서만 포인터 위치를 CSS 변수로 넣어 빛 번짐을 그린다(CSSOM). reduced-motion이면 끈다.
  - 레이아웃: 모바일 1열, 1024px 이상 2열, 카드 최대 폭 약 680px.
  - 빈 카테고리 안내("오늘은 선정된 기사가 없습니다.")를 같은 톤으로 꾸민다.
- **완료 조건**: `FeedRendererTest`가 카드마다 순위 번호, `rel` 속성, 기존 정보 항목을 검증한다. 미리보기에서 360/768/1280px 가로 스크롤 없음, White·Dark에서 스포트라이트와 테두리로 카드가 구분되는지 확인한다.

## G5. 캘린더 (High)

### T-014 DayDigestReader
- **Purpose**: 보관된 날짜별 페이지에서 캘린더 칸에 보여 줄 선정 수·키워드를 읽는다(FR-4.7, FR-6.2).
- **Required**: Yes · **Depends**: T-009 · **Ref**: plan §4.4 읽기 규칙, §3.3
- **작업**:
  - `DayDigest(date, selected, keywords)` record와 `DayDigestReader`를 만든다.
  - 새 페이지: `meta[name=dn:selected]`, `meta[name=dn:keyword]`를 읽는다.
  - 옛 페이지: `ul.chips li`(키워드), `article.card` 개수(선정 수)를 읽는다.
  - 읽기·파싱 실패: `selected=0`, `keywords=[]`, 경고 로그, 계속 진행.
- **완료 조건**: `DayDigestReaderTest`가 새 마크업(T-009 렌더 결과), 옛 마크업(지금 `site/archive/2026-10-31.html`을 fixture로 복사), 깨진 파일 세 경우를 검증한다.

### T-015 캘린더 모델(CalendarPage) 생성
- **Purpose**: 보관 날짜 목록으로 월 달력 구조를 계산한다(FR-4.1~4.4, FR-4.7, FR-4.9).
- **Required**: Yes · **Depends**: T-014 · **Ref**: plan §4.4
- **작업**:
  - `CalendarPage`, `CalendarMonth`, `CalendarDay` record와 이를 만드는 로직을 작성한다.
  - 가장 오래된 달부터 최신 달까지 빈 달 없이 이어지게 만든다. 주는 일요일 시작 7칸이고, 앞뒤 빈 칸은 `dayOfMonth=0`이다.
  - 보관된 날만 `href="archive/YYYY-MM-DD.html"`, 최신 날은 `latest=true`. 칸에 대표 키워드 1개와 상위 3개 미리보기 키워드를 넣는다.
  - `ariaLabel`: "2026년 10월 31일 토요일, 뉴스레터 있음, 선정 25건, 키워드 AI·트럼프·이란" / 없는 날은 "…, 뉴스레터 없음".
  - 첫 달의 `prevId`와 마지막 달의 `nextId`는 `null`이다. 보관이 비면 `empty=true`.
- **완료 조건**: `CalendarModelTest`가 두 달에 걸친 보관(10/15~11/2), 한 달만 있는 경우, 보관 없음, 월 첫날이 토요일인 달의 앞 빈 칸 6개, prev/next 경계, aria-label 문구를 검증한다.

### T-016 CalendarRenderer·calendar.html(new-calendar)
- **Purpose**: 날짜를 눌러 그날 뉴스피드로 가는 캘린더 화면을 만든다(FR-4.1~4.3, FR-4.5~4.7, FR-4.9, 사용자 지시 6).
- **Required**: Yes · **Depends**: T-005, T-008, T-015 · **Ref**: plan §6.2 캘린더, §6.2 표(CalendarGrid)
- **작업**:
  - `CalendarRenderer`와 `calendar.html`(`Nav.CALENDAR`)을 만든다. `fragments/calendar.html`에 `new-calendar`의 겉모양을 옮긴다. 원본의 날짜 선택 상태는 버린다.
  - 머리말: "뉴스 캘린더", 보관 안내(최근 30일 + 저장소 Markdown 아카이브 링크).
  - 달마다 `<table data-calendar-month="2026-10">` + `<caption>`(월) + `<th scope="col">`(일~토). 활성 칸은 `<a href="archive/…html" aria-label="…">`, 비활성 칸은 `<span>`(포커스 불가), 최신 날은 강조색. 640px 이상이면 대표 키워드·선정 수, 좁으면 점 표시.
  - 월 이동 버튼 마크업(`data-calendar-prev/next`, 처음엔 `hidden`), 범례(오늘·있음·없음), 빈 보관 안내.
  - 360px에서 7열 유지, 칸 약 44px.
  - `PreviewSite`에 캘린더를 추가한다.
- **완료 조건**: `CalendarRendererTest`가 7열, 활성 칸 = 보관 파일(링크 경로 존재), 비활성 칸에 `<a>` 없음, 최신 날 강조 클래스와 aria-label, 보관 안내·Markdown 링크, 빈 보관 안내를 검증한다. 미리보기에서 날짜를 누르면 그날 뉴스피드가 열린다(404 없음, SC2).

### T-017 archive.json·archive.html 리다이렉트
- **Purpose**: 날짜별 이전/다음 이동용 데이터를 만들고, 옛 주소 `/archive.html`이 깨지지 않게 한다(FR-1.3, FR-6.3, SC3).
- **Required**: Yes · **Depends**: T-016 · **Ref**: plan §3.2, §4.5
- **작업**:
  - `build.gradle`에 `jackson-databind`를 `implementation`으로 명시한다.
  - `site/data/archive.json`(`generatedAt`, `latest`, `dates` 최신순)을 쓴다. `dates`는 `site/archive/*.html` 목록과 같다.
  - `redirect.html` 템플릿으로 `site/archive.html`을 만든다: `<meta http-equiv="refresh" content="0; url=calendar.html">` + 누를 수 있는 링크 + `<link rel="canonical">`.
  - `ArchiveRenderer`와 `ArchiveRendererTest`를 지우고 검증 내용을 `CalendarRendererTest`로 옮긴다.
- **완료 조건**: 테스트가 `archive.json`의 날짜·순서가 보관 파일과 같음, 패턴 밖 파일 제외, `archive.html`의 meta refresh와 링크를 검증한다.

### T-018 달력 월 이동 JS
- **Purpose**: 보관 기간이 두 달에 걸칠 때 한 달씩 넘겨 보게 한다(FR-4.4, FR-4.6, NFR-7).
- **Required**: Yes · **Depends**: T-016 · **Ref**: plan §5.2(`data-calendar*`), §7.2
- **작업**:
  - `app.js`: `[data-calendar]` 안에서 한 달만 보이게 하고, 이전/다음 버튼의 `hidden`을 푼다. 보관 범위 밖이면 버튼을 비활성화한다(`disabled`·`aria-disabled`).
  - 처음에는 `#2026-10` 해시가 가리키는 달, 없으면 최신 달을 보여 준다. 달을 바꾸면 해시를 갱신한다(`history.replaceState`).
  - 월 제목이 바뀐 것을 스크린리더가 알 수 있게 한다(`aria-live="polite"` 영역 또는 포커스 이동).
  - JS가 없으면 모든 달이 쌓여 보인다.
- **완료 조건**: 미리보기(두 달 샘플)에서 처음에 최신 달이 보이고, 이전/다음 이동, 경계 버튼 비활성, `#2026-10` 직접 열기, JS를 껐을 때 모든 달과 날짜 링크가 보이는지 확인한다.

### T-019 날짜별 뉴스피드 DayNav
- **Purpose**: 날짜별 뉴스피드에서 이전/다음 날짜와 캘린더로 바로 이동하게 한다(FR-3.6).
- **Required**: Yes · **Depends**: T-017 · **Ref**: plan §5.2(`data-day-nav`), §6.2 뉴스피드
- **작업**:
  - 날짜별 페이지에만 `<nav data-day-nav data-date="…" data-base="../">`를 넣는다. "캘린더로 돌아가기" 링크(`../calendar.html`)는 항상 HTML에 있다.
  - `app.js`: `archive.json`을 한 번 읽어 이전/다음 날짜 링크를 채운다. 보관 목록에 없는 날짜로는 링크하지 않는다. 읽기 실패 시 이전/다음은 숨긴다.
- **완료 조건**: 테스트가 날짜별 페이지에만 `data-day-nav`와 캘린더 링크가 있고 `today.html`에는 없음을 검증한다. 미리보기에서 첫 날에는 "이전"이, 마지막 날에는 "다음"이 없다.

## G6. 홈 (Medium)

### T-020 HomeRenderer·home.html(히어로·버튼·Latest)
- **Purpose**: 사용자가 지시한 미니멀 홈을 만든다(FR-1.2, FR-2.1~2.5, SC1).
- **Required**: Yes · **Depends**: T-005, T-008, T-009, T-016 · **Ref**: plan §4.2, §6.2 홈, §6.2.1
- **작업**:
  - `HomePage(chrome, latestLabel, isoDate, todayHref, calendarHref)`와 `HomeRenderer`를 만들고 `site/index.html`(`Nav.HOME`)을 쓴다.
  - `fragments/hero.html`: `m-variable-font-hover-2`의 마크업을 옮긴다. `<h1 lang="en" aria-label="EveryDay. News Feed" data-variable-hover>` 안에 `EveryDay.`(1줄)와 `News Feed`(2줄)를 글자마다 `<span aria-hidden="true">`로 정적으로 나눈다. 다른 문구는 두지 않는다.
  - `fragments/buttons.html`: `flow-button`을 옮겨 `<a data-cta="primary" href="today.html">Today</a>`(채움)와 `<a data-cta="secondary" href="calendar.html">Calendar</a>`(외곽선)를 히어로 바로 아래에 둔다. 반복 흐름 효과가 있으면 호버·포커스 때만 돌게 한다.
  - 버튼 아래에 `Latest · <time datetime="2026-11-02">2026-11-02 (월)</time>`를 작은 muted 글씨로 둔다.
  - 히어로를 화면 세로 가운데에 두고, Display 크기는 `clamp()`로 제한한다. 360×640에서 두 줄과 두 버튼이 첫 화면에 들어오게 한다.
  - `PreviewSite`에 홈을 추가한다.
- **완료 조건**: `HomeRendererTest`가 `<h1>` aria-label과 두 줄 구조, 버튼이 정확히 2개(`Today` → `today.html`, `Calendar` → `calendar.html`)이고 이 순서임, `Latest`와 `<time datetime>`, 키워드·통계·헤드라인 요소가 없음, `aria-current`를 검증한다. 미리보기 360×640에서 스크롤 없이 두 버튼이 보인다.

### T-021 히어로 줄 hover 굵기·기울기 효과
- **Purpose**: 두 줄이 서로 반대로 굵어지고 기울어지는 원본 효과로 "센세이셔널"한 첫인상을 준다(G2, NFR-5).
- **Required**: Yes · **Depends**: T-003, T-020 · **Ref**: plan §5.2(`data-variable-hover`), §6.2.1, design-sources #3
- **작업**:
  - CSS: 1줄은 평소 `wght` 100·기울기 0 → hover 시 `wght` 900·`skewX(-10deg)`, 2줄은 그 반대. `transition` 500ms, `cubic-bezier(0.34, 1.56, 0.64, 1)`, `transition-delay: calc(var(--i) * 20ms)`. 2줄은 `muted-foreground`.
  - `app.js`: 글자별 `--i`를 CSSOM으로 넣는다(1줄은 첫 글자부터, 2줄은 마지막 글자부터). 터치 기기는 줄을 탭하면 `.is-active`를 잠시 붙여 한 번 재생한다.
  - reduced-motion이면 transition을 끈다.
- **완료 조건**: 미리보기에서 줄마다 hover 효과와 글자별 지연이 원본 영상처럼 보인다. JS를 끄면 지연 없이 동작한다. reduced-motion에서는 움직임이 없다. 굵기가 바뀌어도 줄 높이와 버튼 위치가 흔들리지 않는다(CLS). 360px에서 hover 상태의 `EveryDay.`도 한 줄에 들어간다.

## G7. 파이프라인 연결·검증 (Medium)

### T-022 DailyNewsPipeline 렌더 단계 교체·통합 테스트
- **Purpose**: 매일 실행 한 번으로 새 화면 전체가 생성되게 한다(FR-6.1~6.4, US-8).
- **Required**: Yes · **Depends**: T-017, T-019, T-020 · **Ref**: plan §3.1, §3.3
- **작업**:
  - `DailyNewsPipeline`의 순서를 `Markdown → Feed → RetentionCleaner → Calendar → Home → README`로 바꾼다. `HtmlRenderer`·`ArchiveRenderer` 참조를 지운다.
  - `PipelineIntegrationTest`를 고친다. 생성 파일(`index.html`, `today.html`, `calendar.html`, `archive.html`, `archive/날짜.html`, `data/archive.json`, `assets/{app.css, app.js, theme-init.js, fonts/…, style.css, tabs.js}`), 보관 정리 후 캘린더·`archive.json`에 남은 날짜만 있음, 옛 마크업 페이지를 미리 넣었을 때 캘린더가 그 날의 키워드를 읽음, **두 번 실행 시 바이트 동일**을 검증한다.
  - `PipelineFailureTest`가 그대로 통과하는지 확인한다(전부 실패 시 `site/` 미생성).
- **완료 조건**: `./gradlew test` 전체가 통과한다. `./gradlew run --dry-run`이 파일을 만들지 않는다.

### T-023 PageSafetyTest(전 페이지 공통 검사)
- **Purpose**: 모든 새 페이지가 보안·접근성·테마 규칙을 지키는지 자동으로 막는다(NFR-3, NFR-8, FR-7.3).
- **Required**: Yes · **Depends**: T-022 · **Ref**: plan §9.1 PageSafetyTest
- **작업**: 통합 실행 결과의 `index.html`, `today.html`, 날짜별 페이지, `calendar.html`, `archive.html`에 대해 다음을 검사하는 테스트를 만든다.
  - `lang="ko"`, viewport, `color-scheme=light dark`, CSP 메타
  - `theme-init.js`가 `defer`·`async` 없이 스타일시트보다 먼저 나옴(리다이렉트 페이지 제외)
  - 배경 스위처 라디오 3개와 접근 가능한 이름(리다이렉트 페이지 제외)
  - `script:not([src])` 0개, `[style]` 0개, `on*` 속성 0개, 외부 `src`/자원 `href` 0개, 남은 `th:` 속성 없음
  - `<script>` 제목이 모든 페이지에서 텍스트로 출력됨
- **완료 조건**: 테스트가 통과한다. 템플릿에 `style="…"`를 임시로 넣으면 실패한다(확인 후 제거).

### T-024 AssetBudgetTest(자원 용량·폰트)
- **Purpose**: 성능 예산을 자동으로 지킨다(NFR-6, NFR-11).
- **Required**: Yes · **Depends**: T-022 · **Ref**: plan §9.1 AssetBudgetTest
- **작업**: classpath의 `static/app.css`, `static/app.js`, `static/theme-init.js`를 gzip으로 압축한 크기를 잰다.
- **완료 조건**: 합계 ≤ 150KB, `app.css` ≤ 30KB, `app.js` ≤ 6KB, `theme-init.js` ≤ 0.5KB. `pretendard.css`의 모든 `@font-face`에 `font-display: swap`이 있다. Pretendard `LICENSE`가 있다. 넘으면 실패한다.

### T-025 수동 점검 체크리스트 작성·수행
- **Purpose**: 자동 테스트로 확인할 수 없는 반응형·Lighthouse·키보드·테마·모션을 점검한다(SC1, SC5~SC9, SC11, SC12).
- **Required**: Yes · **Depends**: T-021, T-023, T-024 · **Ref**: plan §9.3
- **작업**:
  - `docs/feat/page-ui/qa-checklist.md`를 plan §9.3 항목으로 만든다(첫 화면 CTA, 가로 스크롤, Lighthouse, JS 끄기, 키보드, 대비, 배경 전환, 모션, 히어로, 스크린리더, 브라우저, 전환 기간).
  - `./gradlew previewSite` 결과로 점검하고 결과(점수, 통과/실패, 스크린샷 경로)를 기록한다. 실패 항목은 고치거나 해당 작업 Notes에 남긴다.
  - `design-sources.md`에 9개 컴포넌트의 "바꾼 점"이 모두 채워졌는지 확인한다(SC11).
- **완료 조건**: 체크리스트의 모든 항목이 통과다. 특히 세 화면 Lighthouse 모바일 Performance ≥ 90, Accessibility ≥ 95, LCP ≤ 2.5초, CLS ≤ 0.1이다.

## G8. 배포 (Medium)

### T-026 CI에 Tailwind 캐시 추가
- **Purpose**: CI와 매일 실행에서 Tailwind 실행 파일을 안정적으로 쓰고 매번 내려받지 않게 한다(SC10).
- **Required**: Yes · **Depends**: T-001 · **Ref**: plan §10.2
- **작업**: `ci.yml`, `daily-news.yml`의 Gradle 단계 앞에 `actions/cache@v4`(`path: build/tools/tailwind`, `key: tailwind-${{ runner.os }}-${{ hashFiles('gradle/tailwind.properties') }}`)를 추가한다. 다른 권한·커밋·배포 단계는 바꾸지 않는다.
- **완료 조건**: PR에서 `ci / test`가 통과하고, 로그에서 linux-x64 실행 파일의 해시 검증 성공을 확인한다. 두 번째 실행에서 캐시가 맞는다.

### T-027 PR·병합·workflow_dispatch 배포·배포 후 점검
- **Purpose**: 새 화면을 실제 GitHub Pages에 배포하고 결과를 확인한다(SC3, SC6, SC10).
- **Required**: Yes · **Depends**: T-025, T-026 · **Ref**: plan §10.3
- **작업**:
  - `feat/page-ui` → `main` PR을 연다(사용자 확인 후). CI 통과 후 사용자가 병합한다.
  - Actions에서 `daily-news`를 `workflow_dispatch`로 한 번 실행한다(사용자 확인 후).
  - 배포된 사이트에서 `/`, `/today.html`, `/calendar.html`, `/archive.html`, 보관 중인 `/archive/날짜.html`(옛 디자인 포함)이 404 없이 열리는지, 캘린더 날짜 링크, Lighthouse를 다시 점검한다.
- **완료 조건**: 수동 수정 없이 새 화면이 배포됐다. 기존 URL 404가 0건이다. 결과를 Notes와 `qa-checklist.md`에 기록했다.

## G9. 선택·후속 (Low)

### T-028 카드 등장 애니메이션(FR-3.8)
- **Purpose**: 카드가 화면에 들어올 때 짧게 나타나게 해 트렌디한 느낌을 더한다.
- **Required**: No · **Depends**: T-013 · **Ref**: plan §5.2(`data-reveal`), §6.3-5
- **작업**: 카드·섹션에 `data-reveal`을 붙이고, `app.js`가 IntersectionObserver로 `data-revealed`를 더한다. `html.js`일 때만 숨김으로 시작하고, 400ms 이하, reduced-motion이면 끈다.
- **완료 조건**: 미리보기에서 효과가 보이고, JS를 끄거나 reduced-motion이면 처음부터 모두 보인다. Lighthouse CLS ≤ 0.1을 유지한다.

### T-029 캘린더 키워드 팝오버(FR-4.8)
- **Purpose**: 날짜 칸에 마우스를 올리거나 포커스하면 그날 키워드 상위 3개를 미리 보여 준다.
- **Required**: No · **Depends**: T-016 · **Ref**: plan §6.2 캘린더
- **작업**: 활성 칸 안에 키워드 3개 목록을 두고 CSS(`:hover`, `:focus-visible`)로만 보여 준다. 키워드는 이미 aria-label에 있으므로 팝오버는 `aria-hidden="true"`로 둔다. 화면 가장자리 칸에서 잘리지 않게 위치를 조정한다.
- **완료 조건**: 미리보기에서 마우스·키보드 포커스로 팝오버가 보이고, 360px 가장자리 칸에서 가로 스크롤이 생기지 않는다.

### T-030 전환 기간 종료 후 옛 자원 정리
- **Purpose**: 옛 디자인 날짜별 페이지가 모두 지워진 뒤 남은 호환 코드를 없앤다(plan §3.3).
- **Required**: No · **Depends**: T-027 · **Ref**: plan §3.3, §10.3-5
- **작업**: 새 디자인 첫 실행일을 D라 할 때 D+30 이후에 별도 브랜치에서 한다. `style.css`·`tabs.js` 복사와 원본 파일, `DayDigestReader`의 옛 마크업 대체 읽기와 그 fixture·테스트를 지운다.
- **완료 조건**: `site/archive/`에 옛 마크업 페이지가 없음을 확인한 뒤 진행했다. `./gradlew test`가 통과하고 배포 후 404가 0건이다.
