# 지난 뉴스레터 기사 재선정 방지 — 작업 목록 (Tasks)

> 기준: [`spec.md`](./spec.md), [`plan.md`](./plan.md) · 브랜치: `feat/history-dedup` · 작성일: 2026-11-04
> 기존 시스템: [`../../feature/daily-news/tasks.md`](../../feature/daily-news/tasks.md)

## Summary
| 그룹 | 이름 | 작업 범위 | 복잡도 | 작업 수 |
|---|---|---|---|---|
| G1 | 설정 | T-001 | Low | 1 |
| G2 | 지난 뉴스레터 URL 모으기 | T-002 ~ T-003 | Medium | 2 |
| G3 | 정제 단계에서 빼기 | T-004 | Low | 1 |
| G4 | 파이프라인 연결·검증 | T-005 ~ T-008 | Medium | 4 |
| G5 | 문서·배포 | T-009 ~ T-010 | Low | 2 |
| G6 | 선택·후속 | T-011 | Low | 1 |

- **총 11개 작업** / 필수 10개 / 선택 1개
- 진행 원칙: 작업 하나 = 테스트나 점검으로 확인할 수 있는 결과 하나. 모든 작업은 `feat/history-dedup` 브랜치에서 하고, main에는 PR로만 반영한다.
- Markdown·웹·README 출력 형식, 수집 구간(`windowHours`), 선정·요약 규칙은 바꾸지 않는다(spec FR-5).
- 공통 완료 조건: `./gradlew test` 통과
- 일정: 일일 실행(12:00 KST) 전에 병합하면 그날 뉴스레터부터 적용된다(plan §10).

## Progress
| ID | Task | Group | Required | Status | Notes |
|---|---|---|---|---|---|
| T-001 | `historyDays` 설정 추가(AppConfig·ConfigLoader·sources.yml) | G1 | Yes | Done | 2026-11-04: `AppConfig.historyDays`(windowHours 뒤), `ConfigLoader.DEFAULT_HISTORY_DAYS = 7`, sources.yml `historyDays: 7`. `ConfigLoaderTest` 34개 통과(생략→7, 0, -1·"a" 오류, 기본 설정 7) |
| T-002 | `PublishedHistory`: 날짜 범위의 Markdown에서 URL 모으기 | G2 | Yes | Done | 2026-11-04: `render/PublishedHistory`. 범위·원문/같은 사안 URL·괄호 URL 왕복·N=0 케이스. `TestNewsletters.on`은 날짜가 달라도 URL이 같아서 테스트에서 날짜별 URL 뉴스레터를 직접 만들어 `MarkdownRenderer`로 씀 |
| T-003 | `PublishedHistory`: 없는·깨진 파일 처리와 로그 | G2 | Yes | Done | 2026-11-04: 경고 형식은 기존 render 코드(`LegacyArchiveUpgrader`, `DayDigestReader`)를 따라 `({e.toString()})`로 맞춤(plan §5.1의 `- {이유}`와 다름). 형식 오류·잘못된 UTF-8·news 없음·CRLF 케이스 추가, `PublishedHistoryTest` 6개 통과 |
| T-004 | `ArticleCleaner`: 지난 URL 빼기와 정제 로그 | G3 | Yes | Done | 2026-11-04: 새 매개변수 이름은 지역 변수 `published`(발행 시각)와 겹쳐 `publishedUrls`로 둠. `ArticleCleanerTest` 9개 통과(카테고리 무관 제외·`utm_*` 판 제외·URL 다르면 유지·빈 집합이면 기존과 같음) |
| T-005 | `DailyNewsPipeline`에 연결 | G4 | Yes | Done | 2026-11-04: `OutputPaths`를 run() 처음에 만들어 history와 write()가 함께 씀. 기존 `PipelineIntegrationTest` 9·`PipelineFailureTest` 3·`DailyNewsAppTest` 5 통과 |
| T-006 | 통합 테스트: 이어지는 날짜 0건·같은 날 재실행 | G4 | Yes | Done | 2026-11-04: `바로_전날_뉴스레터에_실린_기사는_다시_고르지_않는다_history_dedup_SC4` 추가. 둘째 날 정제 로그 `225건 → 90건 (지난 뉴스레터 135)`, 겹침 0. SC9 테스트의 지난 Markdown 31개는 내용이 `old`라 형식 오류로 건너뜀(경고 7건, 범위 안 7일분) → fixture URL과 겹치지 않아 조정 불필요. 옛 디자인 테스트의 10-27 Markdown(URL 48개)도 fixture와 겹치지 않음 |
| T-007 | 회귀 확인: 전체 테스트·골든 파일 | G4 | Yes | Done | 2026-11-04: `./gradlew test` 389개 통과(실패 0). `src/test/resources/`(golden 포함)와 템플릿 변경 없음 |
| T-008 | 실제 데이터 재현(2026-11-04) | G4 | Yes | Done | 2026-11-04 18:19 KST 실행(임시 root에 news/ 복사): `지난 뉴스레터: 2026-10-28 ~ 2026-11-03 중 6개 파일에서 URL 280개 (건너뜀 0)`(10-28 파일은 원래 없음), `정제: 1100건 → 983건 (기간 밖 101, 지난 뉴스레터 8, 중복 8)`, 5개 분야 각 5개 선정. 새 11-04 URL 54개와 지난 URL 280개 교집합 0건. 참고: 오늘 12:01 정기 실행으로 이미 게시된 11-04는 11-03과 URL 8건이 겹침(병합 전 실행이라 적용 안 됨) |
| T-009 | 문서 갱신(README·기존 spec FR-3.4) | G5 | Yes | Done | 2026-11-04: README "선정"에 최근 7일 제외 문장 추가(LATEST 블록 밖). `docs/feature/daily-news/spec.md` FR-3.4 추가·링크 |
| T-010 | PR·병합·첫 실행 로그 확인 | G5 | Yes | In Progress | 2026-11-04: origin/main(12:01 일일 커밋 포함) 위로 리베이스 후 PR 생성. 병합 승인과 병합 뒤 첫 일일 실행(2026-11-05 12:00) 로그 확인 대기 |
| T-011 | 이어지는 사안 중복 관찰(1주) | G6 | No | Todo | |

