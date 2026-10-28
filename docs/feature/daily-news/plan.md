# Daily News — 구현 계획 (Plan)

> 기준 명세: [`spec.md`](./spec.md) · 브랜치: `feature/daily-news` · 작성일: 2026-10-28

## 1. Original Tech Stack Request
- 사용자 입력(원문): **"뉴스 커밋은 바로 main으로 넣어줘"**
- AskUserQuestion으로 정한 항목:
  | 영역 | 선택 |
  |---|---|
  | Java 버전 | **JDK 21** |
  | 빌드 도구 | **Gradle Groovy DSL** |
  | HTML 생성 | **Thymeleaf 단독 사용**(Spring 없이) |
  | 오늘의 키워드 | **KOMORAN**(약 6MB, Gradle 캐시에 저장되고 git에는 포함되지 않음) |
  | 서울신문 | **제외**. robots.txt가 RSS 경로 `/xml/`을 `Disallow`로 막고 있어 NFR-3에 맞지 않는다 |
- 명세에서 정해진 것: 언어는 Java(C1), 비용 0원(C2), GitHub Actions + GitHub Pages(C3)

### spec 대비 변경·확정 사항
| 항목 | spec | plan 결정 | 이유 |
|---|---|---|---|
| FR-8.1 뉴스 커밋 방식 | (a)/(b) 미정 | **(a) main에 직접 push** | 사용자 지시 |
| 언론사 | 4곳 | **3곳**(연합·동아·경향) | 서울신문 robots.txt `/xml/` Disallow |
| FR-7.2, FR-8.4 웹 출력 위치 | `docs/` | **`site/`** + Pages "GitHub Actions" 배포 | `docs/`에는 SDD 문서(spec/plan)가 있어서, 섞이면 문서까지 웹에 공개된다 |
| NFR-5 비밀값 | 없음 | **배포 키 1개**(`NEWS_DEPLOY_KEY`) | main 보호 규칙을 일일 커밋만 우회하는 데 필요하다(§8) |
| FR-7.5 보관 정책(신규) | — | **웹 날짜별 페이지 30일, Markdown 영구** | 사용자 결정. 공개 페이지에 오래된 인용이 남지 않게 하고, 기록은 Markdown으로 보존한다 |

→ `spec.md`의 해당 항목도 같이 갱신한다.

---

## 2. Technology Stack
| 구분 | 선택 | 용도 |
|---|---|---|
| 언어/런타임 | Java 21 (Temurin) | record, text block, `java.net.http.HttpClient`, `java.time` |
| 빌드 | Gradle 9.8.2 (Wrapper), Groovy DSL, `application` 플러그인, Java toolchain 21 | 빌드, 실행(`./gradlew run`), 테스트 |
| RSS 파싱 | Rome 2.1.0 | RSS 2.0 + `dc:date` 모듈 |
| HTML 파싱 | jsoup 1.23.2 | 기사 본문 추출, RSS 설명문 HTML 제거 |
| 템플릿 | Thymeleaf 3.1.5 (standalone `TemplateEngine`) | `index.html`, 날짜별 페이지, 아카이브 페이지 |
| 형태소 분석 | KOMORAN 3.3.9 (JitPack) | 제목 명사 추출(키워드·클러스터링 보조) |
| 설정 | Jackson YAML 2.22.4 | `sources.yml`을 record로 읽음 |
| 로깅 | SLF4J simple 2.0.20 | 실행 로그(NFR-8) |
| HTTP | JDK `HttpClient` | 의존성 없이 처리, 타임아웃·리다이렉트 지원 |
| 테스트 | JUnit 6.1.3, AssertJ 3.27.7 | 단위·통합 테스트 |
| CI/CD | GitHub Actions(`schedule`, `workflow_dispatch`, `pull_request`) | 일일 실행, PR 검증 |
| 호스팅 | GitHub Pages(Actions 배포: `upload-pages-artifact` + `deploy-pages`) | 웹페이지 공개 |

---

## 3. Architecture Overview
단일 JVM 배치 프로그램이다. 하루 1회 실행되고, 서버도 DB도 없다. 상태는 git 저장소의 파일로만 남는다.

