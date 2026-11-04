# 지난 뉴스레터 기사 재선정 방지 구현 계획 (feat/history-dedup)

명세: [spec.md](spec.md)

## 1. Original Tech Stack Request

> (입력 없음)

`/sdd:plan`에 기술 스택을 따로 적지 않았다. 이번 작업은 기존 파이프라인의 정제 단계에 기능 하나를 더하는 것이고, 스택은 저장소 구조로 이미 정해져 있어 따로 묻지 않았다(Java 21 toolchain, Gradle, JUnit 6 + AssertJ, `build.gradle`·`src/` 기준).

## 2. Technology Stack

| 영역 | 사용 기술 | 비고 |
|---|---|---|
| 언어 | Java 21 | 기존 toolchain 그대로 |
| 빌드 | Gradle(wrapper) | 작업 추가 없음 |
| 설정 | Jackson YAML(`sources.yml`) | 기존 `ConfigLoader`에 항목 1개 추가 |
| 지난 뉴스레터 읽기 | 기존 `MarkdownNewsletterReader` | 새 파서를 만들지 않는다 |
| 파일 입출력 | `java.nio.file.Files` | UTF-8 |
| 로그 | SLF4J | 기존 그대로 |
| 테스트 | JUnit Jupiter, AssertJ, `@TempDir` | 기존 그대로 |

새 의존성은 없다(spec NFR-5).

## 3. Architecture Overview

### 3.1 바뀐 실행 흐름

```
수집 ─→ [지난 뉴스레터 URL 모으기] ─→ 정제(정규화 → 기간 → ★지난 URL 빼기 → 중복) ─→ IT 필터 ─→ 클러스터 ─→ 선정 ─→ 요약 ─→ 키워드 ─→ 출력(그대로)
            │
            └─ news/YYYY/MM/YYYY-MM-DD.md  (출력 날짜 −historyDays ~ −1일)
```

- `DailyNewsPipeline.run()`이 수집 직후 `PublishedHistory.urlsBefore(paths, date)`로 URL 집합을 만들고, `ArticleCleaner.clean(articles, published)`에 넘긴다.
- 키워드와 `RunStats.afterDedup`은 지금처럼 `cleaned` 목록에서 계산하므로, 지난 기사를 뺀 뒤 기준이 된다(spec FR-2.4). 코드를 따로 바꿀 필요가 없다.
- 출력(`write`)은 바꾸지 않는다. `--dry-run`에서도 URL 모으기는 실행한다(읽기만 하므로, spec FR-1.6).

### 3.2 정제 단계 안의 순서

`ArticleCleaner.clean`의 기사별 판정 순서는 다음과 같다. 먼저 걸린 조건 하나로만 센다.

1. URL 정규화 실패 → `허용되지 않는 링크`
2. 기간 밖(24시간 이전 또는 미래) → `기간 밖`
3. **정규화 URL이 지난 URL 집합에 있음 → `지난 뉴스레터`** (새로 추가)
4. 같은 카테고리 URL·제목 중복 → `중복`

3번을 정규화 뒤에 두는 이유는 Markdown에 저장된 URL이 정규화된 URL이기 때문이다(spec A1). 기간 판정 뒤에 두는 것은 로그 숫자가 "기간 안에 있었는데 지난 뉴스레터 때문에 빠진 기사"를 뜻하게 하기 위해서다. 4번보다 앞에 두면, 같은 기사가 지난 URL(정규화 전 `utm_*`가 붙은 판)과 새 URL로 나뉘어 와도 둘 다 정규화 후 같은 URL이라 함께 빠진다.

### 3.3 날짜 범위 규칙

```
출력 날짜 D, historyDays N (N ≥ 1)
읽는 날짜: D−N, D−N+1, …, D−1   (D 당일, D 이후는 읽지 않음)
N = 0: 파일을 하나도 읽지 않고 빈 집합
```

- D 당일을 빼므로 같은 날 다시 실행해도 결과가 바뀌지 않는다(기존 FR-7.4 멱등, spec G3).
- 기준은 파일 이름의 날짜(출력 날짜)다. 생성 시각(`생성: … KST`)은 쓰지 않는다. 2026-11-04 00:23처럼 자정 직후에 만든 파일도 그 파일 날짜로만 판단한다.

## 4. Data Models

### 4.1 설정

