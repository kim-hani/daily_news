# 뉴스레터 웹 페이지 UI 개선 구현 계획 (feat/page-ui)

> 입력: [spec.md](spec.md) · 기존 시스템: [../../feature/daily-news/plan.md](../../feature/daily-news/plan.md)

## 1. Original Tech Stack Request

- 사용자 입력(`/sdd:plan` 인자): 없음
- 이전 요청: "magic mcp 와 21st.dev를 사용하여 뉴스레터 웹 페이지 ui를 꾸미고 싶어. 일단 내가 먼저 찾아볼건데 디자인 못찾겠으면 너에게도 부탁할게"
- AskUserQuestion으로 정한 항목:

  | 영역 | 선택 |
  |---|---|
  | UI 방식 | **Thymeleaf + Tailwind CLI**: 지금의 Java/Thymeleaf 생성 구조를 유지한다. 21st.dev 컴포넌트의 마크업과 Tailwind 클래스를 템플릿으로 옮기고, React 상태 로직은 바닐라 JS로 바꾼다. Tailwind standalone CLI는 Gradle에서 실행한다(Node 불필요). |
  | 폰트 | **Pretendard 셀프호스트**: 가변 woff2 동적 서브셋을 `site/assets/fonts/`에 넣는다. |
  | 모션 | **CSS + 소량 바닐라 JS**: CSS transition/keyframes와 IntersectionObserver로 구현한다. 모션 라이브러리는 쓰지 않는다. |
  | 테스트 | **JUnit + jsoup + 수동 점검**: 생성된 HTML의 구조·링크·ARIA를 자동 검증하고, 반응형·Lighthouse·키보드 동작은 체크리스트로 수동 점검한다. |

- UI 확정 지시(2026-11-02, spec §1): 흰색 라이트 배경 + White/Dark/System 선택(기본 System), 미니멀 홈(`EveryDay.` / `News Feed` 두 줄 + `Today`·`Calendar` 버튼 + 최신 날짜), 키워드·통계는 뉴스피드로 이동, 21st.dev 컴포넌트 9개 확정(spec FR-5.1, 이 문서 §6.2).
- 프로젝트에서 이미 정해진 것: Java 21, Gradle, Thymeleaf 3.1.5 standalone, jsoup, Jackson, JUnit 6 + AssertJ, GitHub Actions + GitHub Pages(`site/` 배포), 운영 비용 0원.

## 2. Technology Stack

| 영역 | 기술 | 용도 / 이유 |
|---|---|---|
| HTML 생성 | Thymeleaf 3.1.5 (기존) | 홈, 뉴스피드, 캘린더, 리다이렉트 페이지. `th:text`만 써서 이스케이프를 유지한다. |
| 스타일 | **Tailwind CSS v4 standalone CLI** | 21st.dev 컴포넌트의 클래스를 그대로 쓴다. CSS 우선 설정(`@theme`)으로 디자인 토큰을 한곳에서 관리한다(FR-5.4). Node 없이 단일 실행 파일로 돌아간다. |
| 디자인 토큰 | Tailwind `@theme` + CSS 변수(shadcn 호환 이름) | 21st.dev(대부분 shadcn/ui 기반) 코드의 `bg-background`, `text-muted-foreground` 같은 클래스를 고치지 않고 쓸 수 있다. |
| 상호작용 | 바닐라 JS(ES2020, `app.js`는 `defer`, `theme-init.js`는 `<head>` 동기 로드) | 탭과 활성 표시 이동, 달력 월 이동, 이전/다음 날짜, 등장 애니메이션, 히어로 글자 굵기 효과, 카드 스포트라이트, 숫자 카운트업, 배경 전환. 목표: `app.js` 6KB, `theme-init.js` 0.5KB 이하(gzip). |
| 모션 | CSS transition/`@keyframes` + IntersectionObserver | framer-motion 효과를 비슷하게 재현한다. `prefers-reduced-motion`에서는 끈다(NFR-5). |
| 폰트 | Pretendard Variable 1.3.9 동적 서브셋(woff2, `unicode-range`) | 화면에 쓰인 글자 묶음만 내려받는다. `font-display: swap`과 시스템 폰트 대체를 둔다(NFR-11). |
| 아이콘 | Lucide SVG를 인라인으로(Thymeleaf 조각) | 21st.dev가 주로 쓰는 lucide-react를 대신한다. JS 런타임이 필요 없다. |
| JSON 출력 | Jackson databind(기존 의존성) | `site/data/archive.json`(이전/다음 날짜 이동용) |
| HTML 읽기 | jsoup(기존) | 보관된 날짜별 페이지에서 키워드·선정 수를 읽는다(캘린더 칸 정보). |
| 디자인 검색 | magic MCP(21st.dev) | 사용자가 컴포넌트를 못 찾았거나 요청하면 `search` / `get_inspiration` / `get_component`로 후보를 찾는다(FR-5.2). |
| 테스트 | JUnit 6 + AssertJ + jsoup(기존) | 렌더 결과 구조 검증, 자원 용량 예산 검사 |
| 로컬 미리보기 | `jwebserver`(JDK 18+ 내장) | 생성된 `build/preview/site`를 정적 서버로 확인한다. |

## 3. Architecture Overview

### 3.1 전체 흐름

수집·선정·요약 단계는 바꾸지 않는다. **렌더 단계와 빌드 단계만 바뀐다.**

```
[빌드 시]  src/main/frontend/app.css ─┐
           templates/**/*.html  ───────┼─ Tailwind CLI(buildCss) ─▶ build/generated/css/static/app.css
           static/app.js        ───────┘                               │ (processResources가 classpath에 포함)
                                                                       ▼
[실행 시]  … 수집 → 선정 → 요약 → Newsletter
                │
                ├─ MarkdownRenderer          news/YYYY/MM/YYYY-MM-DD.md            (변경 없음)
                ├─ FeedRenderer              site/today.html, site/archive/YYYY-MM-DD.html
                │                            + site/assets/{app.css, app.js, theme-init.js, fonts/*, (구) style.css, tabs.js}
                ├─ RetentionCleaner          site/archive/*.html 30일 초과 삭제     (변경 없음)
                ├─ CalendarRenderer          site/calendar.html, site/archive.html(리다이렉트),
                │     └─ DayDigestReader     site/data/archive.json
                ├─ HomeRenderer              site/index.html
                └─ ReadmeUpdater             README.md                              (변경 없음)
```

순서는 `Markdown → Feed → RetentionCleaner → Calendar → Home → README`다. Calendar는 보관 정리 뒤에 실행해서 남은 날짜만 반영한다(FR-6.3).

### 3.2 페이지와 URL

| URL | 페이지 | 생성 | 비고 |
|---|---|---|---|
| `/`, `/index.html` | **홈** | HomeRenderer | FR-1.2. 지금의 "오늘 뉴스레터" 자리를 홈이 대신한다. |
| `/today.html` | **오늘의 뉴스피드** | FeedRenderer | 최신 뉴스레터. 북마크할 수 있는 고정 URL이다. |
| `/archive/YYYY-MM-DD.html` | **날짜별 뉴스피드** | FeedRenderer | 같은 템플릿을 쓰고, 이전/다음/캘린더 이동을 붙인다. |
| `/calendar.html` | **캘린더 뉴스피드** | CalendarRenderer | 지금의 날짜 목록을 대체한다. |
| `/archive.html` | 리다이렉트 | CalendarRenderer | `meta refresh`로 `calendar.html`로 보내고, 직접 누를 수 있는 링크도 둔다(FR-1.3). |
| `/data/archive.json` | 보관 날짜 목록 | CalendarRenderer | 날짜별 페이지의 이전/다음 링크를 JS로 채운다. |