```
GitHub Actions (cron 0 0 * * * = 09:00 KST)
        │
        ▼
┌──────────────────────────── DailyNewsApp (main) ────────────────────────────┐
│ 1 ConfigLoader      sources.yml → AppConfig                                  │
│ 2 FeedCollector     피드 15개 순차 수집(호스트별 지연, 실패 피드 skip) → List<Article>
│ 3 ArticleCleaner    URL 정규화(utm 제거)·제목 정규화·24h 필터·중복 제거       │
│ 4 CategoryFilter    연합 industry → IT 키워드 매칭만 IT로                     │
│ 5 StoryClusterer    카테고리별 제목 유사도 클러스터 → List<Story>             │
│ 6 StoryRanker       점수 = 언론사 수×10 + 최신성(0~1) → 카테고리별 Top 5       │
│ 7 BodyFetcher       대표 기사 원문 요청(robots 검사) → Jsoup 본문 추출         │
│ 8 LeadSummarizer    크레디트 제거 → 첫 2~3문장(≤200자) / RSS 대체 / 없음      │
│ 9 KeywordExtractor  KOMORAN 명사 빈도 Top 10                                 │
│10 Renderers         MarkdownRenderer · HtmlRenderer(Thymeleaf) · ReadmeUpdater
│11 RetentionCleaner  site/archive 30일 초과 삭제 → ArchiveRenderer(남은 파일로 목록 재생성)
└──────────────────────────────────────────────────────────────────────────────┘
        │ 파일 생성: news/…md, site/…html, README.md
        ▼
GitHub Actions: git commit → main에 push(배포 키) → Pages 배포
```

**설계 원칙**
- **단계 분리**(NFR-7): 각 단계는 입력과 출력이 분명한 클래스다. 네트워크는 `HttpFetcher` 인터페이스 뒤에 둔다. 테스트에서는 fixture를 돌려주는 가짜 구현을 넣는다.
- **부분 실패 허용**(NFR-2): 피드·본문 단위 예외는 잡아서 `RunReport`에 기록하고 계속 진행한다. 수집 기사가 0건이면 종료 코드 1로 끝나고, 워크플로는 커밋하지 않는다.
- **멱등**(FR-7.4): 출력 경로는 날짜로만 정한다. 다시 실행하면 같은 경로를 덮어쓴다.
- **카테고리 간 중복 방지**: 같은 기사(정규화 URL)가 여러 피드에 있으면(예: 동아 경제·과학), 카테고리 순서(정치→사회→경제→글로벌→IT)대로 먼저 선정된 쪽에만 남긴다.

---

## 4. Data Models
DB는 없다. 메모리 안의 불변 record와 설정 파일만 쓴다.

### 4.1 설정 `src/main/resources/sources.yml` (US8: 코드 수정 없이 언론사 추가·삭제)
```yaml
userAgent: "DailyNewsBot/1.0 (+https://github.com/<owner>/daily_news)"
requestDelayMillis: 1000      # 같은 호스트에 연속 요청할 때 간격
timeoutSeconds: 10
windowHours: 24
perCategory: 5
retention:
  siteArchiveDays: 30         # 웹 날짜별 페이지 보관 일수(실행일 포함)
  newsDays: 0                 # Markdown 보관 일수, 0 = 무기한(삭제 안 함)
sources:
  - id: yonhap
    name: 연합뉴스
    bodySelector: "div.story-news.article > p"
    creditPatterns: ['^\(.+?=연합뉴스\)\s*\S+\s*(기자|특파원)\s*=\s*']
    feeds:
      POLITICS: https://www.yna.co.kr/rss/politics.xml
      SOCIETY:  https://www.yna.co.kr/rss/society.xml
      ECONOMY:  https://www.yna.co.kr/rss/economy.xml
      IT:       https://www.yna.co.kr/rss/industry.xml
      GLOBAL:   https://www.yna.co.kr/rss/international.xml
    itKeywordFilterFeeds: [IT]          # 이 피드는 IT 키워드가 맞는 기사만 사용
  - id: donga
    name: 동아일보
    bodySelector: "section.news_view"
    feeds: { POLITICS: https://rss.donga.com/politics.xml, SOCIETY: https://rss.donga.com/national.xml,
             ECONOMY: https://rss.donga.com/economy.xml, IT: https://rss.donga.com/science.xml,
             GLOBAL: https://rss.donga.com/international.xml }
  - id: khan
    name: 경향신문
    bodySelector: "div#articleBody p.content_text"
    feeds: { POLITICS: https://www.khan.co.kr/rss/rssdata/politic_news.xml, SOCIETY: https://www.khan.co.kr/rss/rssdata/society_news.xml,
             ECONOMY: https://www.khan.co.kr/rss/rssdata/economy_news.xml, IT: https://www.khan.co.kr/rss/rssdata/science_news.xml,
             GLOBAL: https://www.khan.co.kr/rss/rssdata/kh_world.xml }
itKeywords: [AI, 인공지능, 반도체, 통신, 5G, 6G, 플랫폼, 앱, 소프트웨어, 보안, 해킹, 클라우드, 데이터센터, 게임, 로봇, 우주, 배터리, 디스플레이, 네이버, 카카오, 삼성전자, SK하이닉스]
stopwords: [기자, 오늘, 내일, 올해, 지난해, 관련, 대통령실, 정부]   # 키워드 불용어 (조정 가능)
```
본문 selector는 2026-10-28에 실제 기사로 확인했다.
- 연합: `div.story-news.article`
- 동아: `section.news_view`
- 경향: `div#articleBody`(`itemprop=articleBody`), 문단은 `p.content_text`