`sources.yml`(기본 설정)에 추가:

```yaml
windowHours: 24               # 최근 몇 시간 안의 기사만 쓸지
historyDays: 7                # 이전 며칠 치 뉴스레터에 실린 기사를 다시 고르지 않을지(0 = 끔, 생략하면 7)
```

`AppConfig` record에 `int historyDays`를 `windowHours` 바로 뒤에 추가한다. Javadoc `@param`도 추가한다.

`ConfigLoader`:

```java
static final int DEFAULT_HISTORY_DAYS = 7;
// RawConfig에 Integer historyDays 추가
int historyDays = raw.historyDays() == null ? DEFAULT_HISTORY_DAYS
        : requireAtLeast(raw.historyDays(), 0, "historyDays");
```

- 생략 → 7, 음수 → `historyDays: 0 이상이어야 합니다(현재 -1)`.
- `"a"`처럼 정수가 아닌 값은 Jackson이 실패하므로 기존 경로대로 `YAML을 해석할 수 없습니다` 설정 오류가 된다(종료 코드 1, spec FR-3.3).

### 4.2 지난 URL 집합

- 타입은 `Set<URI>`(불변, `Set.copyOf`)다. `Article.url()`이 `URI`이고, `URI.equals`는 정규화된 같은 문자열끼리 같다.
- 원소는 각 `NewsItem`의 `story().representative().url()`(원문)과 `story().related()`의 각 `url()`(같은 사안)이다.
- 같은 사안 링크의 `%28`/`%29`는 `MarkdownNewsletterReader`가 `(`/`)`로 되돌린다. `MarkdownRenderer`가 쓸 때 바꾼 것을 되돌리는 것이므로 원래 URL과 같다.

### 4.3 정제 결과 통계

`RunStats`와 Markdown 헤더 형식은 바꾸지 않는다(spec FR-5.1). 뺀 건수는 로그로만 남긴다. 로그 형식은 다음과 같다(숫자는 예시).

```
정제: 1100건 → 960건 (허용되지 않는 링크 0, 기간 밖 40, 지난 뉴스레터 46, 중복 54)
지난 뉴스레터: 2026-10-28 ~ 2026-11-03 중 7개 파일에서 URL 363개 (건너뜀 0)
```

## 5. API Design

외부 API나 HTTP 엔드포인트는 없다. 클래스 사이의 공개 메서드만 바뀐다.

| 클래스 | 메서드 | 내용 |
|---|---|---|
| `PublishedHistory`(신규) | `PublishedHistory(int historyDays, MarkdownNewsletterReader reader)` | |
| | `Set<URI> urlsBefore(OutputPaths paths, LocalDate date)` | §3.3 범위의 Markdown에서 URL을 모은다. 예외를 던지지 않는다 |
| `ArticleCleaner` | `List<Article> clean(List<Article> articles, Set<URI> published)` | 신규. §3.2 순서로 판정 |
| | `List<Article> clean(List<Article> articles)` | 기존. `clean(articles, Set.of())`에 위임(기존 테스트·동작 유지) |
| `AppConfig` | `int historyDays()` | 신규 컴포넌트 |
| `DailyNewsPipeline` | `run(LocalDate, Path, boolean)` | 시그니처 그대로. 안에서 `OutputPaths`를 먼저 만들어 `PublishedHistory`에 넘긴다 |

### 5.1 `PublishedHistory.urlsBefore` 동작

```
for d in D−N .. D−1:
    file = paths.markdown(d)
    파일 없음 → 건너뜀(경고 아님, 셈하지 않음)          # spec FR-1.5
    읽기·해석 실패 → warn("지난 뉴스레터를 읽지 못해 건너뜁니다: {파일} - {이유}"), 건너뜀 수 +1   # spec FR-1.4
    성공 → 원문 URL + 같은 사안 URL을 집합에 추가, 읽은 파일 수 +1
info(§4.3 둘째 줄)
return Set.copyOf(urls)
```

- 잡는 예외는 `IOException`(인코딩 오류 `MalformedInputException` 포함), `IllegalArgumentException`(리더의 형식 오류, `URI.create`, `NumberFormatException`), `DateTimeException`(생성 시각 파싱)이다. 다른 런타임 예외는 버그로 보고 잡지 않는다.
- 읽기는 `Files.readString(file, UTF_8)`이다. CRLF는 리더가 처리한다.