---

## G1. 설정 (Low)

### T-001 `historyDays` 설정 추가(AppConfig·ConfigLoader·sources.yml)
- **Purpose**: 비교할 일수를 코드 수정 없이 바꾸고, `0`으로 기능을 끌 수 있게 한다(spec FR-3, US4).
- **Required**: Yes · **Depends**: — · **Ref**: plan §4.1, spec FR-3.1~3.3
- **작업**:
  - `AppConfig` record에 `int historyDays`를 `windowHours` 바로 뒤에 추가하고 `@param` Javadoc을 쓴다.
  - `ConfigLoader`에 `DEFAULT_HISTORY_DAYS = 7`을 두고 `RawConfig`에 `Integer historyDays`를 추가한다. 생략하면 7, 값이 있으면 `requireAtLeast(…, 0, "historyDays")`로 검증한다.
  - `src/main/resources/sources.yml`의 `windowHours` 아래에 `historyDays: 7`과 설명 주석을 넣는다.
  - `ConfigLoaderTest`에 다음을 추가한다: 생략 → 7, `0` → 0, `-1` → 설정 오류(메시지에 `historyDays`), `"a"` → 설정 오류, 기본 `sources.yml` → 7.
- **완료 조건**: 추가한 `ConfigLoaderTest`와 기존 설정 테스트가 통과한다(spec SC5).

## G2. 지난 뉴스레터 URL 모으기 (Medium)

### T-002 `PublishedHistory`: 날짜 범위의 Markdown에서 URL 모으기
- **Purpose**: 출력 날짜 이전 N일 뉴스레터에 실린 기사 URL을 집합으로 만든다. 당일과 그 이후 파일은 읽지 않아 같은 날 재실행을 멱등으로 유지한다(spec FR-1.1~1.3, G3).
- **Required**: Yes · **Depends**: — · **Ref**: plan §3.3, §4.2, §5, §5.1, §6
- **작업**:
  - `src/main/java/dailynews/render/PublishedHistory.java`를 만든다. 생성자는 `(int historyDays, MarkdownNewsletterReader reader)`, 메서드는 `Set<URI> urlsBefore(OutputPaths paths, LocalDate date)`다.
  - D−N부터 D−1까지 `paths.markdown(d)`를 `Files.readString(…, UTF_8)`로 읽고 `reader.read(text, d)`로 해석한다. 모든 카테고리의 `representative().url()`과 `related()`의 `url()`을 모아 `Set.copyOf`로 돌려준다.
  - `historyDays == 0`이면 파일을 읽지 않고 `Set.of()`를 돌려준다.
  - `render/PublishedHistoryTest`(신규)를 만든다. `@TempDir`에 `MarkdownRenderer`와 `TestNewsletters.on(…)`으로 2026-10-27 ~ 2026-11-05 파일을 쓴다. D=2026-11-04, N=7이면 10-28 ~ 11-03의 URL만 있고 10-27·11-04·11-05의 URL은 없어야 한다. 같은 사안 URL과 괄호가 든 URL 1건이 왕복되는지도 확인한다. N=0이면 파일이 있어도 빈 집합이어야 한다.
- **완료 조건**: `PublishedHistoryTest`의 범위·URL·N=0 케이스가 통과한다(spec SC1, SC2 일부).