### 4.2 도메인 record (`dailynews.model`)
```java
enum Category { POLITICS("정치","🏛"), SOCIETY("사회","👥"), ECONOMY("경제","💰"), IT("IT","💻"), GLOBAL("글로벌","🌍"); String label; String emoji; }

record Source(String id, String name, String bodySelector, List<Pattern> creditPatterns,
              Map<Category, URI> feeds, Set<Category> itKeywordFilterFeeds) {}

record Article(Source source, Category category, String title, URI url,      // url = 정규화된 원문 URL(FR-7.0)
               ZonedDateTime publishedAt, String rssDescription) {}

record Story(Category category, Article representative, List<Article> related, // related: 다른 언론사의 같은 사안
             int outletCount, double score) {}

enum SummaryOrigin { BODY, RSS, NONE }
record Summary(String text, SummaryOrigin origin) {}

record NewsItem(Story story, Summary summary) {}

record Newsletter(LocalDate date, ZonedDateTime generatedAt,
                  Map<Category, List<NewsItem>> sections,   // EnumMap, 순서 고정
                  List<String> keywords, RunStats stats) {}

record RunStats(int collected, int afterDedup, int selected,
                List<String> failedFeeds, int bodyOk, int bodyFallback) {}
```

### 4.3 파일 산출물 (저장소 = 데이터 저장소)
| 경로 | 내용 | 갱신 |
|---|---|---|
| `news/YYYY/MM/YYYY-MM-DD.md` | Markdown 뉴스레터(FR-7.1) | 매일 생성, **영구 보관** |
| `site/index.html` | 오늘 뉴스레터(FR-7.2) | 매일 덮어씀 |
| `site/archive/YYYY-MM-DD.html` | 날짜별 페이지 | 매일 생성, **30일 지나면 삭제**(FR-7.5) |
| `site/archive.html` | 날짜 목록(`site/archive/*.html`을 스캔해 최신순으로 정렬) | 매일 재생성 |
| `site/assets/style.css` | 공통 스타일(라이트·다크) | 템플릿에서 복사 |
| `README.md` | `<!-- LATEST:START -->…<!-- LATEST:END -->` 구간만 교체(FR-7.3) | 매일 |

---

## 5. API Design
외부에 공개하는 HTTP API는 없다. 인터페이스는 **CLI**와 **외부 HTTP 호출** 두 가지다.

### 5.1 CLI
```
./gradlew run --args="[--date 2026-10-28] [--root .] [--config path/to/sources.yml] [--dry-run]"
```
| 옵션 | 기본값 | 설명 |
|---|---|---|
| `--date` | 오늘(Asia/Seoul) | 출력 파일 날짜. 수집 기준 시각은 실행 시각이다 |
| `--root` | `.` | 출력 루트(테스트에서는 임시 폴더) |
| `--config` | classpath `sources.yml` | 설정 파일 교체 |
| `--dry-run` | false | 파일을 쓰지 않고 결과 요약만 로그에 출력 |

종료 코드: `0`은 성공(일부 피드 실패 포함), `1`은 수집 0건 또는 설정 오류, `2`는 출력 실패.

### 5.2 외부 HTTP 호출 (`HttpFetcher`)
- 요청 대상: 피드 URL 15개 + 대표 기사 최대 25건(카테고리 5 × 5)
- 공통 설정:
  - `User-Agent: DailyNewsBot/1.0 (+repo URL)`, 타임아웃 10초
  - 3xx는 따라가고, 5xx와 타임아웃은 1회 재시도
- `RobotsPolicy`: 호스트별 robots.txt를 1회 받아 캐시한다. `User-agent: *` 그룹의 `Allow`/`Disallow`(와일드카드 `*`, `$`)를 최장 일치 규칙으로 평가한다. 막힌 URL은 요청하지 않고 `SummaryOrigin.RSS`로 대체한다.
- `HostThrottle`: 같은 호스트에 연속 요청할 때 `requestDelayMillis` 간격을 둔다. 요청은 모두 순차로 보내고, 25건 기준 30초 안팎이다(NFR-1의 5분 이내).

