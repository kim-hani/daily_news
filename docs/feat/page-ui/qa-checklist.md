# 수동 점검 체크리스트 (qa-checklist)

> plan §9.3 · 작업: T-025(미리보기), T-027(배포 후) · 대상: `./gradlew previewSite` → `jwebserver -d "$PWD\build\preview\site" -p 8000`(PowerShell, 절대 경로 필요)
> 점검 환경: Windows 11, Playwright(Chromium) · 점검일: 2026-11-03
> 미리보기 데이터: 2026-10-15 ~ 11-02(10-18·10-25 없음), 10-14는 옛 디자인 페이지, IT 카테고리 빈 상태, `<script>` 제목, 홀수 날 수집 실패 피드

## 결과 요약

| # | 항목 | spec | 미리보기(T-025) | 배포 후(T-027) |
|---|---|---|---|---|
| 1 | 첫 화면 CTA | SC1 | 통과 | 대기 |
| 2 | 가로 스크롤 | SC5, NFR-1 | 통과 | 대기 |
| 3 | Lighthouse | SC6, NFR-6 | 접근성 100 · 성능 일부 미달 → CLS 고침(아래 참고) | 대기 |
| 4 | JS 끄기 | SC7, NFR-7 | 통과 | 대기 |
| 5 | 키보드 | SC8 | 통과(자동화로 확인한 범위) | 대기 |
| 6 | 대비 | SC9 | 통과(계산) | 대기 |
| 7 | 배경 전환 | FR-7, SC12 | 통과 | 대기 |
| 8 | 모션 | NFR-5 | 통과 | 대기 |
| 9 | 히어로 | FR-2.1, FR-2.4 | 통과 | 대기 |
| 10 | 스크린리더 | NFR-3 | **미실시**(사람이 확인) | 대기 |
| 11 | 브라우저 | NFR-9 | Chromium만 | 대기 |
| 12 | 전환 기간 | §3.3, SC3 | 통과 | 대기 |
| 13 | 터치 대상 44px | NFR-2 | 통과(수정 후) | 대기 |
| 14 | 디자인 출처 기록 | SC11 | 통과 | — |

## 항목별 기록

### 1. 첫 화면 CTA (SC1)
- 360×640: 히어로 두 줄, `Today`·`Calendar` 버튼, `Latest · 2026-11-02 (월)`가 스크롤 없이 보인다. 두 번째 버튼 아래 끝 y=347px.
- `Today` → `today.html`, `Calendar` → `calendar.html`(HomeRendererTest로도 검증).

### 2. 가로 스크롤 (SC5)
- `index`, `today`, `calendar`, `archive/2026-10-20`, `archive/2026-11-02` × 360/768/1280px 모두 `scrollWidth == clientWidth`.
- 카테고리 탭은 탭 줄 안에서만 가로로 스크롤된다.

### 3. Lighthouse (SC6) — 1차 측정 일부 미달 → CLS 고친 뒤 재측정 통과, LCP는 배포 후 확인
- Chrome DevTools Lighthouse(모바일, 시크릿 창), `jwebserver` 미리보기. 기준: Performance ≥ 90, Accessibility ≥ 95, LCP ≤ 2.5초, CLS ≤ 0.1.

  | 화면 | Performance | Accessibility | FCP | LCP | CLS |
  |---|---|---|---|---|---|
  | `/` | 98 | 100 | 1.8초 | 2.0초 | 0.014 |
  | `/today.html` | **89** | 100 | 2.9초 | **3.1초** | 0.008 |
  | `/calendar.html` | **88** | 100 | 2.2초 | 2.3초 | **0.172** |

- **CLS 원인과 수정**(Playwright로 재현: 412×823, 웹폰트 800ms·app.js 400ms 지연)
  - 캘린더: JS가 실행되기 전에는 두 달이 쌓여 그려지고, app.js가 한 달을 접으면 아래 내용이 밀렸다. → `.js` 클래스(theme-init.js가 첫 화면 전에 붙인다)로 CSS가 처음부터 최신 달(`calendar-month-latest`) 하나만 그린다. 월 이동 등장 효과는 사용자가 넘길 때만 준다(`is-navigated`).
  - 모든 화면: 배경 전환 버튼이 app.js 실행 뒤 `hidden`이 풀리면서 메뉴 링크를 밀었다. → `hidden` 대신 `.no-js .js-only { display: none }`. 월 이동 막대도 같은 방식으로 바꿨다.
  - 웹폰트 교체: 맑은 고딕의 한글이 Pretendard보다 약 17% 넓어 폰트가 바뀔 때 줄바꿈이 달라졌다(캘린더 안내 문장의 링크가 다른 줄로 이동). → 한글 범위에만 `size-adjust: 85%`를 준 대체 글꼴 `Pretendard Fallback`(local 맑은 고딕)을 둔다. 실측 폭 비율은 0.844~0.852배였다.
  - 고친 뒤 360/412/1280px × 홈·뉴스피드·캘린더·날짜별: CLS 0.000~0.013.