## 6. Component Structure

```
dailynews
├── DailyNewsPipeline          (수정) PublishedHistory 생성·호출, cleaner.clean(articles, published)
├── config
│   ├── AppConfig              (수정) historyDays 추가
│   └── ConfigLoader           (수정) historyDays 읽기·검증, 기본값 7
├── clean
│   └── ArticleCleaner         (수정) published 집합으로 빼기 + 로그 항목
└── render
    ├── MarkdownNewsletterReader (그대로) 재사용
    ├── OutputPaths            (그대로) markdown(date) 재사용
    └── PublishedHistory       (신규) 지난 Markdown → Set<URI>
```

`PublishedHistory`를 `render` 패키지에 두는 이유는 다음과 같다. `news/` 파일 경로(`OutputPaths`)와 Markdown 형식(`MarkdownRenderer`/`MarkdownNewsletterReader`)을 아는 곳이 `render`이고, `RetentionCleaner`도 같은 방식으로 `news/`를 다룬다. `ArticleCleaner`는 파일을 모르고 URL 집합만 받는다(spec NFR-3).

## 7. State Management

- 상태 저장소는 `news/` Markdown뿐이다(spec C2). 새 상태 파일, 캐시, DB는 없다.
- 실행마다 처음부터 다시 읽는다. 파일 7개(각 약 16KB)라 비용이 무시할 만하다(spec NFR-1).
- GitHub Actions `build` job은 매번 `main`을 체크아웃하므로 지난 Markdown이 항상 있다. 커밋 전 `git pull --rebase`로 경쟁 실행과 합쳐질 때도 Markdown 형식은 그대로다.
- `retention.newsDays`가 `historyDays`보다 짧으면 보관 정리로 지워진 날짜는 비교하지 않는다(spec FR-5.3). 보관 정리는 `write()` 안에서 선정 뒤에 실행되므로 이번 실행의 비교에는 영향이 없다.

## 8. Security Considerations

- 새 네트워크 요청, 비밀값, 권한이 없다(spec NFR-5). 워크플로 권한도 그대로다.
- 읽는 경로는 `OutputPaths.markdown(date)`로 만든 날짜 경로뿐이다. 사용자 입력이나 파일 내용으로 경로를 만들지 않으므로 경로 조작 위험이 없다.
- Markdown 내용은 URL 집합을 만드는 데만 쓰고 출력에 다시 쓰지 않는다. 깨진 URL은 `IllegalArgumentException`으로 그 파일만 건너뛴다.

## 9. Testing Strategy

모든 테스트는 네트워크 없이 실행한다(`./gradlew test`).

### 9.1 단위

| 테스트 | 검증 | spec |
|---|---|---|
| `render/PublishedHistoryTest`(신규) | `@TempDir`에 `MarkdownRenderer`로 2026-10-27 ~ 2026-11-05 파일을 쓰고 D=2026-11-04, N=7이면 10-28 ~ 11-03 URL만 모인다. 10-27·11-04·11-05 URL은 없다 | SC1 |
| | 원문 URL과 같은 사안 URL(괄호 포함 URL 1건)이 모두 들어간다 | SC2, A1 |
| | 깨진 파일 1개(카테고리 누락)가 섞여도 나머지 URL은 모두 모이고 예외가 나지 않는다 | SC2, FR-1.4 |
| | `news/` 폴더가 없으면 빈 집합, N=0이면 파일이 있어도 빈 집합 | SC2, FR-1.5, FR-3.2 |
| `clean/ArticleCleanerTest`(추가) | 지난 URL과 같은 기사는 카테고리가 달라도 빠진다. `utm_*`가 붙은 판도 정규화 후 빠진다 | SC3 |
| | 빈 집합이면 기존 `clean(list)`와 결과가 같다 | SC3 |
| `config/ConfigLoaderTest`(추가) | 생략 → 7, `0` → 0, `-1` → 설정 오류, `"a"` → 설정 오류, 기본 `sources.yml` → 7 | SC5 |

테스트 Markdown은 `TestNewsletters.on(date, variant, failed)`와 `MarkdownRenderer`로 만든다. 실제 출력 형식과 같은 파일로 왕복 검증하기 위해서다.

### 9.2 통합 (`PipelineIntegrationTest`에 추가)