---

## 6. Component Structure
```
daily_news/
├─ settings.gradle                 # rootProject.name, foojay toolchain resolver
├─ build.gradle                    # application, java toolchain 21, 의존성, JitPack 저장소
├─ gradlew, gradlew.bat, gradle/wrapper/
├─ .gitignore                      # .idea/, *.iml, build/, .gradle/, out/
├─ .github/workflows/
│   ├─ daily-news.yml              # schedule + dispatch → run → main에 push → Pages 배포
│   └─ ci.yml                      # pull_request → ./gradlew test
├─ src/main/java/dailynews/
│   ├─ DailyNewsApp.java           # main: CLI 파싱, 파이프라인 조립, 종료 코드
│   ├─ config/   AppConfig, SourceConfig, ConfigLoader
│   ├─ model/    Category, Source, Article, Story, Summary, NewsItem, Newsletter, RunStats
│   ├─ http/     HttpFetcher(interface), JdkHttpFetcher, RobotsPolicy, HostThrottle
│   ├─ collect/  FeedCollector, RssParser(Rome 래핑, dc:date/CDATA 처리)
│   ├─ clean/    UrlNormalizer, TitleNormalizer, ArticleCleaner, CategoryFilter
│   ├─ select/   StoryClusterer, TitleSimilarity, StoryRanker, StorySelector
│   ├─ summary/  BodyExtractor, LeadSummarizer, SentenceSplitter
│   ├─ keyword/  KeywordExtractor(KOMORAN 래핑)
│   └─ render/   MarkdownRenderer, HtmlRenderer, ArchiveRenderer, ReadmeUpdater, RetentionCleaner, OutputPaths
├─ src/main/resources/
│   ├─ sources.yml
│   ├─ templates/ layout.html, newsletter.html, archive.html  (Thymeleaf)
│   └─ static/style.css
├─ src/test/java/dailynews/…       # 패키지별 단위 테스트 + PipelineIntegrationTest
├─ src/test/resources/fixtures/    # yonhap/ donga/ khan/: 실제 RSS 1개 + 기사 HTML 2개씩
├─ news/                           # (생성물) Markdown 아카이브
├─ site/                           # (생성물) Pages 배포 루트
├─ docs/feature/daily-news/        # SDD 문서(spec, plan, tasks). 웹에 공개하지 않음
└─ README.md
```

### 6.1 핵심 알고리즘
**StoryClusterer / TitleSimilarity**
1. 제목 정규화
   - `[단독]`, `[속보]`, `[사설]` 같은 대괄호 말머리와 따옴표·특수문자를 지운다.
   - 공백을 정리하고 소문자로 바꾼다.
2. 유사도 = max(문자 2-gram Jaccard, KOMORAN 명사 집합 Jaccard)
3. 같은 카테고리 안에서 최신순으로 순회한다. 유사도가 0.4 이상(설정값)인 기존 클러스터가 있으면 붙이고, 없으면 새 클러스터를 만든다(greedy single-link).
4. `outletCount` = 클러스터 안의 서로 다른 `source.id` 수

**StoryRanker**: `score = outletCount × 10 + recency`다. recency는 최신 기사 기준 `1 − 경과시간/24h`다. 동점이면 기사 수가 많은 쪽을 앞에 둔다.
- 대표 기사 = 클러스터 안에서 가장 먼저 보도된 기사. 동점이면 `sources.yml` 순서를 따른다.
- `related` = 대표 기사를 제외하고 언론사마다 1건(가장 이른 기사)

**LeadSummarizer**
1. 본문 selector 결과에서 문단 텍스트를 뽑는다. `figure`, `figcaption`, `script`, `.ad`, `em.img_desc` 같은 요소는 제거한다.
2. 문장이 아닌 부분을 지운다: 크레디트 패턴, 이메일, `▶ 관련기사`, `ⓒ`·`무단 전재` 문구, 문단 맨 앞의 `[언론사명]` 머리말
3. `SentenceSplitter`로 문장을 나눈다. 기준은 `다.`, `요.`, `.`, `?`, `!` 뒤 공백이며, 따옴표 안에서는 나누지 않는다.
4. 앞 문장부터 누적한다. 120자 이상이 되거나 3문장이 되면 멈춘다. 200자를 넘으면 마지막 문장을 뺀다. 첫 문장 하나가 200자를 넘으면 200자에서 자르고 `…`을 붙인다.
5. 본문이 없거나 40자 미만이면 RSS 설명문을 텍스트로 바꿔 같은 규칙을 적용한다(`RSS`). 그것도 없으면 `NONE`이다.
- 원문에 없는 문장은 만들지 않는다. 출력 문장은 본문 문장의 부분 문자열이다. 이 성질은 SC5 테스트로 검증한다.