### T-003 `PublishedHistory`: 없는·깨진 파일 처리와 로그
- **Purpose**: 지난 파일에 문제가 있어도 뉴스레터 생성이 실패하지 않게 하고, 무엇을 읽었는지 로그로 남긴다(spec FR-1.4, FR-1.5, FR-4.1, NFR-2).
- **Required**: Yes · **Depends**: T-002 · **Ref**: plan §4.3, §5.1
- **작업**:
  - 파일이 없으면 조용히 건너뛴다(세지 않는다).
  - `IOException`, `IllegalArgumentException`, `DateTimeException`은 잡아서 `warn("지난 뉴스레터를 읽지 못해 건너뜁니다: {파일} - {이유}")`를 남기고 건너뜀 수를 늘린다. 다른 예외는 잡지 않는다.
  - 끝나면 `info("지난 뉴스레터: {시작} ~ {끝} 중 {읽은 수}개 파일에서 URL {개수}개 (건너뜀 {수})")`를 남긴다. N=0이면 기능이 꺼졌다는 로그 한 줄을 남긴다.
  - `PublishedHistoryTest`에 다음을 추가한다: 카테고리가 빠진 깨진 파일 1개가 섞여도 예외 없이 나머지 URL은 모두 모인다, `news/` 폴더가 없으면 빈 집합이다, CRLF 파일도 읽힌다.
- **완료 조건**: 추가 케이스가 통과한다(spec SC2).

## G3. 정제 단계에서 빼기 (Low)

### T-004 `ArticleCleaner`: 지난 URL 빼기와 정제 로그
- **Purpose**: 정규화한 URL이 지난 뉴스레터에 있던 기사를 후보에서 빼고, 몇 건을 뺐는지 남긴다(spec FR-2.1, FR-2.2, FR-4.2).
- **Required**: Yes · **Depends**: — · **Ref**: plan §3.2, §4.3, §5
- **작업**:
  - `clean(List<Article> articles, Set<URI> published)`를 추가한다. 판정 순서는 정규화 실패 → 기간 밖 → **지난 뉴스레터** → 중복이고, 먼저 걸린 조건 하나로만 센다.
  - 기존 `clean(List<Article>)`는 `clean(articles, Set.of())`에 위임한다.
  - 정제 로그를 `정제: A건 → B건 (허용되지 않는 링크 x, 기간 밖 y, 지난 뉴스레터 z, 중복 w)`로 바꾼다.
  - 클래스 Javadoc의 처리 목록에 새 단계를 추가한다.
  - `ArticleCleanerTest`에 다음을 추가한다: 지난 URL과 같은 기사는 카테고리가 달라도 빠진다, `utm_*`가 붙은 판도 정규화 후 빠진다, 제목만 같고 URL이 다르면 빠지지 않는다, 빈 집합이면 `clean(list)`와 결과가 같다.
- **완료 조건**: 추가 케이스와 기존 `ArticleCleanerTest`가 모두 통과한다(spec SC3).

## G4. 파이프라인 연결·검증 (Medium)

### T-005 `DailyNewsPipeline`에 연결
- **Purpose**: 실제 실행에서 지난 URL 집합을 만들어 정제기에 넘긴다(spec FR-1.6, FR-2.3, FR-2.4).
- **Required**: Yes · **Depends**: T-001, T-002, T-003, T-004 · **Ref**: plan §3.1, §5
- **작업**:
  - 생성자에서 `new PublishedHistory(config.historyDays(), new MarkdownNewsletterReader())`를 만든다.
  - `run()`에서 `OutputPaths paths = new OutputPaths(root)`를 수집 전에 만든다. 수집 뒤 `published = history.urlsBefore(paths, date)`를 구하고 `cleaner.clean(collected.articles(), published)`를 호출한다. `write()`에는 같은 `paths`를 넘긴다.
  - `--dry-run`에서도 URL 모으기를 실행한다(읽기만 한다).
  - 클래스 Javadoc의 흐름 설명에 "지난 뉴스레터 URL 빼기"를 추가한다.
- **완료 조건**: 컴파일되고 기존 `PipelineIntegrationTest`, `PipelineFailureTest`, `DailyNewsAppTest`가 통과한다.

### T-006 통합 테스트: 이어지는 날짜 0건·같은 날 재실행
- **Purpose**: 42분 간격 재실행과 같은 최악의 경우에도 겹치는 기사가 0건임을 전체 흐름으로 검증한다(spec SC4, G2, G3).
- **Required**: Yes · **Depends**: T-005 · **Ref**: plan §9.2
- **작업**:
  - `PipelineIntegrationTest`에 테스트를 추가한다. 같은 `root`, 같은 `FixtureWeb.CLOCK`으로 2026-10-27을 실행한 뒤 2026-10-28을 실행한다. 둘째 뉴스레터의 원문·같은 사안 URL과 첫째 URL의 교집합이 비어 있고 종료 코드는 0이어야 한다.
  - 기존 `두_번_실행해도_결과_파일이_바이트_단위로_같다_FR_7_4`가 그대로 통과하는지 확인한다(당일 파일을 읽지 않는다는 회귀 검증).
  - `이전_31일치가_있으면…` 테스트가 만드는 `TestNewsletters` URL이 fixture 기사 URL과 겹치는지 확인한다. 겹치면 보관 정리 검증 의도가 유지되도록 fixture 쪽을 조정하고 Notes에 기록한다.