`FixtureWeb.withArticles()`와 고정 시각 `FixtureWeb.CLOCK`은 두 번 실행해도 같은 기사를 돌려준다. 이것으로 "42분 간격 재실행"과 같은 최악의 상황을 재현한다.

1. **이어지는 날짜:** 같은 root에서 2026-10-27로 실행한 뒤 2026-10-28로 실행한다. 둘째 결과의 원문·같은 사안 URL 중 첫째 결과의 URL은 0건이고, 종료 코드는 0이다. 후보가 없어 카테고리가 비어도 실패하지 않는다(기존 FR-4.4). (SC4)
2. **같은 날 재실행:** 기존 `두_번_실행해도_결과_파일이_바이트_단위로_같다_FR_7_4`가 그대로 통과한다. 당일 파일을 읽지 않는다는 회귀 검증이다. (SC4, G3)
3. **기존 SC9 테스트 확인:** `이전_31일치가_있으면…` 테스트는 `TestNewsletters` URL로 지난 Markdown 31개를 미리 만든다. 이 URL이 fixture 기사 URL과 겹치지 않는지 구현할 때 확인한다. 겹치면 보관 정리만 검증하는 테스트이므로 기대값이 아니라 fixture 쪽을 조정한다.

### 9.3 회귀

- `MarkdownRendererTest`의 골든 파일(`src/test/resources/golden/newsletter.md`)은 바뀌지 않는다. (SC7)
- 전체 `./gradlew test` 통과. (SC7)

### 9.4 실제 데이터 재현 (수동, SC6)

`--dry-run`은 파일을 쓰지 않으므로, 저장소의 `news/`를 임시 root로 복사해 실제로 쓰게 한 뒤 비교한다.

```bash
R=<scratchpad>/replay && rm -rf "$R" && mkdir -p "$R" && cp -r news "$R/"
./gradlew run --args="--root $R --date 2026-11-04"
# R/news/2026/11/2026-11-04.md의 URL과 R/news의 10-28~11-03 URL 교집합이 0건인지 스크립트로 확인
```

로그의 `지난 뉴스레터 N`(N > 0, 11-03이 23:41 생성이므로)과 카테고리별 선정 수를 구현 보고에 기록한다.

## 10. Deployment Plan

- 워크플로(`.github/workflows/daily-news.yml`, `ci.yml`)는 바꾸지 않는다. `build` job의 `./gradlew run`이 체크아웃한 `news/`를 그대로 읽는다.
- `sources.yml`에 `historyDays: 7`을 명시한다(생략해도 7이지만 설정 위치를 보이게 한다).
- 문서를 갱신한다.
  - `README.md` "어떻게 만드나요 → 선정"에 "최근 7일 뉴스레터에 실린 기사는 빼고" 한 줄을 추가한다. `<!-- LATEST -->` 블록 밖이라 파이프라인이 덮어쓰지 않는다.
  - `docs/feature/daily-news/spec.md`의 FR-3에 FR-3.4를 추가하고 이 명세로 링크한다.
- 반영 순서: `feat/history-dedup`에서 PR → CI 통과 → main 병합. 일일 실행(12:00 KST) 전에 병합되면 그날 뉴스레터부터 적용된다. 병합 뒤 첫 실행 로그에서 `지난 뉴스레터` 항목을 확인한다.
- 이미 게시된 날짜(2026-11-01, 2026-11-04 등)의 중복은 고치지 않는다(spec C3). 단 2026-11-04는 12:00 실행에서 다시 만들어지므로, 그 전에 병합되면 자연스럽게 새 기사로 바뀐다.
- 되돌리기: `sources.yml`에서 `historyDays: 0`으로 바꾸면 코드를 되돌리지 않고 기능을 끌 수 있다.

## 11. Dependencies

| 패키지 | 버전 | 변경 |
|---|---|---|
| com.fasterxml.jackson.dataformat:jackson-dataformat-yaml | 2.22.4 | 그대로(설정 읽기) |
| org.slf4j:slf4j-api | 2.0.20 | 그대로(로그) |
| org.junit:junit-bom / junit-jupiter | 6.1.3 | 그대로(테스트) |
| org.assertj:assertj-core | 3.27.7 | 그대로(테스트) |

새로 추가하거나 버전을 올리는 의존성은 없다.