- **LCP·렌더링 차단(today 3.1초)**: `jwebserver`는 gzip 압축과 캐시 헤더가 없어서, 첫 화면에 필요한 파일이 압축 없이 전송된다(today.html 29KB→gzip 4.7KB, app.css 33KB→7.5KB, pretendard.css 54KB→13.4KB). "효율적인 캐시 수명" 항목도 같은 이유다. GitHub Pages는 gzip으로 보내므로 **배포 후(T-027)에 다시 측정**해서 판단한다. 그래도 미달하면 pretendard.css를 렌더링 차단에서 빼는 방법을 검토한다.
- 고친 뒤 사용자가 `/calendar.html`을 다시 측정해 CLS 기준 통과를 확인했다. 월 이동도 정상 동작한다.

### 4. JS 끄기 (SC7)
- 스크립트 없는 iframe(`sandbox="allow-same-origin"`)으로 확인했다.
  - 뉴스피드: 탭 패널 5개와 카드 5개가 모두 보이고, 선택된 탭에 정적 밑줄이 있다. 배경 전환 버튼은 숨는다.
  - 캘린더: 두 달이 쌓여 보이고 날짜 링크 18개(옛 디자인 1개 포함)가 모두 보인다. 월 이동 버튼은 숨는다.
  - 날짜별 뉴스피드: "캘린더로 돌아가기"만 보이고 이전/다음은 숨는다.
  - 다크 기기에서는 JS 없이도 어두운 배경(`#09090B`)이 된다(FR-7.7).

### 5. 키보드 (SC8)
- 탭: ←/→/Home/End로 선택과 포커스가 옮겨지고 패널이 바뀐다. 클릭하면 해시(`#economy`)가 바뀌고, 해시로 바로 열린다.
- 캘린더: 이전/다음 달 버튼으로 이동한다. 경계 달에 닿아 버튼이 꺼지면 포커스가 반대 버튼으로 옮겨진다(점검 중 발견해 고침).
- 모든 링크·버튼에 `:focus-visible` 외곽선 규칙이 있다(app.css). 본문 건너뛰기 링크가 DOM 순서상 첫 포커스 대상이다.
- **남은 확인**: 실제 키보드로 홈 → 뉴스피드 → 캘린더 → 날짜별 페이지를 한 번에 이동해 보기.