- **완료 조건**: 새 테스트와 기존 통합 테스트가 모두 통과한다.

### T-007 회귀 확인: 전체 테스트·골든 파일
- **Purpose**: 출력 형식과 기존 동작이 바뀌지 않았음을 확인한다(spec SC7, FR-5).
- **Required**: Yes · **Depends**: T-006 · **Ref**: plan §9.3
- **작업**:
  - `./gradlew test`를 실행한다.
  - `git diff --stat`으로 `src/test/resources/golden/`과 출력 템플릿이 바뀌지 않았는지 확인한다.
- **완료 조건**: 전체 테스트가 통과하고(통과 수를 Notes에 기록) 골든 파일 변경이 없다.

### T-008 실제 데이터 재현(2026-11-04)
- **Purpose**: 실제 피드와 실제 `news/`로, 문제가 된 2026-11-04가 지난 7일과 겹치지 않게 만들어지는지 확인한다(spec SC6).
- **Required**: Yes · **Depends**: T-007 · **Ref**: plan §9.4
- **작업**:
  - scratchpad에 임시 root를 만들고 저장소의 `news/`를 복사한다. `./gradlew run --args="--root <임시> --date 2026-11-04"`를 실행한다(저장소 파일은 건드리지 않는다).
  - 스크립트로 새 `2026-11-04.md`의 URL과 10-28 ~ 11-03 URL의 교집합을 센다.
  - 로그의 `지난 뉴스레터` 줄(파일 수, URL 수)과 정제 줄의 `지난 뉴스레터 z`, 카테고리별 선정 수를 Notes에 기록한다.
- **완료 조건**: 교집합이 0건이고, 지난 기사를 뺀 수가 0보다 크다.

## G5. 문서·배포 (Low)

### T-009 문서 갱신(README·기존 spec FR-3.4)
- **Purpose**: 바뀐 선정 규칙을 사용자 문서와 기존 명세에 반영한다(plan §10).
- **Required**: Yes · **Depends**: T-005 · **Ref**: plan §10
- **작업**:
  - `README.md` "어떻게 만드나요 → 선정"에 "최근 7일 뉴스레터에 이미 실린 기사는 빼고"를 추가한다. `<!-- LATEST -->` 블록 밖만 고친다.
  - `docs/feature/daily-news/spec.md` FR-3에 FR-3.4를 추가하고 이 명세로 링크한다.
- **완료 조건**: 두 문서가 이 작업의 동작(7일, 당일 제외, `historyDays: 0`으로 끄기)과 맞는다.

### T-010 PR·병합·첫 실행 로그 확인
- **Purpose**: main에 반영하고, 실제 일일 실행에서 기능이 동작하는지 확인한다.
- **Required**: Yes · **Depends**: T-007, T-008, T-009 · **Ref**: plan §10
- **작업**:
  - `feat/history-dedup` → `main` PR을 만들고 CI 통과를 확인한다. 병합은 사용자가 승인한다.
  - 병합 뒤 첫 일일 실행(Actions `daily-news`) 로그에서 `지난 뉴스레터` 두 줄을 확인한다.
  - 새로 만든 뉴스레터의 URL이 직전 7일과 겹치지 않는지 T-008 스크립트로 확인한다.
- **완료 조건**: 병합 완료. 첫 실행 로그와 겹침 0건 결과를 Notes에 기록한다.

## G6. 선택·후속 (Low)

### T-011 이어지는 사안 중복 관찰(1주)
- **Purpose**: 범위 밖으로 둔 "URL은 다르지만 어제와 같은 사안" 중복이 실제로 거슬리는 수준인지 데이터로 판단한다(spec C3).
- **Required**: No · **Depends**: T-010 · **Ref**: spec §7 C3
- **작업**:
  - 병합 뒤 7일 동안 이웃한 날짜끼리 대표 기사 제목을 비교한다(`TitleSimilarity`로 `clusterThreshold` 이상인 쌍을 센다).
  - 결과와 함께, 다음 기능(어제 사안과 비슷한 사안 감점 또는 제외)이 필요한지 의견을 Notes에 남긴다.
- **완료 조건**: 7일 치 관찰 결과가 기록되어 있다.