### 3.3 기존 날짜별 페이지와의 호환(전환 기간)

배포 시점에 이미 있는 `site/archive/2026-10-29~31.html` 같은 페이지는 **옛 마크업 그대로** 남는다. 이 페이지들은 `assets/style.css`, `assets/tabs.js`를 쓰고 `archive.html`로 링크한다.

- 렌더러는 새 자원(`app.css`, `app.js`)과 함께 **옛 자원(`style.css`, `tabs.js`)도 계속 복사한다.** 그래서 옛 페이지 디자인이 깨지지 않는다.
- 옛 페이지에 있는 `index.html` 링크는 홈으로 가고, `archive.html` 링크는 리다이렉트로 캘린더에 간다. 404는 생기지 않는다(SC3).
- 캘린더의 `DayDigestReader`는 옛 마크업(`ul.chips li`, `article.card`)도 읽는다(§4.3).
- **정리 시점**: 새 디자인으로 처음 실행한 날을 D라고 하면, 옛 페이지는 D+29에 모두 삭제된다. D+30 이후 작업에서 옛 자원 복사와 옛 마크업 대체 읽기 코드를 지운다. 이 정리는 tasks에 후속 작업으로 등록한다.

### 3.4 21st.dev 컴포넌트 이식 흐름

```
사용자가 21st.dev에서 고름 ──┐
(못 찾으면) magic MCP 검색 ──┴─▶ 후보 제시 → 사용자 확정 → design-sources.md에 기록
                                    │
                                    ▼
   React/TSX 원본 ── 이식 규칙(§6.3) ──▶ Thymeleaf 조각 + Tailwind 클래스 + app.js 훅(data-*)
```

컴포넌트 9개는 2026-11-02에 모두 확정됐다(§6.2 표). 구현할 때 magic MCP `get_component`로 원본 코드를 한 번씩 받아 이식한다. 이때 라이선스와 작성자를 확인해 `design-sources.md`에 적는다. `get_component`는 조회 횟수를 쓰므로 컴포넌트당 한 번만 받고, 받은 원본은 `docs/feat/page-ui/sources/<컴포넌트>.tsx`에 참고용으로 남긴다(빌드에는 쓰지 않는다).

## 4. Data Models

DB는 없다. 렌더러 입력은 지금의 불변 record `Newsletter`이고, 렌더러마다 화면용 view record를 만든다. 표시 문자열은 Java에서 미리 만들고, 템플릿에는 로직을 두지 않는다.

### 4.1 공통

```java
/** 페이지 공통 값. base는 site 루트까지의 상대 경로("" 또는 "../") */
record Chrome(String title, String description, String base, Nav current, String generatedAt, String repositoryUrl) {}

enum Nav { HOME, TODAY, CALENDAR, NONE }   // aria-current="page" 대상(FR-1.4). 날짜별 페이지는 NONE
```

### 4.2 홈(HomeRenderer)

홈은 미니멀하므로(spec FR-2) 날짜와 링크만 필요하다. 히어로 문구(`EveryDay.` / `News Feed`)와 버튼 글자(`Today` / `Calendar`)는 바뀌지 않는 값이라 템플릿에 직접 쓴다.

```java
record HomePage(Chrome chrome,
                String latestLabel,        // "2026-11-02 (월)"  → "Latest · 2026-11-02 (월)" (FR-2.3)
                String isoDate,            // "2026-11-02" (<time datetime>)
                String todayHref,          // "today.html"
                String calendarHref) {}    // "calendar.html"
```

### 4.3 뉴스피드(FeedRenderer, 지금의 HtmlRenderer를 확장)

지금의 `Page`, `Section`, `Card`, `Link` record를 유지하고 필드만 추가한다(FR-3.2: 정보 항목은 빼지 않는다).

```java
record FeedPage(Chrome chrome, String dateLabel, String isoDate, boolean archive,  // archive=true면 날짜별 페이지
                String heading,                     // "오늘의 뉴스피드" / "2026-10-29 (목) 뉴스피드"
                String statsLine,                   // 기존 한 줄(언론사 이름 포함). 스크린리더·JS 없는 환경용 문장
                List<Stat> stats,                   // FR-3.9 카운트업 숫자 3개
                List<String> failedFeeds, List<String> keywords, int selected,   // keywords: FR-3.9, 최대 10개
                List<Section> sections) {}
record Stat(String label, int value) {}            // 수집 1100 / 중복 제거 443 / 선정 25
record Section(String slug, String label, String emoji, int count, List<Card> cards) {}   // count: 탭 배지
record Card(int rank, String title, String url, String sourceName, String time, String isoTime,
            int outletCount, String summary, boolean rssSummary, List<Link> related) {}
record Link(String name, String url) {}
```

뉴스피드 HTML `<head>`에는 캘린더가 읽을 **기계용 메타 태그**를 넣는다.

```html
<meta name="dn:date" content="2026-10-31">
<meta name="dn:selected" content="25">
<meta name="dn:keyword" content="AI">      <!-- 키워드마다 1개(쉼표가 든 키워드도 안전) -->
```

### 4.4 캘린더(CalendarRenderer, ArchiveRenderer를 대체)

```java
/** 보관된 날짜 하나의 요약. DayDigestReader가 site/archive/YYYY-MM-DD.html에서 읽는다. */
record DayDigest(LocalDate date, int selected, List<String> keywords) {}

record CalendarPage(Chrome chrome, int keepDays, String markdownArchiveUrl,
                    List<CalendarMonth> months,       // 오래된 달 → 최신 달, 빈 달 없이 이어짐
                    String latestMonthId, boolean empty) {}

record CalendarMonth(String id,          // "2026-10" (data-calendar-month, 앵커)
                     String label,       // "2026년 10월"
                     String prevId, String nextId,      // 보관 범위 밖이면 null(버튼 비활성, FR-4.4)
                     List<List<CalendarDay>> weeks) {}  // 일요일 시작 7열, 앞뒤 빈 칸 포함

record CalendarDay(int dayOfMonth,       // 0 = 이번 달이 아닌 빈 칸
                   String isoDate,
                   String href,          // 보관된 날만 "archive/2026-10-31.html", 아니면 null(FR-4.2)
                   boolean latest,       // 최신 뉴스레터 날(FR-4.3)
                   int selected,
                   String topKeyword,    // 칸에 보여 줄 대표 키워드(FR-4.7)
                   List<String> previewKeywords,  // 상위 3개(FR-4.8)
                   String ariaLabel) {}  // "2026년 10월 31일 토요일, 뉴스레터 있음, 선정 25건, 키워드 AI·트럼프·바이"
```