**RetentionCleaner** (FR-7.5)
- 기준일은 `--date`(KST)다. `site/archive/`에서 이름이 `^\d{4}-\d{2}-\d{2}\.html$`인 파일만 대상으로 한다.
- 파일 날짜가 `기준일 − (siteArchiveDays − 1)`보다 이전이면 삭제한다. 30일이면 오늘을 포함해 30개 파일이 남는다.
- `newsDays: 0`이면 `news/`는 건드리지 않는다. 0보다 크면 같은 규칙을 `news/YYYY/MM/*.md`에 적용하고, 비게 된 월 폴더도 지운다.
- 패턴에 맞지 않는 파일, 기준일보다 미래 날짜의 파일, 하위 폴더는 삭제하지 않는다. 삭제한 파일 목록은 로그에 남긴다.
- 순서는 HtmlRenderer → RetentionCleaner → ArchiveRenderer다. 그래서 목록에는 남은 파일만 나온다.

**KeywordExtractor**: 정규화한 모든 제목을 KOMORAN에 넣고 품사 `NNP`(고유명사)와 `NNG`(일반명사) 중 2글자 이상만 남긴다. 불용어를 빼고 빈도 Top 10을 뽑는다. 동률이면 고유명사를 우선한다.

### 6.2 화면 구성 (Thymeleaf)
- `layout.html`
  - 헤더: 제목, 날짜, 아카이브 링크
  - 키워드 칩 목록
  - 카테고리 탭: `<nav role="tablist">`. 각 탭은 해당 `<section id="politics">`로 가는 앵커이고, JS 없이도 동작한다. JS 탭 전환은 선택적으로 덧붙인다.
- `newsletter.html`: 카테고리 섹션 × 기사 카드(`<article>`)
  - 제목 `<a href=원문>`
  - 언론사 배지, 발행 시각(HH:mm), "N개 언론사 보도" 배지
  - 요약(`<blockquote>`, origin이 RSS면 "RSS 요약" 표기)
  - **원문 URL을 텍스트로 표시**(FR-7.0): `<a class="url">https://…</a>`, 긴 URL은 `overflow-wrap:anywhere`로 줄바꿈
  - 같은 사안: 다른 언론사 링크
- `archive.html`: 연·월별로 묶은 날짜 링크
- CSS: `:root` 색 토큰과 `prefers-color-scheme: dark`, 최대 폭 760px, 좌우 여백 16px, 360px 폭에서 가로 스크롤 없음(SC7). 대비는 WCAG AA를 맞춘다.

### 6.3 Markdown 양식 (FR-7.1)
```markdown
# 📰 Daily News · 2026-10-28 (수)
> 수집: 연합뉴스 · 동아일보 · 경향신문 | 수집 512건 → 중복 제거 431건 → 선정 25건
> 🔑 오늘의 키워드: 예산안 · 반도체 · …

## 🏛 정치
### 1. 국회, 내년도 예산안 심사 착수…여야 쟁점 예산 충돌
- 언론사: 연합뉴스 · 08:12 · 🗞 3개 언론사 보도
- 원문: https://www.yna.co.kr/view/AKR20261028000000001
> 국회 예산결산특별위원회가 28일 전체회의를 열고 …
- 같은 사안: [동아일보](https://www.donga.com/…) · [경향신문](https://www.khan.co.kr/article/…)
```
제목·요약의 Markdown 특수문자(`[ ] * _ # < >`)는 이스케이프한다.

---

## 7. State Management (데이터 흐름)
- 프로그램은 **상태가 없다**. 실행할 때마다 RSS에서 처음부터 다시 계산한다. 이전 실행 결과에 의존하는 것은 아카이브 목록(파일 스캔)과 README 마커 교체뿐이다.
- 데이터는 한 방향으로만 흐른다: `AppConfig → List<Article> → List<Story> → List<NewsItem> → Newsletter → files`. 모든 단계 산출물은 불변 record다.
- 시간은 `Clock`을 주입한다(`Clock.system(Asia/Seoul)`). 테스트에서는 고정 시각을 써서 24시간 필터와 recency를 결정적으로 만든다.
- 실행 지표는 `RunStats`로 모아서 로그와 Markdown 헤더에 남긴다(NFR-8). Actions 로그에서 피드별 건수, 실패 피드, 본문 추출 성공률을 볼 수 있다.