### 6. 대비 (SC9)
- 토큰 조합 계산 결과는 [design-sources.md](./design-sources.md#디자인-토큰-대비t-004-sc9). 가장 낮은 값은 White의 `accent` 글자 4.70:1.
- 점검 중 Today 버튼 hover(강조색 위 흰 글자)가 4.49:1인 것을 찾아 `accent-foreground` 토큰을 더해 고쳤다.

### 7. 배경 전환 (FR-7, SC12)
- 저장값 없음 → System이 선택돼 있고 기기 설정을 따른다(다크 기기 → `dark` 클래스).
- 다크 기기에서 White 저장 → 첫 화면부터 밝은 배경(`theme-init.js`가 CSS보다 먼저 실행), 스위처에 White 선택.
- Dark 선택 → 다른 페이지(캘린더)로 가도 Dark 유지.
- `dn-theme=abc` → System처럼 동작.
- System 상태에서 기기 설정을 다크 → 라이트로 바꾸면 새로고침 없이 흰 배경이 된다.
- 저장소를 쓸 수 없는 환경(opaque origin iframe)에서 스크립트 오류가 없다.
- **남은 확인**: 느린 네트워크(Slow 4G)에서 깜빡임이 없는지 눈으로 보기.

### 8. 모션 (NFR-5)
- `prefers-reduced-motion: reduce`: 카운트업이 돌지 않고 최종 숫자(1,212·491·5)가 바로 보인다. 등장 효과 대기 상태가 생기지 않는다. 스포트라이트가 꺼진다(`display: none`). transition 길이가 0.01ms가 된다.
- 카운트업은 0.9초 한 번. 그 밖의 transition은 400ms 이하.

### 9. 히어로 (FR-2.1, FR-2.4)
- 360px에서 `EveryDay.`가 hover 상태(wght 900·기울기)로도 256px로 한 줄에 들어간다.
- 1280px에서 1줄 hover 시 글자가 차례로 굵어지고 기울어진다. 2줄은 반대.
- 버튼은 hover·focus 시 가운데 원이 커지며 채워지고 화살표가 바뀐다.

### 10. 스크린리더 — 미실시
- 사람이 NVDA(Windows) 또는 VoiceOver로 확인한다: 탭 이름(예: "정치 2건"), 캘린더 칸 이름(예: "2026년 10월 31일 토요일, 최신 뉴스레터, 선정 5건, 키워드 …"), 월 이동 시 제목 읽기(`aria-live`).

### 11. 브라우저 — Chromium만
- 남은 확인: Edge, Firefox, Safari(iOS), Samsung Internet.

### 12. 전환 기간 (§3.3, SC3)
- 2026-11-04 변경(T-031): 옛 디자인 페이지는 그날 Markdown이 있으면 새 디자인으로 다시 만든다. 배포 데이터 사본으로 10-29~11-02 5개 페이지가 모두 바뀌고, 기사 25건·통계·키워드·이전/다음 날짜 이동이 나오는 것을 확인했다. 아래는 Markdown이 없는 옛 페이지의 경우다.
- 옛 디자인 `archive/2026-10-14.html`이 `assets/style.css`·`tabs.js`로 그대로 열린다. 캘린더는 이 날의 키워드·선정 수(25건)를 읽는다(PipelineIntegrationTest, CalendarRendererTest).
- `archive.html`은 `calendar.html`로 리다이렉트한다.

### 13. 터치 대상 44px (NFR-2)
- 360px에서 로고 링크가 28px 폭, 캘린더 칸이 40px 폭이라 고쳤다. 고친 뒤 로고 44×44px, 캘린더 칸 최소 45px(스크롤바 있는 345px 폭 기준).
- 기사 제목·원문 URL 같은 문장 속 링크는 대상에서 뺐다(WCAG 2.5.8 inline 예외).

### 14. 디자인 출처 (SC11)
- `design-sources.md`에 9개 컴포넌트의 출처·라이선스·"바꾼 점"이 모두 있다. Pretendard·Lucide 고지도 있다.

## 배포 후 점검(T-027) — 2026-11-03
- 배포: PR #9 병합(`331bbdc`) → `daily-news` workflow_dispatch(run 41656108654) 성공, `3ef2a80 news: 2026-11-03 daily newsletter`. 폰트 92개가 `site/assets/fonts/`로 커밋됐다.
- `/`, `/today.html`, `/calendar.html`, `/archive/2026-11-03.html`: 200, 새 디자인, Pretendard 적용, 412px 가로 스크롤 없음, 콘솔 오류 없음.
- `/archive.html` → `/calendar.html` 리다이렉트. 캘린더 날짜 링크 6개(10-29~11-03) 모두 열리고, 최신 칸을 누르면 `archive/2026-11-03.html`로 간다. 날짜별 페이지의 이전 날짜 링크(archive.json)도 나온다.
- 옛 디자인 `archive/2026-10-29~11-02.html`: `style.css`·`tabs.js`로 그대로 열린다. 이 페이지들은 아이콘 링크가 없어 브라우저가 `/favicon.ico`를 찾다 404를 낸다(기존과 같고 T-030 뒤 사라짐).
- GitHub Pages가 HTML·CSS·JS를 gzip으로 보낸다(today.html 11.7KB, app.css 7.6KB, pretendard.css 13.6KB 전송).
- Tailwind 캐시: main의 첫 실행이라 캐시가 없어 다시 받았고 저장했다. 다음 실행부터 적중한다.
- 2026-11-04 T-031 배포 후: 옛 디자인 페이지 5개(10-29~11-02)가 새 디자인으로 바뀌어 `/favicon.ico` 404도 사라졌다. 캘린더 → 10-29 이동, 모든 날짜의 이전/다음 이동 확인.
- **남은 것**: 배포 사이트 Lighthouse(모바일) 재측정, 키보드 일주, Slow 4G 깜빡임, 스크린리더, 다른 브라우저(휴대폰 포함).
