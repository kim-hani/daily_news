# 디자인 출처 (design-sources)

> spec FR-5.1, FR-5.3, SC11 · 작업: T-005 · 원본 코드 보관: [`sources/`](./sources/) (참고용, 빌드에 쓰지 않음)
> 최종 갱신: 2026-11-03 (T-025: 9개 컴포넌트 "바꾼 점" 기록 완료)

## 21st.dev 컴포넌트

magic MCP 무료 플랜은 원본 조회가 **하루 2회**다. 받지 못한 원본은 사용자가 21st.dev 페이지에서 코드를 복사해 `sources/`에 넣거나, 다음 날 조회 한도로 받는다.

| # | 요소 | 컴포넌트(demo id) | 작성자 | 원본 확보 | 라이선스 | 원본 의존성 | 쓰는 조각 | 바꾼 점 |
|---|---|---|---|---|---|---|---|---|
| 1 | 상단 내비게이션 | [navigation-menu-05](https://21st.dev/@shadcnui-blocks/components/navigation-menu-05) "Underlined Navigation Menu" (18169) | shadcnui-blocks | **확보** (`sources/navigation-menu-05.tsx`, 사용자 복사, 2026-11-03) | MIT (21st.dev 표기) | shadcn `navigation-menu`(Radix), `lucide-react`, `next/link` | `nav.html` | Radix NavigationMenu·next/link 대신 `<nav><ul><a>`. 항목은 로고(→홈)·Today·Calendar 3개, 드롭다운 없음. 밑줄은 원본 클래스(`before:scale-x-0` → hover·focus·`aria-[current=page]`에서 `scale-x-100`) 그대로. 360px에서는 아이콘·로고 글자를 숨기고 `DN` 마크만 둔다. 상단 고정 + 아래 테두리 + `backdrop-blur`. 본문 건너뛰기 링크 추가 |
| 2 | 배경 전환 | [theme-switcher-1](https://21st.dev/@ncdai/components/theme-switcher-1) (6609) | ncdai | **확보** (`sources/theme-switcher-1.tsx`, 사용자 복사, 2026-11-03) | 21st.dev 표기 unknown. 원 저장소 [ncdai/chanhdai.com](https://github.com/ncdai/chanhdai.com) MIT(`theme-switcher` 레지스트리 포함) | `motion/react`, `next-themes`, `lucide-react` | `nav.html` | `role=radio` 버튼 + next-themes 대신 `<fieldset hidden>` + `<legend>배경</legend>` + 진짜 라디오 3개(White/Dark/System, `aria-label`). 순서는 spec대로 White·Dark·System. motion `layoutId` 테두리 대신 `:has(:checked)::after` 테두리(scale+opacity 300ms). 칸 크기 32px → 44px(NFR-2). zinc 하드코딩 색 → `foreground`/`muted-foreground`/`border` 토큰. 상태·저장은 `theme-init.js`·`app.js`(localStorage `dn-theme`) |
| 3 | 히어로 텍스트 | [m-variable-font-hover-2](https://21st.dev/@cnippet-dev/components/m-variable-font-hover-2) "Variable Font Hover Hero" (18647) | cnippet-dev | **확보** (`sources/m-variable-font-hover-2.tsx`, 2026-11-02) | MIT (21st.dev 표기) | `@/components/ui/variable-font-hover`(받은 코드에 없음), framer-motion 계열 spring | `hero.html` | `VariableFontHover`(framer-motion) 대신 글자별 `<span>` + CSS transition. 원본 값(1줄 wght 100→900·slnt 0→-10, 2줄 반대, 20ms 지연)은 유지하고 `slnt`는 `skewX(-10deg)`로 대신. 전환 500ms → 400ms(NFR-5), spring은 `cubic-bezier(0.34,1.56,0.64,1)`. 지연 순서 `--i`만 app.js가 CSSOM으로 넣는다. 터치 기기는 탭하면 1.2초 재생, reduced-motion이면 바뀌지 않음. "Hover the headline" 문구 삭제, `<h1 lang="en" aria-label>` + 글자 `aria-hidden`, 크기 `text-5xl` → `clamp(3rem,14vw,9rem)` |
| 4 | Today / Calendar 버튼 | [flow-button](https://21st.dev/@xubohuah/components/flow-button) "FlowButton" (1929) | xubohuah | **확보** (`sources/flow-button.tsx`, `flow-button.demo.tsx`, magic MCP, 2026-11-03) | MIT (21st.dev 표기) | `lucide-react`(ArrowRight)만. 나머지는 Tailwind 클래스 | `buttons.html` | `<button>` → `<a data-cta>`. primary(채움: `primary` 배경, hover 시 `accent` 원)·secondary(원본 외곽선, hover 시 `primary` 원) 두 변형. `#111`·`#333` → 토큰(Dark에서 반전). 600~800ms → 300~400ms(비율 유지), focus-visible에도 같은 효과. 높이 44px(`min-height: 2.75rem`) |
| 5 | 오늘의 키워드 | [base-badge](https://21st.dev/@sean0205/components/base-badge) (8145) | sean0205 | **확보** (`sources/base-badge.tsx`, 사용자 복사, 2026-11-03) | MIT (21st.dev 표기) | `@base-ui-components/react`(`useRender`, `mergeProps`), `class-variance-authority` | `badges.html` | `useRender`·`mergeProps`·cva 대신 정적 클래스. 키워드 칩은 secondary·lg·circle 조합(`bg-muted`, `h-7`, `rounded-full`), 메타 배지는 outline·sm 조합(`border-border`, `h-6`, 11px). "N개 언론사 보도"만 `accent` 외곽선. `ul.chips li`·`.badge.source/outlets` 의미 클래스 유지 |
| 6 | 통계 숫자 | [counting-number](https://21st.dev/@cnippet-dev/components/counting-number) (19065) | cnippet-dev | **확보** (`sources/counting-number.tsx`, 사용자 복사, 2026-11-03) | 21st.dev 표기 no-license. 원 저장소 [cnippet-dev/ui-cnippet](https://github.com/cnippet-dev/ui-cnippet) MIT(`m-counting-number-*` 포함, 같은 파일인지는 미확인) | `motion/react` | `stats.html` | motion `animate` 대신 `requestAnimationFrame` + IntersectionObserver. 3초 easeInOut → 0.9초 ease-out 한 번(FR-3.9, NFR-5). HTML에는 처음부터 최종 숫자(`1,100`)와 `data-value`가 있고, JS가 0으로 바꿨다가 센다. reduced-motion이면 세지 않는다. 숫자 묶음은 `aria-hidden`, 같은 내용을 `statsLine` 문장으로 둔다 |
| 7 | 카테고리 전환 | [animated-navigation-tabs](https://21st.dev/@ln-dev7/components/animated-navigation-tabs) (575) | ln-dev7 | **확보** (`sources/animated-navigation-tabs.tsx`, 사용자 복사, 2026-11-03) | MIT (21st.dev 표기) | `motion/react`(`layoutId`) | `tabs.html` | 기존 `role=tablist/tab/tabpanel` + `<a href="#slug">` 구조에 겉모양만 입혔다(원본의 `ul > button`은 쓰지 않음). `active` 막대는 `[data-tabs-indicator]` + `--indicator-x/-w`(CSSOM) + 350ms spring transition, `hover` 막대·`hover-bg`는 탭마다 `::after`·`::before`로 제자리에 나타난다. JS가 없으면 선택된 탭에 정적 밑줄. 탭에 이모지·기사 수 배지 추가, sticky(내비게이션 아래), 가로 넘침은 탭 줄 안에서만 스크롤 |
| 8 | 기사 카드 | [spotlight-card](https://21st.dev/@preetsuthar17/components/spotlight-card) (2220) | preetsuthar17 | **확보** (`sources/spotlight-card.tsx`, `spotlight-card.demo.tsx`, magic MCP, 2026-11-03) | 21st.dev 표기 unknown. 라이브러리 HextaUI 저장소 [preetsuthar17/HextaUI](https://github.com/preetsuthar17/HextaUI) MIT(현재 저장소에 이 파일은 없음) | 없음(React state만). 데모는 `react-icons` | `card.html` | React state 대신 `.spotlight::before`의 `radial-gradient(… at var(--x) var(--y))`. app.js가 마우스 기기에서만 document `pointermove` 하나로 좌표를 넘긴다. 색 `#6300ff30` → `spotlight` 토큰(흑 7%/백 8%), 불투명도 0.6·500ms → 1·300ms, 카드 `neutral-900` → `card` 토큰 + `border`, 반경 `rounded-3xl` → `1rem`. 키보드 포커스(`:focus-within`)에도 켜짐, reduced-motion이면 끔. 사진 없음(spec C5) |
| 9 | 월 달력 | [new-calendar](https://21st.dev/@edwinvakayil/components/new-calendar) "New Calendar" (13635) | edwinvakayil(라이브러리 Dali Agents는 `@lyanchouss` 소유. 옛 `@lyanchouss` 경로는 404) | **확보** (`sources/new-calendar.tsx` 1394줄, `new-calendar.demo.tsx`, 2026-11-02) | 21st.dev 표기 unknown. 작성자 저장소 [edwinvakayil/iconiq](https://github.com/edwinvakayil/iconiq) MIT의 `registry/calendar.tsx`와 비슷하나 내용이 다름(2119줄 vs 1393줄) | `date-fns` 4.1.0, `motion/react`, `lucide-react` | `calendar.html` | 자체 달력(1393줄)의 겉모양만 옮겼다. 인라인 스타일·`.dark` 감지 대신 토큰(`--calendar-from/to`, `--calendar-latest-*`)과 클래스. 원본 "선택 칸" 그라디언트는 최신 날 칸에, "오늘 점"은 뉴스레터 있는 날 점에 썼다. 날짜 선택 상태·연월 팝오버·월 이동 애니메이션 코드는 버리고 `<table>`+`<caption>`+`<th scope=col abbr>`로 다시 짰다. 활성 칸은 `<a aria-label>`, 없는 날은 포커스 안 되는 글자. 월 전환은 app.js + 300ms spring keyframe. 640px 이상에서 대표 키워드·선정 수, hover/focus 시 키워드 3개 팝오버 |

### 라이선스 확인 (2026-11-03)

- 21st.dev 컴포넌트 페이지의 `License` 값(`https://21st.dev/@작성자/components/이름.md`)과 원 저장소의 LICENSE를 확인했다.
- MIT 표기 5개(#1, #3, #4, #5, #7)는 그대로 쓸 수 있다. MIT는 저작권·허가 고지를 함께 두어야 하므로, 이식한 조각에 출처 주석을 달고 배포물에 고지 파일을 둔다.
- 21st.dev 표기가 unknown/no-license인 4개(#2, #6, #8, #9)는 작성자의 원 저장소가 MIT라 MIT로 보는 것이 합리적이다. 다만 #6, #8, #9는 받은 코드와 똑같은 파일을 저장소에서 찾지 못했다. 확실히 하려면 작성자에게 묻거나, 원본 코드를 옮기지 말고 동작(설정값)만 참고해 새로 짠다.
- 21st.dev 이용약관: 데모 페이지·미리보기·스크린샷·영상은 21st Labs 소유이고, 컴포넌트 코드는 작성자가 정한 라이선스를 따른다. 우리는 코드(마크업·클래스)만 옮기고 데모 미디어는 쓰지 않는다.

### 원본을 받고 확인한 점

**#3 히어로(m-variable-font-hover-2)**
- 두 줄 각각이 `VariableFontHover`이고, **줄에 마우스를 올리면** 글자가 차례로 바뀐다. 포인터 거리에 따라 바뀌는 방식이 아니다. plan §6.2.1을 이에 맞게 고쳤다.
  - 1줄: `'wght' 100, 'slnt' 0` → `'wght' 900, 'slnt' -10`, 첫 글자부터 0.02초 간격
  - 2줄: `'wght' 900, 'slnt' -10` → `'wght' 100, 'slnt' 0`, 마지막 글자부터 0.02초 간격, `text-muted-foreground`
  - 전환: spring, 0.5초. 가운데 정렬, `text-5xl tracking-tight`
- Pretendard Variable에는 `slnt`(기울기) 축이 없다. 기울기는 글자별 `transform: skewX()`로 대신한다.
- 위 "Hover the headline" 안내 문구는 쓰지 않는다(사용자 지시: 두 줄만).
- 애니메이션 본체(`variable-font-hover`)는 받은 코드에 없지만, 위 설정값만으로 CSS transition + 글자별 지연으로 같은 동작을 만들 수 있다. 원본을 따로 받을 필요는 없다.

**#4 버튼(flow-button)**
- 순수 CSS 전환으로 된 버튼이다. JS 없이 그대로 옮길 수 있다.
  - 기본: 알약 모양(`border-radius: 100px`), `1.5px` 테두리 `#333`/40%, 투명 배경, 오른쪽에 화살표
  - hover: 가운데 점(`16px`)이 `220px` 원으로 커지며 배경을 채움(`800ms cubic-bezier(0.19,1,0.22,1)`), 글자 흰색, 모서리 `12px`로 바뀜(`600ms cubic-bezier(0.23,1,0.32,1)`)
  - 화살표: 오른쪽 화살표가 밖으로 나가고 왼쪽 화살표가 들어옴(`800ms cubic-bezier(0.34,1.56,0.64,1)`), 글자는 `-12px → +12px` 이동
  - active: `scale(0.95)`
- 색이 `#111`/흰색으로 고정돼 있어 Dark 테마에서는 반전 토큰으로 바꿔야 한다. 원본 전환 시간 600~800ms는 spec NFR-5의 400ms 상한을 넘으므로 비율을 유지한 채 400ms 이하로 줄인다. 반복 흐름 효과는 없다(hover·focus 때만 동작).

**#7 카테고리 전환(animated-navigation-tabs)**
- 탭마다 `motion.div` 세 개를 `layoutId`로 옮긴다.
  - `active`: 활성 탭 아래 `2px` 막대(`bg-primary`). 클릭하면 새 탭으로 미끄러져 간다.
  - `hover`: 마우스가 올라간 탭 아래 `2px` 막대. 탭 사이를 옮겨 다닌다.
  - `hover-bg`: 마우스가 올라간 탭 안쪽 배경(`bg-primary/10`, `border-radius: 6px`)
  - 글자색: 활성 `text-primary`, 나머지 `text-muted-foreground`, hover 시 `text-primary`(300ms)
  - 탭 안쪽 여백 `px-5 py-2`
- 전환 값은 motion 기본 spring이다. CSS transition(400ms 이하)과 spring ease `cubic-bezier(0.34, 1.56, 0.64, 1)`로 바꾼다.
- plan §5.2의 `[data-tabs-indicator]`는 `active` 막대만 다룬다. `hover` 막대와 `hover-bg`는 JS 없이 탭마다 `:hover`/`:focus-visible` 가상 요소로 만든다(탭 사이를 미끄러지는 효과는 빼고 제자리에서 나타나게 한다).
- 원본은 `<ul>` 바로 아래에 `<button>`을 두고, `items` 타입을 `Props`(배열 아님)로 적어 두었다. 이식할 때는 기존 `role=tablist/tab` + `<a href="#slug">` 구조를 쓰므로 이 부분은 그대로 가져오지 않는다. 원본에는 키보드 조작·ARIA가 없다.

**#8 기사 카드(spotlight-card)**
- 카드 위에 `radial-gradient(circle at x y, 색, transparent 80%)` 층을 얹고, `mousemove`로 좌표를, `mouseenter/leave`로 불투명도(0 ↔ 0.6, 500ms)를 바꾼다. 500ms는 spec NFR-5에 맞춰 400ms 이하로 줄인다.
- 이식할 때는 좌표를 CSS 변수(`--x`, `--y`)로 넘기는 짧은 JS 한 개로 충분하다. 키보드 포커스 때는 좌표를 고정하고 불투명도만 올린다(원본 동작 그대로).
- 기본 색 `#6300ff30`(보라 19%)과 어두운 카드 배경은 우리 흑백 팔레트와 맞지 않으므로 토큰 색으로 바꾼다. `prefers-reduced-motion`에서는 효과를 끈다.

**#9 달력(new-calendar)**
- 자체 구현 달력이다(react-day-picker를 쓰지 않음. 함께 딸려 온 `button`·`card`·`calendar` 레지스트리 파일은 쓰지 않는다).
- 색은 흑백 팔레트이고, 원본이 `<html>`의 `.dark` 클래스와 `prefers-color-scheme`를 직접 감지해 인라인 스타일로 칠한다. 이식할 때는 아래 값을 토큰·클래스로 옮기고 인라인 스타일은 쓰지 않는다(plan §6.3-8).

  | 원본 값 | White | Dark | 옮길 곳 |
  |---|---|---|---|
  | 카드 배경 | `linear-gradient(135deg, #fff, #f4f4f5)` | `linear-gradient(135deg, #000, #0f0f0f)` | 달력 카드 |
  | 카드 테두리 | `rgba(0,0,0,.08)` | `rgba(255,255,255,.12)` | `border` 계열 |
  | 보조 글자 | `#737373` | `#a3a3a3` | `muted-foreground`(대비 4.5:1 확인 필요) |
  | 칸 hover 배경 | `#f4f4f5` | `#1a1a1a` | 활성 칸 hover |
  | 선택 칸 | `linear-gradient(135deg, #141414, #4a4a4a)` / 흰 글자 | `linear-gradient(135deg, #f5f5f5, #a3a3a3)` / 검은 글자 | **최신 날짜(오늘) 칸** 강조 |
  | 오늘 점 | `#0a0a0a` | `#f5f5f5` | 뉴스레터 있는 날 점 표시 |
- 월 이동 애니메이션은 spring ease `cubic-bezier(0.34, 1.56, 0.64, 1)`을 쓴다. CSS transition에 같은 값을 쓴다(400ms 이하).
- 원본의 연·월 선택 팝오버, 날짜 선택 상태는 쓰지 않는다. 달력은 `<table>`과 링크로 다시 짠다(plan §6.2 캘린더).

## 디자인 토큰 대비(T-004, SC9)

`src/main/frontend/app.css` 토큰으로 계산했다. `ThemeTokensTest`가 같은 계산으로 4.5:1 미만이면 실패한다.

| 글자 / 배경 | White | Dark |
|---|---|---|
| `foreground` / `background` | 19.80:1 | 19.06:1 |
| `muted-foreground` / `background` | 7.73:1 | 7.76:1 |
| `muted-foreground` / `muted` | 7.03:1 | 5.81:1 |
| `muted-foreground` / `card` | 7.73:1 | 6.91:1 |
| `primary-foreground` / `primary` | 18.97:1 | 18.97:1 |
| `accent` / `background` | 4.70:1 | 7.39:1 |
| `accent` / `card` | 4.70:1 | 6.58:1 |
| `accent-foreground` / `accent` | 4.70:1 | 7.36:1 |
| `calendar-latest-foreground` / `calendar-latest-to` | 8.86:1 | 7.85:1 |

- plan §6.4에 없던 `accent-foreground`(White `#FFFFFF`, Dark `#0A0A0A`)를 더했다. `primary-foreground`(`#FAFAFA`)를 강조색 위에 쓰면 White에서 4.49:1로 기준에 조금 못 미치기 때문이다(Today 버튼 hover).

## 폰트·아이콘

| 이름 | 버전 | 라이선스 | 고지 |
|---|---|---|---|
| Pretendard Variable | 1.3.9 | OFL-1.1 | npm `pretendard@1.3.9` tarball(sha512 무결성 확인)의 `dist/web/variable/woff2-dynamic-subset` 92개 파일. `static/fonts/LICENSE` 함께 배포(T-003) |
| Lucide icons | 1.67.0 | ISC | 쓰는 9개(arrow-right, newspaper, calendar, chevron-left/right, external-link, sun, moon-star, monitor)만 `templates/fragments/icons.html`에 인라인 SVG로 복사(T-008). Copyright (c) 2026 Lucide Icons and Contributors |