---

## 8. Security Considerations
| 위험 | 대응 |
|---|---|
| main 보호와 자동 push의 충돌 | main에 **Repository Ruleset**을 건다: "Require a pull request before merging" + "Block force pushes". **Bypass list에는 Deploy key만 넣는다.** 일일 워크플로는 쓰기 권한이 있는 SSH 배포 키(`secrets.NEWS_DEPLOY_KEY`)로 checkout하고 push한다. 결과: 사람과 기능 변경은 PR로만, 뉴스 커밋만 main에 직접 들어간다(사용자 지시). |
| 비밀값 노출 | 비밀값은 배포 키 1개뿐이고 GitHub Secrets에 저장한다. 코드와 로그에 출력하지 않는다. API 키는 쓰지 않는다. |
| 커밋 이메일 공개(public 저장소) | 작성자 이메일은 저장소 Variables `NEWS_COMMIT_NAME`, `NEWS_COMMIT_EMAIL`로 주입한다. **GitHub noreply 주소**(`ID+username@users.noreply.github.com`)를 권장한다. 잔디에 그대로 집계되고 개인 메일은 노출되지 않는다. |
| 워크플로 권한 | 기본 `permissions: {}`로 두고 job별로 최소 권한만 연다. Pages job: `pages: write`, `id-token: write`. 실행 job은 배포 키로 push하므로 `contents: read`. |
| XSS(외부 RSS 제목·설명 삽입) | Thymeleaf는 `th:text`만 쓴다(자동 이스케이프). `th:utext`는 금지한다. RSS 설명문은 `Jsoup.parse(..).text()`로 텍스트만 남긴다. |
| 악성 링크(`javascript:` 등) | `UrlNormalizer`가 `http`/`https` 스킴과 설정된 언론사 호스트만 허용한다. 나머지 기사는 버린다. |
| 대상 사이트 부담 / 정책 위반 | robots.txt 준수(`RobotsPolicy`), 호스트별 1초 간격, 하루 1회, 식별 가능한 User-Agent(NFR-3) |
| 저작권 | 본문은 메모리에서만 처리하고 저장하지 않는다. 요약은 200자 이하이고 출처와 원문 링크를 항상 함께 표시한다(NFR-4). |
| 공급망 | 의존성 버전을 고정한다(동적 버전 금지). Gradle Wrapper 검증 액션(`gradle/actions/wrapper-validation`)을 쓰고, 액션은 메이저 태그로 고정한다. |

---

## 9. Testing Strategy
| 레벨 | 대상 | 방법 |
|---|---|---|
| 단위 | `RssParser` | fixture RSS 3종(연합·동아·경향)을 파싱한다. 건수, `pubDate`/`dc:date`, CDATA, 인코딩을 검증한다. |
| 단위 | `UrlNormalizer`, `TitleNormalizer` | `utm_*` 제거, `&amp;` 디코딩, 말머리 제거, 비 http 스킴 거부 |
| 단위 | `ArticleCleaner`, `CategoryFilter` | 24시간 경계(고정 `Clock`), URL·제목 중복, IT 키워드 필터 |
| 단위 | `TitleSimilarity`, `StoryClusterer`, `StoryRanker` | 같은 사안의 제목 쌍(예: 3사 보도)은 1개 클러스터로 묶이고, 다른 사안은 분리된다. 점수 정렬, Top 5, 5개 미만일 때 처리(FR-4.4) |
| 단위 | `BodyExtractor`, `LeadSummarizer`, `SentenceSplitter` | 언론사별 기사 HTML fixture에서 크레디트가 제거되고, 200자 이하이며, **요약 문장이 본문의 부분 문자열**인지 검증한다(SC5). 본문이 없으면 RSS·NONE으로 대체되는지 본다. |
| 단위 | `RobotsPolicy` | Disallow/Allow 최장 일치, 와일드카드, 서울신문 `/xml/` 차단 사례 |
| 단위 | `KeywordExtractor` | 불용어 제외, Top 10, 2글자 이상 |
| 단위 | `RetentionCleaner` | 30일째 파일은 보존되고 31일째 파일은 삭제되는지(경계값). 패턴 외 파일과 미래 날짜 파일은 보존. `newsDays: 0`이면 Markdown 미삭제. 삭제 후 아카이브 목록 갱신(SC9) |
| 단위 | `MarkdownRenderer`, `HtmlRenderer` | 스냅샷 비교(golden file). 모든 기사에 원문 URL 문자열이 있는지(FR-7.0, SC3-1). 이스케이프(`<script>` 제목 → 텍스트로 출력) |
| 통합 | `PipelineIntegrationTest` | `FakeHttpFetcher`(fixture를 돌려줌) + `@TempDir` + 고정 `Clock`으로 전체 실행. 카테고리 5개 존재, 산출 파일 경로, README 마커 교체, 아카이브 목록, 두 번 실행해도 결과 동일(멱등) |
| 통합 | 장애 시나리오 | 피드 1개 500 응답 → 성공하고 `failedFeeds`에 기록(SC6). 전체 실패 → 종료 코드 1, 파일 미생성(NFR-2) |
| 라이브(수동) | `@Tag("live")` `LiveFeedSmokeTest` | 실제 피드 15개가 200을 돌려주고 selector로 본문 80자 이상이 나오는지 본다. 기본 `test`에서는 빼고 `./gradlew liveTest`로 실행한다. 사이트 구조 변경 감지용이다(A1). |
| E2E | 워크플로 | `workflow_dispatch`로 수동 실행 → main 커밋, Pages 반영, 잔디 확인. 7일 연속 운영 관찰(SC1, SC2) |