`DayDigestReader` 읽기 규칙:
1. `meta[name=dn:selected]`와 `meta[name=dn:keyword]`가 있으면 그 값을 쓴다(새 페이지).
2. 없으면 옛 마크업에서 읽는다: 키워드는 `ul.chips li`, 선정 수는 `article.card` 개수. 전환 기간용이다(§3.3).
3. 파일을 읽지 못하거나 파싱에 실패하면 `selected=0`, `keywords=[]`로 두고 링크는 유지한다. 경고 로그를 남기고 실행은 계속한다.

### 4.5 `site/data/archive.json`

```json
{
  "generatedAt": "2026-10-31T12:03:00+09:00",
  "latest": "2026-10-31",
  "dates": ["2026-10-31", "2026-10-30", "2026-10-29"]
}
```

`dates`는 최신순이다. RetentionCleaner가 정리한 뒤의 `site/archive/*.html` 목록과 항상 같다.

## 5. API Design

서버 API는 없다(정적 호스팅, spec C1). 대신 다음 **정적 계약**을 지킨다.

### 5.1 파일·URL 계약
§3.2 표의 URL을 지킨다. `OutputPaths`에 `siteToday()`, `calendarPage()`, `dataDir()`, `archiveJson()`, `fontsDir()`를 추가하고, 경로는 여기서만 정한다.

### 5.2 HTML ↔ JS 계약(`data-*` 훅)
스타일 클래스(Tailwind)와 동작 훅을 분리해서, 디자인을 갈아 끼워도 JS가 깨지지 않게 한다.

| 훅 | 위치 | `app.js` 동작 | JS 없을 때 |
|---|---|---|---|
| `[role=tablist]` / `[role=tab]` / `[role=tabpanel]` | 뉴스피드 | 지금 `tabs.js` 로직(클릭, ←/→/Home/End, 해시 동기화) | 탭은 섹션 앵커, 모든 섹션 표시 |
| `[data-calendar]`, `[data-calendar-month]`, `[data-calendar-prev]`, `[data-calendar-next]` | 캘린더 | 한 달씩 표시, 이전/다음 이동, `#2026-10` 해시 | 모든 달을 쌓아서 표시, 이동 버튼은 `hidden` |
| `[data-day-nav]` (`data-date`, `data-base`) | 날짜별 뉴스피드 | `archive.json`을 읽어 이전/다음 링크 채우기 | "캘린더로 돌아가기" 링크만 표시 |
| `[data-tabs-indicator]` | 뉴스피드 탭 | 활성 탭 위치·폭으로 표시 막대를 옮긴다(`animated-navigation-tabs`) | 활성 탭에 정적 밑줄 |
| `[data-reveal]` | 카드, 섹션 | 화면에 들어오면 `data-revealed` 추가 → CSS 등장 효과 | 처음부터 보임(`html.js`일 때만 숨김 시작) |
| `[data-variable-hover]` | 홈 히어로 | 글자별 지연 순서(`--i`)를 CSSOM으로 넣는다. 효과 자체는 CSS `:hover`(`m-variable-font-hover-2`, §6.2.1). 터치 기기는 탭할 때 한 번 재생 | 효과는 동작하되 글자별 지연 없음 |
| `[data-spotlight]` | 기사 카드 | 포인터 위치를 따라 빛 번짐(`spotlight-card`). 마우스에서만 동작 | 효과 없음 |
| `[data-count-up]` (`data-value`) | 뉴스피드 통계 | 화면에 들어오면 0 → 값으로 1초 이하 한 번 세기 | 최종 숫자 표시 |
| `[data-theme-switcher]` (`input[type=radio]` 3개) | 공통 내비게이션 | 값 저장, `<html>` 클래스 변경, System이면 `matchMedia` 변경 감지 | `hidden`(CSS가 기기 설정을 따름, FR-7.7) |

`<html>`의 `no-js` 클래스는 `theme-init.js`가 `js`로 바꾼다(`app.js`보다 먼저 실행된다). "JS가 있을 때만 숨기는" 스타일은 모두 `.js` 아래에 둔다(NFR-7, SC7).

포인터를 따라 움직이는 효과(히어로 굵기, 스포트라이트, 탭 표시 막대)는 `element.style.setProperty('--x', …)`처럼 **CSSOM으로 CSS 변수만 바꾼다**. CSP `style-src 'self'`는 HTML의 `style="…"` 속성은 막지만 CSSOM 조작은 막지 않는다. 그래서 인라인 스타일 0개 규칙(§6.3-8)을 지킬 수 있다.

### 5.2.1 배경 전환 순서(FR-7)

```
<head>
  <script src="assets/theme-init.js"></script>    ← 동기 로드, CSS보다 먼저
  <link rel="stylesheet" href="assets/app.css">
```

1. `theme-init.js`(동기, 약 20줄): `localStorage['dn-theme']`를 읽는다. 값은 `light` / `dark` / `system`이고, 없거나 잘못된 값이면 `system`이다(FR-7.2). `system`이면 `matchMedia('(prefers-color-scheme: dark)')`로 판단한다. 결과가 다크면 `<html>`에 `dark` 클래스를 붙이고, `no-js`를 `js`로 바꾼다. 첫 화면을 그리기 전에 끝나므로 깜빡임이 없다(FR-7.5).
2. `app.js`(defer): 스위처 라디오의 `checked`를 저장값에 맞추고, 바뀌면 저장하고 클래스를 다시 계산한다. `system`일 때만 `matchMedia` `change` 이벤트를 듣는다(FR-7.6).
3. `localStorage`에 접근할 수 없으면(사생활 보호 모드 등) 메모리 값으로만 동작한다. 오류는 삼킨다.

인라인 스크립트를 쓰면 더 빠르지만 CSP(`script-src 'self'`)와 PageSafetyTest 규칙을 깨므로, 같은 출처의 작은 동기 스크립트를 쓴다. 이 파일은 렌더링을 막지만 0.5KB 이하라 LCP 영향이 작다.

### 5.3 기계용 메타 태그
§4.3의 `dn:*` 메타 태그다. 캘린더 생성기만 읽는다.

## 6. Component Structure

### 6.1 템플릿 구성

```
src/main/resources/templates/
├─ layout.html            head(chrome) · header(chrome) · footer(chrome)   ← 다시 작성
├─ fragments/
│  ├─ icons.html          lucide SVG 조각(arrow-right, calendar, chevron-left/right, external-link, sun, moon, monitor …)
│  ├─ nav.html            상단 내비게이션(navigation-menu-05) + 배경 전환(theme-switcher-1)
│  ├─ hero.html           두 줄 히어로(m-variable-font-hover-2)
│  ├─ buttons.html        flow-button(primary / secondary 변형)
│  ├─ badges.html         키워드 배지(base-badge), 메타 배지
│  ├─ stats.html          통계 숫자(counting-number)
│  ├─ tabs.html           카테고리 전환(animated-navigation-tabs)
│  ├─ card.html           기사 카드(spotlight-card)
│  └─ calendar.html       월 달력 칸(new-calendar)
├─ home.html              홈
├─ newsletter.html        오늘의/날짜별 뉴스피드(기존 파일을 다시 작성)
├─ calendar.html          캘린더
└─ redirect.html          archive.html 리다이렉트
src/main/frontend/app.css Tailwind 입력(토큰·기본 스타일·컴포넌트 레이어)
src/main/resources/static/
├─ app.js                 상호작용(§5.2)
├─ theme-init.js          배경 초기 적용(§5.2.1)
├─ fonts/                 Pretendard 동적 서브셋 woff2 + pretendard.css(@font-face) + LICENSE(OFL)
├─ style.css, tabs.js     (구) 전환 기간 동안 유지
```