- CI(`ci.yml`): 모든 PR에서 `./gradlew test`를 돈다. 실패하면 병합할 수 없다(Ruleset의 required status check).
- 커버리지는 정량 목표 대신, 위 표의 클래스마다 테스트 1개 이상을 원칙으로 한다.

---

## 10. Deployment Plan

### 10.1 로컬 개발 환경
1. JDK 21을 설치한다(Temurin 또는 IntelliJ의 "Download JDK"). Gradle 9는 실행 JVM으로 17 이상이 필요해서 1.8로는 실행되지 않는다.
2. `./gradlew test`, `./gradlew run --args="--dry-run"`, `./gradlew run`을 실행하고 `site/index.html`을 브라우저로 연다.
3. IntelliJ에서는 기존 `.iml` 대신 Gradle 프로젝트로 다시 가져온다. `.idea`와 `*.iml`은 `.gitignore`에 넣는다.

### 10.2 GitHub 1회 설정
1. public 저장소 `daily_news`를 만들고 `main`과 `feature/daily-news`를 push한다.
2. **Deploy key**: 로컬에서 `ssh-keygen -t ed25519`로 키를 만든다.
   - 공개키는 Settings → Deploy keys에 **Allow write access**로 등록한다.
   - 개인키는 Settings → Secrets → Actions에 `NEWS_DEPLOY_KEY`로 등록한다.
3. **Variables**: `NEWS_COMMIT_NAME`, `NEWS_COMMIT_EMAIL`(GitHub noreply 주소. Settings → Emails에서 확인)
4. **Ruleset**(main)
   - Require pull request, Require status check `ci / test`, Block force push
   - **Bypass: Deploy keys**
5. **Pages**: Settings → Pages → Source를 **GitHub Actions**로 설정

### 10.3 `daily-news.yml` (요지)
```yaml
on:
  schedule: [{ cron: "0 0 * * *" }]   # 09:00 KST (FR-1.1)
  workflow_dispatch: {}                # 수동 실행 (FR-1.2)
permissions: {}
concurrency: { group: daily-news, cancel-in-progress: false }
jobs:
  build:
    runs-on: ubuntu-latest
    permissions: { contents: read }
    env: { TZ: Asia/Seoul }
    steps:
      - uses: actions/checkout@v4
        with: { ref: main, ssh-key: "${{ secrets.NEWS_DEPLOY_KEY }}" }
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: "21" }
      - uses: gradle/actions/setup-gradle@v4      # 의존성 캐시(KOMORAN 포함)
      - run: ./gradlew run --no-daemon
      - name: Commit & push to main
        run: |
          git config user.name  "${{ vars.NEWS_COMMIT_NAME }}"
          git config user.email "${{ vars.NEWS_COMMIT_EMAIL }}"
          git add -A news site README.md        # 보관 기간이 지나 삭제된 파일도 스테이징
          git diff --cached --quiet && { echo "no changes"; exit 0; }   # FR-8.3
          git commit -m "news: $(date +%F) daily newsletter"
          for i in 1 2 3; do git pull --rebase origin main && git push origin HEAD:main && break; sleep 5; done
      - uses: actions/upload-pages-artifact@v3
        with: { path: site }
  deploy:
    needs: build
    runs-on: ubuntu-latest
    permissions: { pages: write, id-token: write }
    environment: { name: github-pages, url: "${{ steps.d.outputs.page_url }}" }
    steps:
      - id: d
        uses: actions/deploy-pages@v4
```
- 예약 워크플로는 **main에 있는 파일 기준**으로 동작한다. 그래서 첫 PR이 병합되어야 스케줄이 활성화된다.
- `ci.yml`: `on: pull_request` → checkout → setup-java 21 → `./gradlew test`