### 6.2 화면별 구성(컴포넌트 슬롯)

사용자가 확정한 컴포넌트(spec FR-5.1)를 슬롯에 넣는다. 표에 없는 슬롯(Footer, DayNav, EmptyState, Legend)은 이 컴포넌트들의 토큰·반경·타이포를 따라 직접 만든다.

| 슬롯 | 화면 | 컴포넌트 | 조각 | 이식 메모 |
|---|---|---|---|---|
| Navbar | 공통 | `@shadcnui-blocks/navigation-menu-05` | `nav.html` | 원본은 "Underlined Navigation Menu"(아이콘 + 활성·hover 항목 아래 움직이는 밑줄)다. 링크는 로고(→홈) · Today(아이콘 newspaper) · Calendar(아이콘 calendar) 세 개뿐이다. 현재 페이지는 `aria-current`와 밑줄. |
| ThemeSwitcher | 공통(Navbar 오른쪽) | `@ncdai/theme-switcher-1` | `nav.html` | `fieldset` + 라디오 3개(☀ White / 🌙 Dark / 🖥 System, 각각 `aria-label`). §5.2.1 |
| Hero | 홈 | `@cnippet-dev/m-variable-font-hover-2` | `hero.html` | §6.2.1 |
| CTA 버튼 | 홈 | `@xubohuah/flow-button` | `buttons.html` | `Today`는 primary(채움), `Calendar`는 같은 컴포넌트의 secondary(외곽선). 둘 다 `<a>`다. |
| KeywordBadge | 뉴스피드 | `@sean0205/base-badge` | `badges.html` | 키워드 칩. 메타 배지(언론사·N개 언론사 보도)도 같은 배지의 작은 변형으로 통일한다. |
| StatsCounter | 뉴스피드 | `@cnippet-dev/counting-number` | `stats.html` | `[data-count-up]`. HTML에는 최종 숫자를 넣고 JS가 0부터 센다. |
| CategoryTabs | 뉴스피드 | `@ln-dev7/animated-navigation-tabs` | `tabs.html` | 기존 tablist ARIA를 유지한다. framer-motion `layoutId` 표시 막대는 `[data-tabs-indicator]` + CSS transition으로 바꾼다. |
| ArticleCard | 뉴스피드 | `@preetsuthar17/spotlight-card` | `card.html` | `[data-spotlight]`. 사진 없이 텍스트만 쓴다(spec C5). |
| CalendarGrid | 캘린더 | `@edwinvakayil/new-calendar` | `calendar.html` | 원본(`sources/new-calendar.tsx`)의 흑백 팔레트·선택 칸 그라디언트·spring 이징을 토큰과 클래스로 옮긴다(design-sources #9). 날짜 선택 상태와 연·월 선택 팝오버는 버리고, `<table>` 마크업(아래)에 입힌다. 원본의 "선택 칸" 스타일은 최신 날짜 칸에 쓴다. 활성 칸은 날짜별 뉴스피드로 가는 링크다. |

**공통**
- Navbar: 상단 고정, 흰 배경 위 얇은 아래 테두리(Dark에서는 같은 위치에 어두운 테두리). 모바일에서도 링크 3개와 스위처가 한 줄에 들어간다. 360px에서 스위처는 아이콘만 보인다.
- Footer: 인용·저작권 고지, 생성 시각, GitHub 링크(FR-1.5). 작은 회색 글씨.

**홈(`home.html`)**: 미니멀(spec FR-2)
```
Navbar
Hero (화면 세로·가로 가운데 정렬, 원본과 같음)
 ├─ <h1 aria-label="EveryDay. News Feed">
 │     EveryDay.                     ← 1줄 (m-variable-font-hover-2)
 │     News Feed                     ← 2줄
 ├─ [ Today → ]  [ Calendar ]        ← flow-button ×2, 텍스트 바로 아래   FR-2.2
 └─ Latest · 2026-11-02 (월)          ← 작은 muted 글씨, <time>              FR-2.3
Footer
```
키워드, 통계, 헤드라인은 두지 않는다(FR-2.5). 360×640에서 히어로와 두 버튼이 첫 화면에 들어오도록(FR-2.4) Display 크기를 `clamp()`로 제한한다. 세로 가운데 정렬은 `min-h-[calc(100svh-<navbar>-<footer>)]`로 하되, 내용이 넘치면 위에서부터 쌓이게 한다.

#### 6.2.1 히어로 이식(m-variable-font-hover-2)

원본(`sources/m-variable-font-hover-2.tsx`, design-sources #3)은 두 줄이 서로 반대로 움직이는 구조다. **줄에 마우스를 올리면** 그 줄의 글자가 차례로 바뀐다(포인터 거리 방식이 아니다).

| 줄 | 평소 | hover | 지연 순서 | 색 |
|---|---|---|---|---|
| 1줄 `EveryDay.` | `wght` 100, 기울기 0 | `wght` 900, 기울기 -10° | 첫 글자부터 20ms 간격 | `foreground` |
| 2줄 `News Feed` | `wght` 900, 기울기 -10° | `wght` 100, 기울기 0 | 마지막 글자부터 20ms 간격 | `muted-foreground` |

- 글자 굵기 변화에는 가변 폰트가 필요하다. Pretendard Variable의 라틴 글리프(`wght` 45~920)를 쓴다.
- Pretendard에는 `slnt` 축이 없다. 기울기는 글자별 `transform: skewX(…)`로 대신한다(`display: inline-block` 필요).
- 글자마다 `<span>`으로 나누는 작업은 **템플릿에서 정적으로** 한다(문구가 고정이라 Java 데이터가 필요 없다). 글자 `<span>`들은 `aria-hidden="true"`로 두고, `<h1>`에 `aria-label="EveryDay. News Feed"`를 붙인다. 스크린리더가 글자를 하나씩 읽지 않게 하기 위해서다. 공백도 `<span>`으로 둔다.
- 효과는 **CSS만으로** 만든다: `.hero-line:hover > span { font-variation-settings: "wght" 900; transform: skewX(-10deg) }`, `transition: font-variation-settings 500ms, transform 500ms`, 이징은 spring 근사 `cubic-bezier(0.34, 1.56, 0.64, 1)`. 지연은 `transition-delay: calc(var(--i) * 20ms)`이다.
- `app.js`는 글자별 `--i`(1줄은 0부터 증가, 2줄은 끝에서부터)만 CSSOM으로 넣는다. JS가 없으면 지연 없이 한꺼번에 바뀐다.
- 터치 기기(`hover: none`)는 줄을 탭하면 한 번 재생되게 `.is-active` 클래스를 잠시 붙인다. reduced-motion이면 transition을 끄고 평소 상태로 둔다.
- 원본의 "Hover the headline" 안내 문구는 쓰지 않는다(사용자 지시: 두 줄만).
- 굵기가 바뀌면 글자 폭이 달라진다. 줄 너비가 흔들려 버튼이 밀리지 않도록 줄마다 고정 높이를 두고, 360px에서 가장 넓은 상태(1줄 hover의 `wght` 900)로도 한 줄에 들어가는 Display 크기를 고른다.
- 히어로 텍스트는 영어이므로 `<h1 lang="en">`을 붙인다(spec A7).

**뉴스피드(`newsletter.html`)**
```
Navbar
FeedHeader                 날짜(대형), 제목, 수집 실패 피드 경고
 ├─ StatsCounter           1100 → 443 → 25 (수집 · 중복 제거 · 선정)  counting-number   FR-3.9
 │                         + statsLine 원문(언론사 이름 포함)은 작은 글씨로 함께 둔다(FR-3.2)
 └─ KeywordBadge × ≤10     오늘의 키워드  base-badge                              FR-3.9
DayNav (날짜별만)           ← 이전 날짜 · 캘린더로 돌아가기 · 다음 날짜 →   FR-3.6
CategoryTabs (sticky)      5개 탭 + 기사 수, 활성 표시 막대 이동  animated-navigation-tabs   FR-3.3, FR-3.7
CategoryPanel × 5
 └─ ArticleCard × n        spotlight-card. 순위 숫자 · 제목(가장 큼) · 메타 배지(언론사·시각·N개 언론사 보도)
                           · 요약 인용(+RSS 요약 표시) · 원문 URL · 같은 사안 링크               FR-3.2, FR-3.4
 └─ EmptyState             "오늘은 선정된 기사가 없습니다."
Footer
```
통계 숫자는 스크린리더가 세는 도중의 값을 읽지 않도록, 카운트되는 숫자 요소에 `aria-hidden="true"`를 두고 옆에 최종 문장(`statsLine`)을 둔다.

카드 레이아웃은 모바일 1열, 1024px 이상 2열이다. 본문 폭은 가독성을 위해 카드 하나당 최대 약 680px로 둔다.

**캘린더(`calendar.html`)**
```
Navbar
CalendarHeader             "뉴스 캘린더", 보관 안내(최근 30일 + Markdown 아카이브 링크)   FR-4.5
MonthSwitcher              ‹ 2026년 10월 ›  (보관 범위 밖이면 비활성)                    FR-4.4
CalendarGrid (table)       new-calendar 스타일. 일~토 7열. 칸: 날짜 숫자 + 점 + 대표 키워드/선정 수(≥640px)   FR-4.1~4.3, 4.7
 └─ 활성 칸 <a href="archive/YYYY-MM-DD.html">: 누르면 그날 뉴스피드로 이동                FR-4.2
 └─ 호버/포커스 시 키워드 상위 3개 팝오버(CSS만)                                         FR-4.8
Legend                     오늘 · 뉴스레터 있음 · 없음
EmptyState                 "아직 보관된 뉴스레터가 없습니다."                            FR-4.9
Footer
```

마크업은 `<table>`에 `<caption>`(월)과 `<th scope="col">`(요일)을 쓴다. 활성 칸은 `<a aria-label="…">`, 비활성 칸은 `<span>`(포커스 불가)이다. 360px에서 칸은 약 44px이며 7열을 유지한다(NFR-1, NFR-2).

### 6.3 21st.dev → Thymeleaf 이식 규칙

1. **마크업**: JSX를 HTML로 옮긴다(`className`→`class`, `htmlFor`→`for`, self-closing 정리). 반복은 `th:each`, 값은 `th:text`/`th:href`로 바꾼다. `th:utext`는 쓰지 않는다.
2. **조건부 클래스**: `cn()`/`clsx()`는 정적 문자열로 풀거나 `th:classappend="${x} ? 'bg-primary text-primary-foreground' : ''"`처럼 **완전한 클래스 이름 리터럴**로 쓴다. Tailwind가 소스를 스캔해서 찾을 수 있어야 하므로 문자열 조합은 금지한다.
3. **토큰**: shadcn 토큰 이름(`background`, `foreground`, `primary`, `muted`, `card`, `border`, `ring`, `accent` 등)을 `@theme`에 같은 이름으로 정의한다. 그러면 원본 클래스를 그대로 쓸 수 있다. 원본의 하드코딩 색(`bg-zinc-900` 등)은 토큰으로 바꾼다.
4. **상태/이벤트**: `useState`/`onClick`은 `data-*` 훅과 `app.js`로 바꾼다. 콘텐츠는 항상 HTML 안에 있어야 한다(NFR-7).
5. **모션**: `motion.div`의 initial/animate는 `[data-reveal]` + CSS transition으로, `layoutId` 이동은 CSS 변수 + transition으로, `useSpring`/카운터는 `requestAnimationFrame`으로 바꾼다. 길이는 400ms 이하(카운트업만 1초 이하)다. `@media (prefers-reduced-motion: reduce)`와 JS의 `matchMedia` 검사로 장식 효과를 모두 끈다(spec NFR-5). `flow-button`에 반복 흐름 효과가 있으면 호버·포커스 때만 돌게 한다.
6. **아이콘**: `lucide-react`는 `fragments/icons.html`의 인라인 SVG로 바꾼다. 장식용은 `aria-hidden="true"`, 의미가 있으면 `<title>`이나 버튼의 `aria-label`을 쓴다.
7. **외부 자원**: 원본의 외부 이미지·폰트·스크립트 URL(unsplash, Google Fonts 등)은 지운다(spec C5, CSP).
8. **인라인 스타일 금지**: CSP(`style-src 'self'`) 때문에 `style="…"` 속성은 쓰지 않는다. 지연 시간 같은 값은 유틸리티 클래스나 `nth-child` 규칙으로 바꾼다.
9. **기록**: 컴포넌트마다 `docs/feat/page-ui/design-sources.md`에 이름, 21st.dev URL/ID, 작성자, 라이선스, 바꾼 점을 적는다(FR-5.3, SC11).

### 6.4 디자인 토큰 초안

최종 값은 컴포넌트 원본 코드를 받은 뒤 조정한다. 방향은 **"흰 바탕의 에디토리얼 볼드"**다. 순백 배경에 흑백 고대비를 쓰고, 강조색은 하나만 두며, 큰 헤드라인 타이포를 쓴다. 원본 컴포넌트에 들어 있는 색은 모두 아래 토큰으로 바꾼다(§6.3-3).

| 토큰 | White | Dark | 비고 |
|---|---|---|---|
| `background` | `#FFFFFF` | `#09090B` | 사용자 지시: 흰색 배경 |
| `foreground` | `#0A0A0A` | `#FAFAFA` | |
| `card` | `#FFFFFF` | `#18181B` | White에서는 배경과 같은 색이라 `border`와 스포트라이트로 카드를 구분한다. |
| `muted` / `muted-foreground` | `#F4F4F5` / `#52525B` | `#27272A` / `#A1A1AA` | 보조 텍스트도 4.5:1 이상 |
| `border` | `#E4E4E7` | `#27272A` | |
| `spotlight` | `rgb(10 10 10 / 0.06)` | `rgb(250 250 250 / 0.08)` | spotlight-card 빛 번짐 |
| `primary` / `primary-foreground` | `#0A0A0A` / `#FAFAFA` | `#FAFAFA` / `#0A0A0A` | CTA |
| `accent`("breaking") | `#E11D48` | `#FB7185` | 오늘 날짜 칸, 순위 숫자, 강조. 텍스트로 쓰면 대비를 검증한다. |
| `cat-politics/society/economy/it/global` | indigo/amber/emerald/sky/violet 600 | 같은 색 계열 400 | 점·테두리 위주로 쓰고, 텍스트 색으로는 쓰지 않는다. |
| 폰트 | `Pretendard Variable`, system-ui, "Apple SD Gothic Neo", "Malgun Gothic", sans-serif | | |
| Display | `clamp(3rem, 14vw, 9rem)` / 굵기는 §6.2.1 / `-0.025em`(원본 `tracking-tight`) / 줄간격 0.95 | | 홈 히어로 두 줄. 360px에서 `EveryDay.`(wght 900)가 한 줄에 들어가는지 확인한다. |
| 반경 | 카드 `1rem`, 버튼·칩 `9999px` | | |

배경 전환(spec FR-7, NFR-4)은 **클래스 방식**으로 한다. 시스템 설정을 직접 따르는 Tailwind 기본 `dark:` 변형은 쓰지 않는다. System 선택도 `theme-init.js`/`app.js`가 판단해서 클래스로 바꾼다(§5.2.1).

```css
/* 21st.dev 원본의 dark: 클래스를 그대로 쓸 수 있다.
   JS가 있으면 .dark 클래스로, JS가 없으면(.no-js) 기기 설정으로 다크를 판단한다(FR-7.7). */
@custom-variant dark {
  &:where(.dark, .dark *) { @slot; }
  @media (prefers-color-scheme: dark) { &:where(.no-js, .no-js *) { @slot; } }
}
:root       { --background: #FFFFFF; color-scheme: light; … }   /* White */
:root.dark  { --background: #09090B; color-scheme: dark;  … }   /* Dark */
@media (prefers-color-scheme: dark) {
  :root.no-js { --background: #09090B; color-scheme: dark; … }  /* JS 없음 + 다크 기기 */
}
```

Dark 토큰 값은 `:root.dark`와 `:root.no-js`(다크 기기)에서 함께 쓴다. 두 번 적지 않도록 `app.css`에서 선택자를 묶는다. `<meta name="color-scheme">`는 지금처럼 `light dark`를 유지한다. 한글 줄바꿈을 위해 `word-break: keep-all; overflow-wrap: anywhere`는 유지한다.

## 7. State Management

### 7.1 데이터 흐름
- **원천은 빌드 시점(서버 측)이다.** 모든 화면 데이터는 `Newsletter`(오늘)와 `site/archive/*.html`(지난 날짜)에서 나와 HTML에 박힌다. 클라이언트는 데이터를 새로 만들지 않는다.
- 단방향 흐름: `Newsletter → (HomePage | FeedPage) → HTML`, `site/archive/*.html → DayDigest → CalendarPage → HTML + archive.json`
- 모든 view model은 불변 record다(기존 규칙).

### 7.2 클라이언트 상태(`app.js`, 저장소 없음)
| 상태 | 저장 위치 | 초기값 |
|---|---|---|
| 활성 카테고리 탭 | URL 해시(`#politics`) | 해시가 가리키는 탭, 없으면 첫 탭 |
| 표시 중인 달 | URL 해시(`#2026-10`) + 메모리 | 해시, 없으면 `latestMonthId` |
| 이전/다음 날짜 | `archive.json`에서 한 번 읽음 | 읽기 실패 시 링크를 숨기고 캘린더 링크만 남김 |
| 배경 선택 | `localStorage['dn-theme']` = `light` \| `dark` \| `system` | `system`(FR-7.2). 저장 불가 시 메모리 |
| 카운트업 완료 여부 | 메모리(요소별 1회) | 미실행. 화면에 들어올 때 한 번 실행 |

저장소는 배경 선택값 하나만 `localStorage`에 둔다(spec A5). 쿠키와 외부 요청은 쓰지 않는다.

## 8. Security Considerations

| 항목 | 대책 |
|---|---|
| XSS | 외부 텍스트(RSS 제목·요약·키워드)는 `th:text`/`th:attr`로만 출력한다(기존 규칙, NFR-8). 메타 태그 `content`도 Thymeleaf 속성 이스케이프를 쓴다. `DayDigestReader`가 읽은 값도 다시 이스케이프해서 출력한다. 기존 `<script>` 제목 테스트를 새 페이지 전체에 적용한다. |
| CSP | 새 페이지에 `<meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data:; connect-src 'self'; base-uri 'self'; form-action 'none'; object-src 'none'">`를 넣는다. 인라인 스크립트, 인라인 스타일, 인라인 이벤트 핸들러는 0개다(테스트로 강제). 배경 깜빡임 방지도 인라인 스크립트가 아니라 같은 출처의 `theme-init.js`로 한다(§5.2.1). |
| localStorage | 배경 선택값(`light`/`dark`/`system`)만 저장한다. 읽은 값은 세 값 중 하나일 때만 쓰고, 그 밖의 값은 `system`으로 취급한다. 개인정보는 저장하지 않는다. |
| 외부 링크 | 기사 원문 링크에 `rel="noopener noreferrer"`를 붙이고 `<meta name="referrer" content="strict-origin-when-cross-origin">`를 둔다. |
| 공급망(Tailwind CLI) | 버전을 고정하고, 내려받은 실행 파일은 플랫폼별 **SHA-256 검증 후** 실행한다. 불일치하면 빌드 실패. 해시는 `gradle/tailwind.properties`에 기록한다. |
| 공급망(21st.dev 코드) | 복사한 코드는 마크업과 클래스만 쓴다. 원본 JS는 실행하지 않는다. 라이선스를 기록한다. |
| 폰트·아이콘 라이선스 | Pretendard는 OFL-1.1이라 `LICENSE`를 함께 배포한다. Lucide는 ISC라 `design-sources.md`에 고지한다. |
| 인증·개인정보 | 해당 없음. 로그인, 폼, 쿠키, 분석 스크립트가 없다. |
| CI 권한 | 워크플로 권한은 바꾸지 않는다. 캐시 단계만 추가한다. |

## 9. Testing Strategy

### 9.1 자동(JUnit + jsoup, `./gradlew test`)

| 대상 | 검증 내용 | spec 연결 |
|---|---|---|
| `HomeRendererTest` | `<h1>`의 `aria-label`이 `EveryDay. News Feed`이고 두 줄 구조인지, 버튼이 정확히 2개(`[data-cta=primary]` `Today` → `today.html`, `[data-cta=secondary]` `Calendar` → `calendar.html`)이고 이 순서인지, `Latest · 날짜`와 `<time datetime>`, 키워드·통계·헤드라인 요소가 **없는지**, `aria-current` | FR-2.x, SC1 |
| `FeedRendererTest`(기존 HtmlRendererTest 확장) | 기존 검증 유지: 섹션 5개 순서, 모든 카드의 원문 URL, XSS, 언론사·시각·보도 수·RSS 요약·같은 사안, 빈 카테고리 문구. 추가 검증: 키워드 ≤10개, `[data-count-up]` 3개의 `data-value`와 HTML 안 최종 숫자가 같은지, `dn:*` 메타, 탭 ARIA(`aria-controls`/`aria-selected`), 순위 번호, 날짜별 페이지의 `data-day-nav`와 캘린더 링크, `today.html`/`archive/…` 상대 경로(base) | FR-3.x, SC4 |
| `CalendarRendererTest` | 일요일 시작 7열, 달 사이 빈 달 없음, 활성 칸 = 보관 파일(링크 있음), 비활성 칸은 `<a>` 아님, 최신 날 강조 클래스와 aria-label, 이전/다음 달 경계 null, 빈 보관 시 안내 문구, `archive.html` 리다이렉트(meta refresh + 링크), `archive.json` 내용과 순서 | FR-4.x, FR-1.3, SC2, SC3 |
| `DayDigestReaderTest` | 새 메타 태그 읽기, 옛 마크업 대체 읽기(지금 `site/archive/2026-10-31.html`을 fixture로 복사), 깨진 파일은 기본값 | §4.4 |
| `PageSafetyTest`(전 페이지 공통) | `lang="ko"`, viewport, `color-scheme=light dark`, `<head>`에서 `theme-init.js`가 `defer`·`async` 없이 스타일시트보다 먼저 나오는지, 배경 스위처 라디오 3개(`light`/`dark`/`system`)와 각 접근 가능한 이름, CSP 메타, `script:not([src])` 0개, `[style]` 0개, `[on*]` 속성 0개, 외부 `src`/`href`(자원) 0개, `th:` 잔여 속성 없음 | NFR-3, NFR-8 |
| `AssetBudgetTest` | classpath `static/app.css` + `static/app.js` + `static/theme-init.js`의 gzip 크기 합 ≤ 150KB(목표: CSS ≤ 30KB, `app.js` ≤ 6KB, `theme-init.js` ≤ 0.5KB). 폰트 CSS의 `font-display: swap`. Pretendard `LICENSE` 존재 | NFR-6, NFR-11 |
| `OutputPathsTest` / 파이프라인 통합 테스트 | 실행 후 `index.html`, `today.html`, `calendar.html`, `archive.html`, `archive/날짜.html`, `data/archive.json`, `assets/{app.css, app.js, theme-init.js, fonts/…, style.css, tabs.js}`가 모두 생기는지. 보관 정리 후 캘린더에 남은 날짜만 나오는지 | FR-6.x, SC10 |

테스트의 셀렉터는 Tailwind 클래스가 아니라 **의미 클래스나 data 속성**(`article.card`, `section.category`, `p.url a`, `[data-cta]`, `[data-calendar-month]`)에 건다. 그래서 디자인을 바꿔도 테스트가 깨지지 않는다. 템플릿에는 이 의미 클래스를 Tailwind 클래스와 함께 남긴다.

### 9.2 로컬 미리보기(디자인 반복용)
- `./gradlew previewSite`: 테스트 소스의 `PreviewSite` 메인이 `TestNewsletters` 샘플로 **두 달에 걸친 여러 날짜**와 빈 카테고리·XSS 제목을 포함해 `build/preview/site`를 만든다. 네트워크를 쓰지 않는다.
- `jwebserver -d "$PWD\build\preview\site" -p 8000`(PowerShell)으로 띄워 브라우저에서 확인한다. `jwebserver`는 절대 경로만 받으므로, `previewSite` 실행 결과에 나오는 명령을 그대로 써도 된다.
- CSS만 고칠 때는 `./gradlew buildCss --continuous` 또는 Tailwind CLI `--watch`를 쓴다.

### 9.3 수동 점검(`docs/feat/page-ui/qa-checklist.md`)
| 항목 | 방법 | spec |
|---|---|---|
| 첫 화면 CTA | DevTools 360×640에서 두 버튼이 스크롤 없이 보이는지 | SC1 |
| 가로 스크롤 | 360 / 768 / 1280px에서 세 화면 확인(`document.documentElement.scrollWidth <= innerWidth`) | SC5 |
| Lighthouse | 모바일 모드, 세 화면 각각 Performance ≥ 90, Accessibility ≥ 95, LCP ≤ 2.5s, CLS ≤ 0.1 | SC6, NFR-6 |
| JS 끄기 | DevTools에서 JavaScript 비활성화 후 모든 기사·날짜 링크로 이동되는지 | SC7 |
| 키보드 | Tab/Shift+Tab/화살표만으로 홈 → 뉴스피드(탭 전환) → 캘린더(월 이동) → 날짜별 페이지, 포커스 표시 확인 | SC8 |
| 대비 | White/Dark에서 DevTools 대비 검사, 강조색 텍스트 확인 | SC9 |
| 배경 전환 | 첫 방문(저장값 없음)이 기기 설정을 따르고 스위처에 System이 선택돼 있는지(DevTools `prefers-color-scheme` 에뮬레이션으로 라이트·다크 둘 다) / 다크 기기에서 White 선택 → 다른 페이지 이동·새로고침에서 유지되고 어두운 깜빡임이 없는지, 반대로 라이트 기기에서 Dark도 같은지(DevTools 네트워크 Slow 4G) / System 상태에서 기기 설정을 바꾸면 바로 따라가는지 / JS를 끄면 기기 설정을 따르고 스위처가 숨는지 / 키보드로 세 값 선택 | FR-7, SC12 |
| 모션 | `prefers-reduced-motion` 에뮬레이션에서 히어로 굵기·버튼 흐름·스포트라이트·카운트업·등장 효과가 꺼지는지, 카운트업이 1초 이하인지 | NFR-5 |
| 히어로 | 360px에서 `EveryDay.`와 `News Feed`가 각각 한 줄을 넘지 않는지, 마우스 굵기 효과, 터치 기기에서 효과가 꺼지는지 | FR-2.1, FR-2.4 |
| 스크린리더 | NVDA(Windows) 또는 VoiceOver로 탭, 캘린더 칸 aria-label 읽기 | NFR-3 |
| 브라우저 | Chrome, Edge, Firefox, Safari(iOS), Samsung Internet | NFR-9 |
| 전환 기간 | 옛 `archive/2026-10-29.html`이 옛 디자인으로 정상 표시되고, 상단 링크가 홈과 캘린더로 이어지는지 | §3.3, SC3 |

## 10. Deployment Plan

### 10.1 빌드 구성(`build.gradle`)

```groovy
// Tailwind standalone CLI: 버전·SHA-256은 gradle/tailwind.properties에 고정
def twProps = new Properties()
file('gradle/tailwind.properties').withInputStream { twProps.load(it) }
def twPlatform = /* os.name·os.arch → linux-x64 | windows-x64 | macos-arm64 | macos-x64 */
def twBinary = layout.buildDirectory.file("tools/tailwind/${twProps.version}/tailwindcss-${twPlatform}${isWindows ? '.exe' : ''}")

tasks.register('downloadTailwind') {
    outputs.file(twBinary)
    // -PtailwindBinary=경로 를 주면 내려받지 않고 그 파일을 쓴다(오프라인 개발용)
    doLast { /* GitHub Releases에서 내려받기 → SHA-256 비교(불일치 시 실패) → 실행 권한 */ }
}

tasks.register('buildCss', Exec) {
    dependsOn 'downloadTailwind'
    inputs.file('src/main/frontend/app.css')
    inputs.dir('src/main/resources/templates')
    inputs.file('src/main/resources/static/app.js')
    outputs.file(layout.buildDirectory.file('generated/css/static/app.css'))
    commandLine twBinary.get().asFile, '-i', 'src/main/frontend/app.css',
                '-o', layout.buildDirectory.file('generated/css/static/app.css').get().asFile, '--minify'
}

sourceSets.main.resources.srcDir(layout.buildDirectory.dir('generated/css'))
tasks.named('processResources') { dependsOn 'buildCss' }

tasks.register('previewSite', JavaExec) {          // §9.2
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dailynews.render.PreviewSite'
    args layout.buildDirectory.dir('preview').get().asFile
}
```

`src/main/frontend/app.css`의 머리말:
```css
@import "tailwindcss" source(none);
@source "../resources/templates";
@source "../resources/static/app.js";
@import "../resources/static/fonts/pretendard.css";   /* 또는 별도 <link> */
@theme { /* §6.4 토큰 */ }
```

- `app.css`는 생성물이라 저장소에 커밋하지 않는다(`build/` 아래). `site/assets/app.css`는 매일 실행 결과로 커밋된다.
- Pretendard woff2는 `src/main/resources/static/fonts/`에 커밋한다. `.gitattributes`에 `*.woff2 binary`를 추가한다.

### 10.2 CI 변경
- `ci.yml`, `daily-news.yml`의 Gradle 단계 앞에 Tailwind 실행 파일 캐시를 추가한다.
  ```yaml
  - uses: actions/cache@v4
    with:
      path: build/tools/tailwind
      key: tailwind-${{ runner.os }}-${{ hashFiles('gradle/tailwind.properties') }}
  ```
- 그 밖의 권한, 커밋, Pages 배포 단계는 바꾸지 않는다. `git add -A news site README.md`에 새 파일(`today.html`, `calendar.html`, `data/`, `assets/fonts/`)이 자동으로 포함된다.

### 10.3 배포 순서
1. `feat/page-ui`에서 구현하고, `./gradlew test`와 `./gradlew previewSite`로 수동 점검(§9.3)을 마친 뒤 PR을 연다. CI test가 통과해야 한다.
2. main에 병합한다. `site/`는 병합만으로 바뀌지 않는다(생성물이기 때문).
3. Actions에서 `daily-news`를 **workflow_dispatch로 1회 수동 실행**한다. 그날 뉴스레터가 새 디자인으로 다시 생성되고(같은 날짜 덮어쓰기, 기존 FR-7.4) Pages에 배포된다(SC10).
4. 배포된 사이트에서 §9.3의 Lighthouse, 전환 기간, 링크 점검을 다시 한다.
5. D+30 이후 옛 자원(`style.css`, `tabs.js`)과 옛 마크업 대체 읽기를 지우는 후속 PR을 낸다(§3.3).

### 10.4 환경 설정
- 새 Secret이나 변수는 없다. `sources.yml`의 `repositoryUrl`/`siteUrl`/`retention.siteArchiveDays`를 그대로 쓴다.
- 개발 환경: JDK 21. Tailwind 실행 파일은 첫 빌드에서 자동으로 받는다(인터넷 필요, 또는 `-PtailwindBinary`).

## 11. Dependencies

| 의존성 | 버전 | 라이선스 | 용도 | 비고 |
|---|---|---|---|---|
| Tailwind CSS standalone CLI | **4.3.4로 고정**(2026-11-03, T-001. linux-x64·windows-x64·macos-arm64·macos-x64 SHA-256을 `gradle/tailwind.properties`에 기록) | MIT | CSS 빌드 | 빌드 시에만 쓰고 런타임 의존성은 없다. |
| Pretendard Variable(woff2 동적 서브셋) | 1.3.9 | OFL-1.1 | 웹폰트 | `src/main/resources/static/fonts/`에 커밋, `LICENSE` 포함 |
| Lucide icons(SVG 복사) | 구현 시점 최신 | ISC | 아이콘 | 쓰는 아이콘만 인라인 |
| 21st.dev 컴포넌트(코드 이식) 9개 | 2026-11-02 확정(§6.2 표) | 구현 시 원본 코드에서 확인 | UI | 마크업·클래스만 옮긴다. 원본 런타임 의존성(framer-motion, Radix, react-day-picker 등)은 들이지 않는다. `design-sources.md`에 기록 |
| com.fasterxml.jackson.core:jackson-databind | 2.22.4(기존 yaml 모듈과 같은 버전) | Apache-2.0 | archive.json | 지금은 전이 의존성이다. 직접 쓰므로 `implementation`으로 명시한다. |
| org.thymeleaf:thymeleaf | 3.1.5.RELEASE(기존) | Apache-2.0 | 템플릿 | 변경 없음 |
| org.jsoup:jsoup | 1.23.2(기존) | MIT | DayDigestReader, 테스트 | 변경 없음 |
| JUnit 6.1.3 / AssertJ 3.27.7(기존) | — | EPL-2.0 / Apache-2.0 | 테스트 | 변경 없음 |
| actions/cache | v4 | MIT | CI 캐시 | 추가 |

새로 들이는 런타임 JS·CSS 라이브러리는 없다. 브라우저에 내려가는 코드는 직접 만든 `app.js`와 Tailwind가 생성한 `app.css`뿐이다.

## 12. spec 추적표

| spec | 반영 위치 |
|---|---|
| FR-1.1~1.5 페이지·이동·공통 머리/꼬리 | §3.2, §6.1, §6.2 공통, `Nav` |
| FR-2.x 홈(미니멀) | §4.2, §6.2 홈, §6.2.1 |
| FR-3.x 뉴스피드(키워드·통계 포함) | §4.3, §5.2 탭·day-nav·count-up·spotlight, §6.2 뉴스피드 |
| FR-4.x 캘린더 | §4.4, §4.5, §5.2 calendar, §6.2 캘린더 |
| FR-5.x 디자인 소스 | §3.4, §6.2 후보 표, §6.3, §6.4 |
| FR-6.x 파이프라인 연동 | §3.1, §3.3, §10 |
| FR-7.x 배경 선택 | §5.2 theme-switcher, §5.2.1, §6.4, §7.2, §8, §9 |
| NFR-1/2 반응형·터치 | §6.2(1/2열, 44px 칸), §9.3 |
| NFR-3/4 접근성·테마 | §5.2, §6.2 table/aria-label, §6.2.1, §6.4, §9 |
| NFR-5 모션 | §6.3-5, §7.2 |
| NFR-6 성능 | §2, §9.1 AssetBudgetTest, §9.3 Lighthouse |
| NFR-7 점진적 향상 | §5.2 `no-js`/`js` 규칙 |
| NFR-8 보안 | §8 |
| NFR-10/11 유지보수·한글 | §6.1, §6.4 |