### 10.4 브랜치·릴리스 흐름
`feature/*` → PR(`ci` 통과 필요) → main 병합 → 다음 09:00부터 새 코드로 실행. 뉴스 커밋(`news: YYYY-MM-DD …`)은 매일 main에 직접 쌓인다. 작업 브랜치는 PR 전에 main을 rebase하거나 merge해서 뉴스 커밋을 따라간다.

---

## 11. Dependencies
`build.gradle` (Groovy DSL)
```groovy
plugins {
    id 'java'
    id 'application'
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }          // KOMORAN 전용
}

dependencies {
    implementation 'com.rometools:rome:2.1.0'
    implementation 'org.jsoup:jsoup:1.23.2'
    implementation 'org.thymeleaf:thymeleaf:3.1.5.RELEASE'
    implementation 'com.github.shin285:KOMORAN:3.3.9'
    implementation 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.22.4'
    runtimeOnly    'org.slf4j:slf4j-simple:2.0.20'

    testImplementation platform('org.junit:junit-bom:6.1.3')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly    'org.junit.platform:junit-platform-launcher'
    testImplementation 'org.assertj:assertj-core:3.27.7'
}

application { mainClass = 'dailynews.DailyNewsApp' }

tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }

test { useJUnitPlatform { excludeTags 'live' } }
tasks.register('liveTest', Test) { useJUnitPlatform { includeTags 'live' } }
```
`settings.gradle`
```groovy
plugins { id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0' }
rootProject.name = 'daily_news'
```

| 패키지 | 버전 | 라이선스 | 비고 |
|---|---|---|---|
| Gradle (Wrapper) | 9.8.2 | Apache-2.0 | 실행 JVM 17 이상 필요 |
| com.rometools:rome | 2.1.0 | Apache-2.0 | RSS |
| org.jsoup:jsoup | 1.23.2 | MIT | HTML |
| org.thymeleaf:thymeleaf | 3.1.5.RELEASE | Apache-2.0 | 템플릿(Spring 없이 사용) |
| com.github.shin285:KOMORAN | 3.3.9 | Apache-2.0 | JitPack, jar 약 6MB |
| jackson-dataformat-yaml | 2.22.4 | Apache-2.0 | 설정 |
| org.slf4j:slf4j-simple | 2.0.20 | MIT | 로깅 |
| JUnit BOM | 6.1.3 | EPL-2.0 | 테스트 |
| AssertJ | 3.27.7 | Apache-2.0 | 테스트 |
| actions: checkout v4, setup-java v4, gradle/actions v4, upload-pages-artifact v3, deploy-pages v4 | — | MIT | CI/CD |

버전은 2026-10-28 기준 Maven Central, JitPack, Gradle 배포 정보로 확인했다. 정식 릴리스만 쓰고 alpha와 M 버전은 제외했다.

---

## 12. 열린 이슈 / 리스크
- **R1 사이트 구조 변경**: selector나 RSS가 바뀌면 요약이 RSS 설명문으로 대체된다. 실행은 계속되며, `liveTest`로 주기적으로 점검한다.
- **R2 Actions 지연**: 정각 예약은 수십 분 늦을 수 있다. SC1 기준(10:00)으로 7일간 관찰하고, 늦어지면 cron을 앞당기는 것을 검토한다.
- **R3 Ruleset Bypass UI**: Deploy key를 bypass 대상으로 지정하는 메뉴를 설정할 때 확인해야 한다. 지정할 수 없으면 대안은 "Ruleset의 bypass에 Repository admin 역할 + 소유자 fine-grained PAT(contents: write)로 push"다. 이 경우 비밀값은 PAT로 바뀐다.
- **R4 클러스터링 임계값(0.4)**: fixture와 실제 데이터로 조정한다. 값은 `sources.yml`에서 바꿀 수 있게 둔다.
- **R5 IT 카테고리 표본 부족**: 경향 과학은 하루 3건 안팎이다. 연합 산업(키워드 필터)과 동아 과학이 주 공급원이고, 5건이 안 되면 FR-4.4에 따라 있는 만큼만 표시한다.
