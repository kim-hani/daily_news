# Daily News — 작업 목록 (Tasks)

> 기준: [`spec.md`](./spec.md), [`plan.md`](./plan.md) · 브랜치: `feature/daily-news` · 작성일: 2026-10-28

## Summary
| 그룹 | 이름 | 작업 범위 | 복잡도 | 작업 수 |
|---|---|---|---|---|
| G1 | 프로젝트 기반 | T-001 ~ T-003 | Low | 3 |
| G2 | 모델·설정 | T-004 ~ T-005 | Low | 2 |
| G3 | HTTP 계층 | T-006 ~ T-008 | Medium | 3 |
| G4 | RSS 수집 | T-009 ~ T-011 | Medium | 3 |
| G5 | 정제·중복 제거 | T-012 ~ T-015 | Medium | 4 |
| G6 | 주요 기사 선정 | T-016 ~ T-019 | High | 4 |
| G7 | 본문 요약 | T-020 ~ T-023 | High | 4 |
| G8 | 오늘의 키워드 | T-024 | Low | 1 |
| G9 | 출력·보관 | T-025 ~ T-029 | Medium | 5 |
| G10 | 앱 조립·검증 | T-030 ~ T-034 | Medium | 5 |
| G11 | 배포·운영 | T-035 ~ T-038 | Medium | 4 |
| G12 | 선택 개선 | T-039 | Low | 1 |

- **총 39개 작업** / 필수 38개 / 선택 1개
- 진행 원칙: 작업 하나 = 테스트로 확인할 수 있는 결과 하나. 모든 작업은 `feature/daily-news` 브랜치에서 한다. main에는 PR로만 반영한다(FR-8.0).
- 공통 완료 조건: `./gradlew test` 통과(G1 이후)

## Progress
| ID | Task | Group | Required | Status | Notes |
|---|---|---|---|---|---|
| T-001 | Gradle 프로젝트 초기화 | G1 | Yes | Done | Wrapper 9.8.2, `./gradlew build` 성공, KOMORAN 포함 의존성 해석 확인. plan 대비: Gradle 9 문법에 맞춰 `maven { url = … }` 사용, 로그 API용 `slf4j-api` implementation 추가, `liveTest`에 testClassesDirs/classpath 지정(Gradle 9 필수), `gradlew` 줄바꿈 고정용 `.gitattributes` 추가 |
| T-002 | 패키지 구조·앱 진입점 골격 | G1 | Yes | Done | 빈 패키지가 git에 남도록 패키지마다 `package-info.java`(역할 설명)를 둠. `./gradlew run` 종료 코드 0, `DailyNewsAppTest` 1개 통과. 참고: Windows Git Bash에서는 한글 로그가 깨져 보임(콘솔 인코딩 문제, CI의 ubuntu에서는 해당 없음 → T-034에서 재확인) |
| T-003 | PR 테스트 워크플로(ci.yml) | G1 | Yes | Done | YAML 파싱 검증 완료(로컬 actionlint·Docker 없음 → actionlint는 미실행). PR #1에서 `ci / test` 통과 확인(36초). PR 중복 실행 취소용 `concurrency` 추가. 주의: Windows(`core.filemode=false`)에서 커밋 시 `git add --chmod=+x gradlew`로 실행 권한을 넣어야 CI가 동작함 |
| T-004 | 도메인 모델(Category·record) | G2 | Yes | Done | `CategoryTest` 3개, `ModelImmutabilityTest` 5개 통과. `Source.feeds`/`itKeywordFilterFeeds`도 Category 순서(EnumMap/EnumSet)로 복사해 불변으로 둠 |
| T-005 | sources.yml·ConfigLoader | G2 | Yes | Done | `ConfigLoaderTest` 25개 통과(잘못된 설정 19가지 포함). **피드 수는 15개**: spec FR-2.1 표의 URL이 3사×5카테고리=15개라 15개로 검증하고, plan·tasks에 "14개"로 잘못 적힌 곳(5군데)을 15개로 고침. YAML 원본은 `SourceConfig`(package-private)로 읽고 검증 후 `model.Source`로 변환. 오류는 `ConfigException`에 `파일경로: sources[yonhap].feeds.IT: …` 형태로 위치를 담음(첫 오류에서 중단). `creditPatterns`·`itKeywordFilterFeeds`·`itKeywords`·`stopwords`는 생략 가능, 나머지는 필수. 오타 항목·알 수 없는 카테고리 키도 오류로 처리. userAgent의 저장소 주소를 `kim-hani/daily_news`로 확정 |
| T-006 | HttpFetcher·JdkHttpFetcher·FakeHttpFetcher | G3 | Yes | Done | `FakeHttpFetcherTest` 4개(200·404·500·예외·fixture), `JdkHttpFetcherTest` 7개(로컬 `HttpServer`: UA, 리다이렉트, 5xx·시간 초과 1회 재시도, 4xx 미재시도) 통과. 재시도 전 1초 대기(`Sleeper` 주입, 같은 서버에 바로 재요청하지 않도록). 4xx·5xx는 예외 없이 `FetchResult`로, 연결 실패·시간 초과는 `IOException`으로 구분 |
| T-007 | HostThrottle(호스트별 요청 간격) | G3 | Yes | Done | `HostThrottleTest` 5개 통과(같은 호스트 지연, 다른 호스트 무지연, 남은 시간만 대기, 호스트 대소문자 무시, 실패 요청 뒤에도 간격 유지). 간격은 직전 요청이 **끝난** 시각부터 잼. 순차 요청 전제라 스레드 안전하지 않음(문서화) |
| T-008 | RobotsPolicy(robots.txt 준수) | G3 | Yes | Done | `RobotsRulesTest` 13개(최장 일치, 동률 시 Allow, `*`·`$`, 다중 UA 그룹, `*` 그룹 병합, 빈 줄, `Disallow :`, 대소문자, BOM), `RobotsPolicyTest` 11개 통과. 실제 robots.txt 5개(연합·동아·rss.donga·경향·서울, 2026-10-28 수집)를 `fixtures/robots/`에 저장해 검증하고, 기본 설정 피드 15개가 모두 허용되는지도 확인. plan 대비: 404뿐 아니라 **4xx 전체를 전부 허용**으로 처리(RFC 9309 기준). 규칙 해석은 `RobotsRules`(package-private)로 분리. `*` 그룹만 보고 봇 이름별 그룹은 무시(plan대로), 퍼센트 인코딩은 정규화 없이 그대로 비교 |
| T-009 | 테스트 fixture 수집 | G4 | Yes | Done | 정치 피드 RSS 3개(앞 item 15개만 남김)와 기사 HTML 6개(본문 컨테이너의 앞 4문단과 잡음 요소만 남김, 4~5KB), robots.txt 5개(T-008), `fixtures/README.md`(출처·수집일·축소 방식) 저장. `FixturesTest` 18개 통과(UTF-8 디코딩, item 10개 이상). 공통 헬퍼 `Fixtures`를 추가하고 `FakeHttpFetcher`가 이를 쓰도록 정리. **리스크**: 경향 기사 페이지는 1초 간격 연속 요청에 403을 돌려주고 1~2분 뒤 풀림(빈도 제한). 운영 시 경향 본문이 RSS 요약으로 대체될 수 있음 → T-034에서 호스트별 간격 조정 검토. 참고: 동아 본문은 `<p>` 없이 텍스트+`<br>`, 경향 첫 문단은 소제목(`b.strapline`)이라 G7에서 처리 필요 |
| T-010 | RssParser(날짜·CDATA 처리) | G4 | Yes | Done | `RssParserTest` 10개 통과(fixture 3종 건수 15·첫 item 제목/링크/KST 시각, 경향 `dc:date`, 동아 description HTML 제거, UTC→KST, CDATA 공백·줄바꿈 정리, 날짜·제목·링크 누락/오류 item 건너뜀, 깨진 XML·HTML 응답은 `RssParseException`). 링크는 원본 그대로 넘기고(경향 utm 포함) 정규화는 T-012에서. `javascript:` 등 http(s)가 아닌 링크도 여기서 건너뜀. 공백 정리는 NBSP 포함 |
| T-011 | FeedCollector(실패 피드 skip) | G4 | Yes | Done | `FeedCollectorTest` 6개 통과(정상 225건·카테고리별 45건, 언론사×카테고리 순서, 500 피드 1개만 실패 기록, 연결 실패·HTML 응답도 해당 피드만 건너뜀, robots 차단 피드는 요청 없이 실패 기록, 전부 실패 시 기사 0건·실패 15건). 결과는 `CollectResult(articles, failedFeeds)`, 실패 피드는 `yonhap/POLITICS` 형식. 로그: 피드별 건수 INFO, 실패 사유 WARN, 합계 INFO(NFR-8). 전부 실패 시 종료 코드 1 처리는 T-030에서 |
| T-012 | UrlNormalizer | G5 | Yes | Done | `UrlNormalizerTest` 18개 통과(경향 utm·끝 `/` 제거, `&amp;` 디코딩, fbclid 제거, 소문자·fragment·기본 포트 제거, 하위 도메인 허용, 정규화된 URL 불변, `javascript:`·ftp·다른 도메인·유사 도메인·상대 경로·깨진 URL 거부). plan 대비: 언론사 도메인을 피드 호스트로 추정할 수 없어(`rss.donga.com` vs `www.donga.com`) `sources.yml`에 언론사별 `domains`를 추가하고 `Source.domains`·`ownsHost()`로 판정. 피드 호스트도 `domains`에 속해야 하도록 설정 검증 추가(`ConfigLoaderTest` 28개). 경로 끝 `/`도 제거(경향 RSS 링크와 guid를 같은 URL로 보기 위해) |
| T-013 | TitleNormalizer | G5 | Yes | Done | `TitleNormalizerTest` 11개 통과(말머리 `[단독]`·`[속보]`·`【】`·`<>`·꼬리표 `[정치를 부탁해]`, 연합 갱신 표시 `(종합2보)`·`(2보)`, 따옴표 변형이 같은 정규형). `normalize()`(비교용: 특수문자 제거·소문자)와 `clean()`(키워드용: 대소문자·문장부호 유지) 두 형태로 제공. plan 대비: 연합 `(종합)` 류 갱신 표시도 지워 같은 기사의 갱신본이 중복으로 합쳐지게 함 |
| T-014 | ArticleCleaner(24h·중복 제거) | G5 | Yes | Done | `ArticleCleanerTest` 6개 통과(정확히 24시간 전 포함·1초 초과 제외·미래 제외, URL 정규화·허용 안 된 링크 제거, 같은 카테고리 URL 중복과 같은 언론사 정규화 제목 중복은 가장 이른 기사만 유지, 카테고리가 다르면 합치지 않음, 입력 순서 유지). 카테고리 간 중복은 T-019 선정 단계에서 처리(plan §3 원칙). 정제 결과 건수·사유를 로그로 남김. `Article.withUrl()`과 테스트 헬퍼 `TestArticles` 추가 |
| T-015 | CategoryFilter(IT 키워드) | G5 | Yes | Done | `CategoryFilterTest` 14개 통과(연합 산업 피드는 제목·설명에 키워드가 있는 기사만, 다른 피드는 영향 없음, `AI`가 `MAIN`·`PAID`에 안 걸리고 `AI가`·`(AI)`·소문자 `ai`는 맞음, `5G망` 맞음·`15G` 안 맞음, 키워드 목록이 비면 지정 피드 기사 모두 제외). 모든 키워드에 "앞뒤 영문자·숫자 없음" 경계를 적용(한글 조사 결합은 허용) |
| T-016 | KomoranAnalyzer(명사 추출 래퍼) | G6 | Yes | Done | `KomoranAnalyzerTest` 6개 통과("삼성전자 반도체 투자 확대" → 삼성전자·반도체·투자·확대, 조사 제거, 영문 약어, 1글자 제외, 싱글턴). FULL 모델 사용(로딩 약 3.3초, 한 번만). plan 대비: NNP·NNG에 더해 **영문(SL, 예: SCM·AI)**도 명사로 포함(제목 유사도와 키워드에 중요). `nouns()`는 동시 호출에 대비해 synchronized |
| T-017 | TitleSimilarity | G6 | Yes | Done | `TitleSimilarityTest` 13개 통과. fixture 실제 제목으로 같은 사안 쌍 5개는 0.4 이상(0.455~0.75), 다른 사안 쌍 5개는 0.4 미만(최대 0.19). 사전 탐색에서 같은 사안 쌍 8개 중 7개가 0.4 이상이었고, 0.36인 1쌍도 같은 사안의 다른 제목을 거쳐 묶임 → 임계값 0.4 유지. 2-gram은 공백을 뺀 문자열로 만들고, 비교 특징(`Features`)을 한 번만 계산해 재사용 |
| T-018 | StoryClusterer | G6 | Yes | Done | `StoryClustererTest` 4개 통과(3사 사안 1개 + 단독 2개 → 클러스터 3개, outletCount 3·1·1, 같은 언론사 여러 건은 한 번만 셈, 카테고리별 분리). plan 대비: 순수 greedy 방식은 기사 순서에 따라 같은 사안이 둘로 갈리는 문제가 실제 fixture 제목에서 확인돼(연합-동아 0.36, 둘 다 경향과 0.4 이상), 새 기사가 여러 클러스터와 이어지면 **클러스터를 합치는 single-link**로 구현(연결 요소와 같고 순서 무관) |
| T-019 | StoryRanker·StorySelector | G6 | Yes | Done | `StoryRankerTest` 5개 + `StorySelectorTest` 4개 통과(점수 = 언론사 수×10 + 최신성, 최신성 0~1 범위, 동점이면 기사 수, 대표는 가장 이른 보도·동시각이면 설정 순서, related는 다른 언론사마다 가장 이른 1건, Top 5, 후보 3개면 3개, 빈 카테고리도 결과에 포함, 앞 카테고리 선정 URL 제외, 글로벌을 IT보다 먼저). 앞 카테고리에서 쓰인 URL은 대표·related뿐 아니라 클러스터 구성원 전체를 제외. 동점 정렬 마지막 기준(최근 대표 → URL)을 추가해 결과가 항상 같게 함 |
| T-020 | BodyExtractor | G7 | Yes | Done | `BodyExtractorTest` 11개 통과(fixture 6개 모두 본문 80자 이상·사진 설명 제외, 연합은 부제목·관련뉴스·저작권 문단 제외, 동아는 `<br>`로 문단 분리·중간 제목 `●` 제외, 경향은 소제목 `b.strapline` 제외, 본문 요소 없으면 빈 목록, Content-Type charset(EUC-KR) 디코딩). 잡음 요소 목록에 언론사 fixture에서 확인한 클래스(`.tit-sub`, `.photo-group`, `.art_photo`, `.editor-subtitle`, `.strapline`, `.related-zone`, `.txt-copyright`, `.link-box`)를 추가. selector가 문단 자체를 고르는 경우(연합) 그 문단이 잡음이면 건너뜀 |
| T-021 | SentenceSplitter | G7 | Yes | Done | `SentenceSplitterTest` 7개 통과(다·요·?·! 뒤 공백, 굽은/곧은 따옴표 안에서는 안 나눔, 닫는 따옴표로 끝나는 문장, 소수점·날짜, U.S.·Mr.·이니셜, 작은따옴표 인용과 영문 아포스트로피 구분) |
| T-022 | LeadSummarizer | G7 | Yes | Done | `LeadSummarizerTest` 19개 통과(fixture 6개 모두 BODY·200자 이하·크레디트/이메일/저작권 없음, **SC5: 요약 각 문장이 정제 본문의 부분 문자열**, 120자/3문장 규칙, 200자 초과 시 마지막 문장 제거, 긴 첫 문장은 199자+`…`, `(서울=뉴스1) … 기자 =`·`[언론사]`·이메일 제거와 `▶`·`ⓒ`·무단 전재 문단 제거, 본문 40자 미만이면 RSS, 둘 다 없으면 NONE). 언론사별 패턴 외에 일반 크레디트 패턴 `(지역=언론사) … 기자 =`도 적용. fixture에서 **연합 크레디트가 기자 2명(`김효정 전명훈 기자 =`)이라 기존 패턴이 못 지우는 문제**를 발견해 `sources.yml` 패턴을 `[^=]{0,40}?(기자|특파원)`로 넓힘. RSS 설명문은 끝이 잘린 조각(`...`, `···`)을 다른 문장이 있으면 쓰지 않음 |
| T-023 | SummaryService(본문 요청→요약 연결) | G7 | Yes | Done | `SummaryServiceTest` 3개 통과(정상 기사는 BODY, 404·robots 차단(연합 `/view/AEN…`)은 RSS 대체, bodyOk 2·bodyFallback 2, 사안·순서 유지, 원문 요청 수 ≤ 선정 기사 수, 차단 URL은 요청 안 함). 결과는 `SummaryService.Result(sections, bodyOk, bodyFallback)`. 본문을 못 찾으면 selector와 함께 WARN 로그(사이트 구조 변경 감지용) |
| T-024 | KeywordExtractor | G8 | Yes | Done | `KeywordExtractorTest` 5개 통과(빈도순·동률이면 고유명사 다음 가나다순, 불용어 대소문자 무시, 한 제목 안 중복은 1회, 최대 limit개, 실제 KOMORAN으로 "반도체"가 1위·불용어 "기자"와 말머리 "속보" 제외). plan 대비: 빈도는 **명사가 나온 제목 수**로 셈(한 제목에서 반복된 명사가 부풀리지 않게). 입력 제목은 `TitleNormalizer.clean()`(대소문자 유지)을 써서 "AI" 같은 영문 키워드가 소문자로 바뀌지 않게 함. 테스트를 위해 명사 추출 함수를 생성자로 주입(앱에서는 `KomoranAnalyzer::nouns`) |
| T-025 | OutputPaths·MarkdownRenderer | G9 | Yes | Done | `MarkdownRendererTest` 4개 통과(golden file `src/test/resources/golden/newsletter.md` 일치, 기사 5건 모두 `- 원문: https://`(SC3-1), `[ ] * _ # < >` 이스케이프, `news/YYYY/MM/YYYY-MM-DD.md` 덮어쓰기). 헤더에 수집 실패 피드도 표시(NFR-8), 빈 카테고리는 안내 문구, RSS 요약은 `_(RSS 요약)_` 표기, 같은 사안 링크의 괄호는 `%28/%29`로 인코딩. 모델 변경: 렌더러가 `Newsletter`만으로 그릴 수 있게 `Newsletter.sourceNames` 추가. golden file 비교가 Windows `core.autocrlf`에 깨지지 않게 `.gitattributes`에 `src/test/resources/** eol=lf` 추가 |
| T-026 | Thymeleaf 템플릿·CSS·HtmlRenderer | G9 | Yes | Done | `HtmlRendererTest` 9개 통과(섹션 5개·순서, 카드 5개 모두 `a[href^=https]`와 URL 텍스트, `<script>` 제목·`<b>` 요약이 텍스트로 출력·인라인 스크립트 없음, `lang=ko`·header/nav/main/article/footer·viewport, 배지·시각(`time[datetime]`)·RSS 요약 표기·같은 사안 링크, 키워드 칩·실패 피드, 날짜별 페이지는 `../assets/` 참조, index·archive·assets 파일 생성). 템플릿 `layout.html`(head·header·footer 조각)·`newsletter.html`·`archive.html`, `th:text`만 사용. CSS: 색 토큰·다크모드·760px·16px·`overflow-wrap:anywhere`·탭 sticky. 템플릿에는 표시용 문자열을 미리 만든 view record(`Page/Section/Card/Link`)를 넘김 |
| T-027 | RetentionCleaner(30일 보관) | G9 | Yes | Done | `RetentionCleanerTest` 5개 통과(31개 중 가장 오래된 1개만 삭제·30일째 보존, 패턴 외·잘못된 날짜·미래 날짜·하위 폴더 보존, `newsDays: 0`이면 Markdown 보존, `newsDays > 0`이면 Markdown 삭제와 빈 월·연 폴더 정리, 폴더가 없어도 동작). 삭제 목록은 INFO 로그 |
| T-028 | ArchiveRenderer | G9 | Yes | Done | `ArchiveRendererTest` 3개 통과(서로 다른 월 3개 파일 → 연월 그룹 2개·최신순, 패턴 외 파일 제외, 보관 정리로 지운 날짜는 목록에 없음, 30일 이전은 저장소 Markdown 아카이브(`repositoryUrl/tree/main/news`)로 안내). 설정에 `repositoryUrl` 추가 |
| T-029 | ReadmeUpdater·README 초안 | G9 | Yes | Done | `ReadmeUpdaterTest` 5개 통과(마커 안만 교체, 마커 없으면 끝에 추가, 두 번 실행해도 동일(멱등), 날짜·웹 주소·Markdown 링크·선정 건수·키워드 포함, README가 없으면 생성). 저장소 루트 `README.md` 초안(소개, 웹 주소, 마커, 동작 방식, 실행 방법, 문서 링크) 작성. 설정에 `siteUrl` 추가(`https://kim-hani.github.io/daily_news/`) |
| T-030 | DailyNewsApp CLI·파이프라인 조립 | G10 | Yes | Done | `CliOptionsTest` 3개 + `DailyNewsAppTest` 5개 통과(기본값·모든 옵션 파싱, 알 수 없는 옵션·값 없는 옵션·잘못된 날짜 → 사용법 로그와 종료 코드 1, 설정 파일 없음·잘못된 값 → 1, `--dry-run`이면 파일 0개, `--date` 경로, 날짜 생략 시 실행일 KST). 구조: `DailyNewsApp`(옵션·설정·종료 코드) → `DailyNewsPipeline`(생성자에 `AppConfig`·`HttpFetcher`·`Clock` 주입, 단계 조립) → `Result(exitCode, newsletter)`. 실제 실행은 `HostThrottle(JdkHttpFetcher)`. 종료 코드 0/1/2, 결과 요약·카테고리별 선정 제목·키워드·소요 시간을 로그로 남김(NFR-8). 키워드는 정제된 전체 기사 제목에서 추출 |
| T-031 | PipelineIntegrationTest | G10 | Yes | Done | `PipelineIntegrationTest` 7개 통과. `FixtureWeb`(robots 4개, 피드 15개 → 언론사 RSS fixture, RSS의 모든 기사 링크 → 언론사 기사 HTML fixture)과 `@TempDir`, 고정 시각(2026-10-28 22:00 KST)으로 전체 실행. 확인: news·site/index·archive 페이지·archive.html·assets·README 마커, 카테고리 5개 모두 1~5건(SC3), 카드·Markdown 모든 기사에 원문 URL(SC3-1), 본문 요약 비율 ≥ 90%(실측 25/25, SC4), 카테고리 간 같은 기사 없음, **두 번 실행 시 모든 파일 바이트 동일**(FR-7.4), 이전 31일치를 미리 넣으면 웹 30개만 남고 Markdown 32개 모두 유지·목록에서 삭제 날짜 제외(SC9). 실측 로그: 수집 225 → 정제 220 → 선정 25, 정치 1·2위는 3사 공동 보도 사안 |
| T-032 | 장애 시나리오 테스트 | G10 | Yes | Done | `PipelineFailureTest` 3개 통과. (a) 연합 피드 5개 모두 500 → 종료 코드 0, `failedFeeds` 5건, 5개 카테고리 모두 기사 있음, Markdown에 "수집하지 못한 피드" 표시(SC6). (b) 기사 본문 모두 404 → 종료 코드 0, 요약 전부 RSS, 웹에 "RSS 요약" 표시. (c) 피드 전부 503 → 종료 코드 1, `news/`·`site/` 미생성, 기존 README 그대로(NFR-2) |
| T-033 | LiveFeedSmokeTest·liveTest 태스크 | G10 | Yes | Done | `@Tag("live")` `LiveFeedSmokeTest`(피드 15개: robots 허용·200·item 1개 이상, 피드별 첫 기사: robots 허용·200·본문 80자 이상). 운영과 같은 요청기(UA·호스트별 간격·robots). `./gradlew liveTest` 로컬 통과(15개 중 성공 10, 건너뜀 5), `./gradlew test`에서는 제외 확인. **실측**: 경향 기사 페이지는 연속 요청 시 IP를 몇 분간 403으로 막음(1초·10초 간격 모두, 첫 1~2건만 성공). 빈도 제한은 구조 변경이 아니므로 403·429는 실패 대신 assumption으로 **건너뜀** 처리. 1차 실행에서 IT 기사 1건이 200인데 본문 0자였으나 직후 403으로 바뀌어 차단 페이지로 판단 |
| T-034 | 로컬 실제 실행·화면 점검·임계값 조정 | G10 | Yes | Done | 실제 실행 2회(출력은 scratchpad `--root`). **실행 시간**: 파이프라인 114초(Gradle 포함 125~132초, NFR-1 충족). **결과**: 수집 1,100건 → 정제 999건(기간 밖 91·중복 10) → 선정 25건, 2곳 이상 보도 사안 20/25, 정치·사회 상위는 3사 공동 보도. **본문 요약 비율**: 1차 18/25(72%) → 보정 후 20/25(80%). 미달 원인은 경향 403(IP 빈도 제한) 3건과 연합 [속보](본문 없음) 2건 → [속보]는 대표에서 제외하도록 보정, 경향은 T-038에서 관찰. **조정한 값**: ① `hostDelayMillis`(신규 설정) 경향 10초 ② IT 키워드 필터를 동아·경향 과학 피드에도 적용(날씨·제약 기사 제거)+IT 키워드 14개 추가 ③ 불용어 17개 추가(대통령·내년·분기 등) ④ 글자 10자 미만 요약은 NONE(연합 속보 RSS 설명문이 "(" 한 글자) ⑤ 대표 기사 선정에서 [속보] 후순위 ⑥ RSS 링크의 공백 인코딩(동아 `/news/K-TECH Global Leaders/…` 링크가 버려지던 문제) ⑦ `./gradlew run` 로그 UTF-8 출력(Windows 한글 깨짐). 클러스터 임계값 0.4는 실제 데이터에서 3사 묶음이 잘 되어 유지. **화면**: 헤드리스 Chrome으로 360px iframe에서 라이트·다크·JS 끔 모두 `scrollWidth 345 = clientWidth 345`(가로 스크롤 없음, SC7), 스크린샷으로 라이트·다크 가독성 확인. "같은 사안" 구분점 밑줄 CSS 수정. **한계**: 경향 RSS 설명문 앞에 사진 설명이 붙어 RSS 대체 요약 품질이 낮음 |
| T-035 | 일일 워크플로(daily-news.yml) | G11 | Yes | Done | `.github/workflows/daily-news.yml` 작성, YAML 구조 검증(트리거 schedule `0 0 * * *`+workflow_dispatch, 최상위 `permissions: {}`, job별 최소 권한, concurrency). plan §10.3 그대로: checkout(main, 배포 키) → JDK 21 → setup-gradle → `./gradlew run` → `git add -A news site README.md` → 변경 없으면 종료 → 커밋(`news: YYYY-MM-DD daily newsletter`, 작성자 `vars.NEWS_COMMIT_NAME/EMAIL`, TZ=Asia/Seoul) → pull --rebase 후 push 3회 재시도 → upload-pages-artifact → deploy-pages. plan 대비: 앞에 **`check` job**을 추가해 배포 키·Variables가 없으면 실패 대신 경고를 남기고 건너뜀(설정 전 매일 실패 알림 방지). 비밀값은 env로만 전달하고 출력하지 않음. 실제 동작 확인은 T-037(사용자 설정 후) |
| T-036 | GitHub 1회 설정 가이드 | G11 | Yes | Done | `docs/feature/daily-news/setup-guide.md` 작성: 저장소·브랜치, 배포 키 생성·등록(공개키 write, 개인키 Secret `NEWS_DEPLOY_KEY`), Variables(noreply 이메일 확인법), Pages Source=GitHub Actions, main Ruleset(PR 필수·`test` 체크·force push 차단·삭제 금지·Bypass Deploy keys), Bypass 메뉴가 없을 때 PAT 대안(R3), 수동 실행 방법, 11개 항목 체크리스트, 문제 해결 표. 웹 화면 경로와 `gh` CLI 명령을 함께 제공. 사용자 결정에 따라 실제 설정은 사용자가 수행 |
| T-037 | 첫 PR 병합·수동 실행 E2E | G11 | Yes | Blocked | **사용자 수행 대기**(사용자 결정: GitHub 설정은 가이드만 작성). 코드 PR 병합은 이번 작업에서 수행. 남은 일: `setup-guide.md` 2~5단계(배포 키·Variables·Pages·Ruleset) 설정 → Actions에서 daily-news 수동 실행 → 체크리스트 확인(뉴스 커밋·작성자·Pages URL·잔디·main 직접 push 거부). Pages 예정 주소: https://kim-hani.github.io/daily_news/ |
| T-038 | 7일 연속 운영 관찰 | G11 | Yes | Blocked | **T-037 이후 7일 운영 필요**(현재 시점에 수행 불가). 매일 기록할 항목: 예약 실행 시작·커밋 시각(10:00 이전), 잔디, 카테고리별 기사 수, 본문 요약 비율(경향 빈도 제한 영향 주시), 실패 피드, Actions 사용 시간 |
| T-039 | JS 탭 전환(점진적 향상) | G12 | No | Done | T-026에서 함께 구현(`static/tabs.js`, 템플릿 ARIA). `role=tablist/tab/tabpanel`, `aria-controls`·`aria-labelledby`·`aria-selected`, roving tabindex, ←/→(순환)·Home·End, 클릭 시 `#해시` 갱신, 주소의 해시로 초기 탭 선택. JS가 없으면 앵커 이동·모든 섹션 표시. 렌더 테스트(`HtmlRendererTest` 탭 ARIA 1개)와 헤드리스 Chrome 점검(초기 정치 → → 사회 → End 글로벌 → → 정치(순환) → ← 글로벌, 클릭 경제·`#economy`, 선택 1개, tabindex 0 하나, JS 끔이면 패널 5개 모두 표시)으로 확인. 스크립트는 인라인 대신 별도 파일(`assets/tabs.js`, defer)로 두어 템플릿 처리와 분리 |

---

## G1. 프로젝트 기반 (Low)

### T-001 Gradle 프로젝트 초기화
- **Purpose**: JDK 21 / Gradle Groovy DSL 빌드 환경을 갖춰 이후 모든 작업이 빌드·테스트될 수 있게 한다.
- **Required**: Yes · **Depends**: — · **Ref**: plan §10.1, §11
- **작업**:
  - Gradle Wrapper 9.8.2를 만든다.
  - `settings.gradle`: foojay resolver 1.0.0, `rootProject.name = 'daily_news'`
  - `build.gradle`: plan §11의 의존성, JitPack 저장소, toolchain 21, UTF-8, `test`에서 `live` 태그 제외
  - `.gitignore`: `.idea/`, `*.iml`, `.gradle/`, `build/`, `out/`
- **완료 조건**: `./gradlew build`가 성공한다(테스트 0개). 의존성 해석에 성공한다(KOMORAN 포함).

### T-002 패키지 구조·앱 진입점 골격
- **Purpose**: plan §6의 패키지 구조를 확정해서 이후 작업이 놓일 위치를 고정한다.
- **Required**: Yes · **Depends**: T-001 · **Ref**: plan §6
- **작업**:
  - `dailynews.{config,model,http,collect,clean,select,summary,keyword,render}` 패키지를 만든다.
  - `DailyNewsApp.main`은 시작 로그만 출력한다.
  - 테스트 1개를 둔다(빌드 파이프라인 확인용).
- **완료 조건**: `./gradlew run`이 종료 코드 0으로 끝난다. `./gradlew test`에서 테스트 1개가 통과한다.

### T-003 PR 테스트 워크플로(ci.yml)
- **Purpose**: 모든 PR에서 테스트가 자동으로 돌게 해서, main에 깨진 코드가 들어가지 않게 한다(Ruleset required check의 전제).
- **Required**: Yes · **Depends**: T-001 · **Ref**: plan §9, §10.3, §8(공급망)
- **작업**: `.github/workflows/ci.yml`을 만든다.
  - `on: pull_request`
  - checkout → setup-java 21 → `gradle/actions/wrapper-validation` → setup-gradle → `./gradlew test`
  - job 이름은 `test`, `permissions: contents: read`
- **완료 조건**: YAML 문법이 맞는다(actionlint 또는 GitHub에서 확인). 첫 PR(T-037)에서 `ci / test`가 초록색이다.

---

## G2. 모델·설정 (Low)

### T-004 도메인 모델(Category·record)
- **Purpose**: 파이프라인 단계 사이에서 주고받는 불변 데이터 구조를 정의한다.
- **Required**: Yes · **Depends**: T-002 · **Ref**: plan §4.2
- **작업**:
  - `Category` enum: 정치·사회·경제·IT·글로벌, 라벨, 이모지, slug(`politics` 등)
  - record 정의: `Source`, `Article`, `Story`, `SummaryOrigin`, `Summary`, `NewsItem`, `Newsletter`, `RunStats`
  - `Newsletter.sections`는 Category 순서를 지키는 `EnumMap`이다. 컬렉션은 방어적으로 복사한다(`List.copyOf`).
- **완료 조건**: Category 순서·라벨·slug와 record의 불변성(원본 리스트를 바꿔도 영향이 없음)을 검증하는 단위 테스트가 통과한다.

### T-005 sources.yml·ConfigLoader
- **Purpose**: 언론사·피드·selector·키워드·보관 기간을 코드 수정 없이 바꿀 수 있게 한다(US8, FR-2.4, FR-7.5).
- **Required**: Yes · **Depends**: T-004 · **Ref**: plan §4.1
- **작업**:
  - `src/main/resources/sources.yml`에 연합·동아·경향 피드 15개, selector, 크레디트 패턴, itKeywords, stopwords, `retention`, 클러스터 임계값(0.4)을 넣는다.
  - `AppConfig`/`SourceConfig` record를 만든다.
  - `ConfigLoader`를 만든다(classpath 기본값, `--config` 경로 지원).
  - 검증: http(s) URL만 허용, 필수 필드, `perCategory ≥ 1`, `siteArchiveDays ≥ 1`, `newsDays ≥ 0`, 정규식 컴파일
- **완료 조건**:
  - 기본 설정을 로드하면 언론사 3곳·피드 15개가 나온다.
  - 잘못된 URL이나 누락 필드가 있으면 설명이 담긴 예외가 난다.
  - 외부 파일로 덮어쓸 수 있다.
  - 위 내용을 모두 테스트로 확인한다.

---

## G3. HTTP 계층 (Medium)

### T-006 HttpFetcher·JdkHttpFetcher·FakeHttpFetcher
- **Purpose**: 네트워크를 인터페이스 뒤로 숨겨서, 모든 상위 단계를 오프라인으로 테스트할 수 있게 한다(NFR-7).
- **Required**: Yes · **Depends**: T-005 · **Ref**: plan §5.2
- **작업**:
  - `HttpFetcher.fetch(URI) → FetchResult(status, bytes, contentType, finalUri)`
  - `JdkHttpFetcher`: User-Agent, 타임아웃 10초, 리다이렉트 추적, 5xx·타임아웃은 1회 재시도
  - 테스트용 `FakeHttpFetcher`: URI → fixture 파일 또는 상태 코드 매핑
- **완료 조건**: `FakeHttpFetcher`로 200, 404, 500, 예외 응답을 흉내 낼 수 있다. `JdkHttpFetcher`의 재시도·UA 설정을 단위 테스트로 검증한다(로컬 `HttpServer` 사용).

### T-007 HostThrottle(호스트별 요청 간격)
- **Purpose**: 같은 언론사 서버에 연속으로 요청할 때 간격을 둬서 부담을 줄인다(NFR-3).
- **Required**: Yes · **Depends**: T-006 · **Ref**: plan §5.2
- **작업**: 호스트별 마지막 요청 시각을 기록하고 `requestDelayMillis`만큼 기다린다. 주입한 `Clock`/`Sleeper`로 테스트할 수 있게 한다. `HttpFetcher`를 감싸는 데코레이터로 만든다.
- **완료 조건**: 같은 호스트에 연속 요청하면 지연이 적용되고, 다른 호스트는 지연 없이 나가는 것을 가짜 Sleeper 테스트로 확인한다.

### T-008 RobotsPolicy(robots.txt 준수)
- **Purpose**: robots.txt가 허용하는 경로만 요청한다(NFR-3).
- **Required**: Yes · **Depends**: T-006 · **Ref**: plan §5.2
- **작업**:
  - 호스트별 robots.txt를 1회 받아 캐시한다.
  - `User-agent: *` 그룹을 파싱한다(`Disallow :`처럼 콜론 앞 공백 허용).
  - `Allow`/`Disallow` 최장 일치 규칙, `*`와 `$` 와일드카드를 지원한다.
  - robots.txt가 404면 전부 허용, 5xx나 오류면 보수적으로 차단한다.
- **완료 조건**: 단위 테스트로 다음을 확인한다.
  - 서울신문 `/xml/rss/...`는 차단된다.
  - 연합 `/view/AKR…`는 허용, `/view/AEN…`은 차단된다.
  - 동아 `/news/Economy/article/...`는 허용된다.
  - Allow가 Disallow보다 길게 일치하면 우선한다.

---

## G4. RSS 수집 (Medium)

### T-009 테스트 fixture 수집
- **Purpose**: 실제 사이트 구조를 반영한 오프라인 테스트 데이터를 확보한다.
- **Required**: Yes · **Depends**: T-001 · **Ref**: plan §6 (`src/test/resources/fixtures/`)
- **작업**:
  - `fixtures/{yonhap,donga,khan}/`에 RSS 1개와 기사 HTML 2개씩을 저장한다.
  - 각 언론사 robots.txt와 서울신문 robots.txt(차단 사례)도 저장한다.
  - fixture별 출처 URL과 수집일을 `fixtures/README.md`에 적는다.
  - 기사 HTML은 테스트에 필요한 본문 주변만 남겨 줄여도 된다(저작권·용량).
- **완료 조건**: fixture 파일이 있고 UTF-8로 읽힌다. 각 RSS에 item이 10개 이상이다.

### T-010 RssParser(날짜·CDATA 처리)
- **Purpose**: 언론사마다 다른 RSS 형식을 하나의 `Article` 목록으로 바꾼다(FR-2.2).
- **Required**: Yes · **Depends**: T-004, T-009 · **Ref**: plan §3 단계 2
- **작업**:
  - Rome으로 파싱해서 title, link, `pubDate` 또는 `dc:date`, description을 꺼낸다.
  - CDATA와 앞뒤 공백을 처리한다. 날짜를 해석할 수 없는 item은 건너뛰고 로그를 남긴다.
  - description은 `Jsoup.parse().text()`로 텍스트만 남긴다(img 태그 제거).
- **완료 조건**: fixture 3종을 파싱한 결과로 다음을 확인하는 테스트가 통과한다.
  - 건수가 맞다.
  - 첫 item의 제목·링크·발행 시각(KST)이 맞다.
  - 경향의 `dc:date`를 해석한다.
  - 동아 description에 HTML 태그가 없다.

### T-011 FeedCollector(실패 피드 skip)
- **Purpose**: 피드 15개를 순차로 수집하고, 일부가 실패해도 계속 진행한다(FR-2.5, NFR-2, US6).
- **Required**: Yes · **Depends**: T-007, T-008, T-010 · **Ref**: plan §3
- **작업**: 설정의 (언론사 × 카테고리) 피드를 순회하며 Throttle과 Robots를 거쳐 요청·파싱한다. 결과는 `List<Article>`과 실패 피드 목록이고, 피드별 수집 건수를 로그로 남긴다(NFR-8).
- **완료 조건**: FakeHttpFetcher 테스트로 다음을 확인한다.
  - 정상이면 기사 합계가 맞다.
  - 피드 1개가 500이면 나머지는 수집되고 `failedFeeds`에 1건이 기록된다.
  - robots에 막힌 피드는 요청하지 않는다.

---

## G5. 정제·중복 제거 (Medium)

### T-012 UrlNormalizer
- **Purpose**: 원문 URL을 표준화하고 안전한 링크만 남긴다(FR-3.1, FR-7.0, plan §8).
- **Required**: Yes · **Depends**: T-004 · **Ref**: plan §8
- **작업**:
  - `&amp;`를 디코딩하고 `utm_*` 쿼리를 제거한다. fragment도 제거한다. 호스트는 소문자로 바꾼다.
  - http(s) 스킴과, 설정된 언론사 도메인(하위 도메인 포함)만 허용한다.
- **완료 조건**: 단위 테스트로 다음을 확인한다.
  - 경향 RSS 링크의 utm이 제거된다.
  - `javascript:`와 다른 도메인 링크는 거부된다.
  - 이미 정규화된 URL은 바뀌지 않는다.

### T-013 TitleNormalizer
- **Purpose**: 제목 비교·클러스터링용 정규형을 만든다(FR-3.2, FR-4.1).
- **Required**: Yes · **Depends**: T-004 · **Ref**: plan §6.1
- **작업**: `[단독]`·`[속보]` 같은 대괄호 말머리, 따옴표, 특수문자, 중복 공백을 제거한다. 표시용 원 제목은 그대로 보존한다.
- **완료 조건**: 말머리·따옴표 변형이 있는 제목 쌍이 같은 정규형이 되는지 단위 테스트로 확인한다.

### T-014 ArticleCleaner(24h·중복 제거)
- **Purpose**: 최근 24시간 기사만 남기고 중복을 합친다(FR-2.3, FR-3.2).
- **Required**: Yes · **Depends**: T-012, T-013 · **Ref**: plan §3 단계 3
- **작업**: 고정 `Clock` 기준으로 `windowHours` 이전 기사와 미래 시각 기사를 제외한다. URL이 같거나 같은 언론사에서 정규화 제목이 같으면 하나로 합친다(가장 이른 것 유지).
- **완료 조건**: 24시간 경계(정확히 24시간 전 포함, 1초 초과 제외)와 중복 합치기를 단위 테스트로 확인한다.

### T-015 CategoryFilter(IT 키워드)
- **Purpose**: IT 전용 피드가 없는 연합뉴스 산업 피드에서 IT 기사만 고른다(FR-3.3).
- **Required**: Yes · **Depends**: T-005, T-014 · **Ref**: plan §4.1 `itKeywordFilterFeeds`
- **작업**: `itKeywordFilterFeeds`로 지정된 피드의 기사는 제목이나 설명에 `itKeywords`가 있을 때만 통과시킨다. 영문 키워드는 단어 경계 기준으로 비교한다(예: `AI`가 `MAIN`에 걸리지 않게).
- **완료 조건**: 키워드가 있는 기사는 통과, 없는 기사는 제외되고, 다른 피드는 영향을 받지 않는 것을 테스트로 확인한다.

---

## G6. 주요 기사 선정 (High)

### T-016 KomoranAnalyzer(명사 추출 래퍼)
- **Purpose**: KOMORAN 초기화 비용을 한 번만 치르게 하고, 클러스터링과 키워드 추출이 같은 분석기를 함께 쓰게 한다.
- **Required**: Yes · **Depends**: T-001 · **Ref**: plan §6.1
- **작업**: `KomoranAnalyzer.nouns(String) → List<Noun(text, tag)>`를 만든다(NNP·NNG, 2글자 이상). 인스턴스는 지연 초기화 싱글턴으로 둔다.
- **완료 조건**: 샘플 제목에서 기대한 명사가 추출되는지 단위 테스트로 확인한다(예: "삼성전자 반도체 투자 확대" → 삼성전자·반도체·투자·확대).

### T-017 TitleSimilarity
- **Purpose**: 같은 사안을 다룬 서로 다른 언론사 제목을 알아본다(FR-4.1).
- **Required**: Yes · **Depends**: T-013, T-016 · **Ref**: plan §6.1
- **작업**: `similarity(a, b) = max(문자 2-gram Jaccard, 명사 집합 Jaccard)`를 구현한다. 입력은 정규화된 제목이다.
- **완료 조건**: fixture에서 고른 같은 사안 제목 쌍 3개 이상은 0.4 이상, 다른 사안 쌍 3개 이상은 0.4 미만인지 테스트로 확인한다.

### T-018 StoryClusterer
- **Purpose**: 카테고리 안에서 같은 사안의 기사를 하나의 Story로 묶는다(FR-4.1).
- **Required**: Yes · **Depends**: T-017 · **Ref**: plan §6.1
- **작업**: 카테고리별로 최신순으로 순회하며 greedy single-link로 묶는다(임계값은 설정값). 각 클러스터의 `outletCount`(서로 다른 언론사 수)를 계산한다.
- **완료 조건**: 3사가 보도한 사안 1개와 단독 기사 2개를 넣으면 클러스터 3개가 나오고 `outletCount`가 3·1·1인지 테스트로 확인한다.

### T-019 StoryRanker·StorySelector
- **Purpose**: 카테고리별 Top 5 사안과 대표 기사·관련 링크를 정한다(FR-4.2~4.4, FR-7.0).
- **Required**: Yes · **Depends**: T-018 · **Ref**: plan §3 설계 원칙, §6.1
- **작업**:
  - `score = outletCount×10 + recency`, 동점이면 기사 수가 많은 쪽
  - 대표 기사는 가장 이른 보도이고, 동점이면 설정 순서를 따른다. `related`는 언론사별 1건이다.
  - 카테고리 순서(정치→사회→경제→글로벌→IT)대로, 앞에서 선정된 URL은 뒤 카테고리에서 뺀다.
  - 후보가 5개 미만이면 있는 만큼만 선정한다.
- **완료 조건**: 정렬, Top 5, 후보 3개일 때 3개, 카테고리 간 중복 제거, 대표·related 선택 규칙을 단위 테스트로 확인한다.

---

## G7. 본문 요약 (High)

### T-020 BodyExtractor
- **Purpose**: 기사 HTML에서 본문 문단만 꺼낸다(FR-5.1).
- **Required**: Yes · **Depends**: T-009, T-005 · **Ref**: plan §4.1 selector, §6.1 LeadSummarizer 1
- **작업**: 언론사별 `bodySelector`로 요소를 고르고 `figure`, `figcaption`, `script`, `style`, `.ad`, `em.img_desc` 등을 제거한다. 결과는 문단 텍스트 리스트로 돌려준다. 문단이 없으면 빈 리스트다.
- **완료 조건**: 연합·동아·경향 기사 fixture마다 본문 80자 이상을 추출하고, 사진 설명 문구가 들어가지 않는지 테스트로 확인한다.

### T-021 SentenceSplitter
- **Purpose**: 한국어 기사 문장을 정확히 나눠서 리드 요약의 기본 단위를 만든다.
- **Required**: Yes · **Depends**: T-002 · **Ref**: plan §6.1 LeadSummarizer 3
- **작업**: `다.`·`요.`·`.`·`?`·`!` 뒤 공백을 기준으로 나눈다. 따옴표(“ ” ‘ ' ")가 열린 동안은 나누지 않는다. 소수점(`3.5%`)과 약어도 보호한다.
- **완료 조건**: 인용문, 소수점, 영문 약어가 섞인 문장 세트로 기대한 분할 결과가 나오는지 테스트로 확인한다.

### T-022 LeadSummarizer
- **Purpose**: 원문에 근거한 2~3문장, 200자 이하 요약을 만든다(FR-5.2~5.5, NFR-4, SC5).
- **Required**: Yes · **Depends**: T-020, T-021 · **Ref**: plan §6.1 LeadSummarizer
- **작업**:
  - 크레디트 패턴, 이메일, `▶ 관련기사`, `ⓒ`/무단 전재 문구, `[언론사]` 머리말을 제거한다.
  - 120자 이상이 되거나 3문장이 될 때까지 누적한다. 200자를 넘으면 마지막 문장을 뺀다. 첫 문장이 200자를 넘으면 200자에서 자르고 `…`을 붙인다.
  - 본문이 40자 미만이면 RSS 설명문으로 같은 처리를 한다(`RSS`). 둘 다 없으면 `NONE`이다.
- **완료 조건**: fixture 기사마다 다음을 단위 테스트로 확인한다.
  - 요약이 200자 이하다.
  - 크레디트가 없다.
  - **요약의 각 문장이 정제된 본문의 부분 문자열**이다(SC5).
  - RSS 대체와 NONE 경로가 동작한다.

### T-023 SummaryService(본문 요청→요약 연결)
- **Purpose**: 선정된 대표 기사의 원문을 robots 정책에 맞춰 요청하고, 요약과 통계를 만든다(FR-5, NFR-8).
- **Required**: Yes · **Depends**: T-008, T-019, T-022 · **Ref**: plan §3 단계 7~8
- **작업**: Story마다 다음을 처리하고 `RunStats.bodyOk/bodyFallback`을 집계해서 `List<NewsItem>`을 돌려준다.
  - robots 확인 → Throttle 요청 → BodyExtractor → LeadSummarizer
  - 실패하거나 차단되면 RSS로 대체한다.
- **완료 조건**: FakeHttpFetcher 테스트로 다음을 확인한다.
  - 정상 기사는 `BODY`다.
  - 404나 차단된 기사는 `RSS`다.
  - 요청 수가 선정 기사 수를 넘지 않는다(NFR-1).

---

## G8. 오늘의 키워드 (Low)

### T-024 KeywordExtractor
- **Purpose**: 그날 전체 제목에서 대표 키워드 10개를 뽑는다(FR-6.1).
- **Required**: Yes · **Depends**: T-016, T-005 · **Ref**: plan §6.1
- **작업**: 정규화된 모든 제목의 명사 빈도를 센다. 불용어를 빼고 Top 10을 뽑으며, 동률이면 NNP를 우선하고 그다음 가나다순이다.
- **완료 조건**: 고정 제목 세트로 Top 10의 순서와 불용어 제외를 테스트로 확인한다.

---

## G9. 출력·보관 (Medium)

### T-025 OutputPaths·MarkdownRenderer
- **Purpose**: 저장소 아카이브용 Markdown 뉴스레터를 만든다(FR-7.0, FR-7.1, FR-7.4).
- **Required**: Yes · **Depends**: T-004 · **Ref**: plan §4.3, §6.3
- **작업**:
  - `OutputPaths`: `news/YYYY/MM/YYYY-MM-DD.md`, `site/...` 경로 계산을 한곳에 모은다.
  - `MarkdownRenderer`: 헤더(날짜·요일, 언론사, 건수), 키워드, 카테고리별 기사를 쓴다.
    - 기사 항목: 제목, **원문 URL 문자열**, 언론사·시각·보도 언론사 수, 요약, 같은 사안 링크
  - Markdown 특수문자를 이스케이프한다. 파일은 덮어쓰기로 쓴다.
- **완료 조건**: golden file 비교 테스트가 통과한다. 모든 기사 항목에 `원문: https://`가 있다(SC3-1). `[`나 `*`가 든 제목이 이스케이프된다.

### T-026 Thymeleaf 템플릿·CSS·HtmlRenderer
- **Purpose**: 독자가 아침에 보는 웹페이지를 만든다(FR-7.2, NFR-6, US1~US3).
- **Required**: Yes · **Depends**: T-025 · **Ref**: plan §6.2, §8(XSS)
- **작업**:
  - 템플릿: `layout.html`, `newsletter.html`(카테고리 탭 앵커, 기사 카드, 원문 URL 텍스트, 언론사·보도 수 배지, "RSS 요약" 표기, 같은 사안 링크). `th:text`만 쓴다.
  - `static/style.css`: 색 토큰, 다크모드, 760px, 16px 여백, `overflow-wrap:anywhere`
  - `HtmlRenderer`: `site/index.html`과 `site/archive/YYYY-MM-DD.html`을 쓰고, CSS를 `site/assets/`에 복사한다.
- **완료 조건**: Jsoup으로 출력을 파싱해서 다음을 확인하는 테스트가 통과한다.
  - 섹션이 5개다.
  - 모든 카드에 `a[href^=https]`와 URL 텍스트가 있다.
  - `<script>`가 든 제목이 텍스트로 출력된다.
  - `lang="ko"`와 시맨틱 태그(`nav`, `main`, `article`)가 있다.

### T-027 RetentionCleaner(30일 보관)
- **Purpose**: 웹 날짜별 페이지는 30일만 남기고, Markdown은 영구 보관한다(FR-7.5, SC9).
- **Required**: Yes · **Depends**: T-025, T-005 · **Ref**: plan §6.1 RetentionCleaner
- **작업**:
  - `site/archive/`에서 `^\d{4}-\d{2}-\d{2}\.html$` 파일 중 `기준일 − (siteArchiveDays−1)`보다 이전인 것을 삭제한다.
  - `newsDays > 0`일 때만 `news/`에도 같은 규칙을 적용하고, 빈 월 폴더도 지운다.
  - 삭제 목록을 로그로 남긴다.
- **완료 조건**: `@TempDir` 테스트로 다음을 확인한다.
  - 31개 날짜 파일 중 가장 오래된 1개만 삭제된다(30일째는 보존).
  - 패턴 외 파일과 미래 날짜 파일은 보존된다.
  - `newsDays: 0`이면 Markdown이 삭제되지 않는다.

### T-028 ArchiveRenderer
- **Purpose**: 남아 있는 날짜별 페이지 목록을 보여 준다(FR-7.2, US4).
- **Required**: Yes · **Depends**: T-026, T-027 · **Ref**: plan §4.3, §6.2
- **작업**: `site/archive/*.html`을 스캔해 최신순으로 정렬하고 연·월별로 묶어 `site/archive.html`을 만든다. "30일 이전 뉴스레터는 저장소의 Markdown 아카이브에서 볼 수 있다"는 안내와 링크를 넣는다.
- **완료 조건**: 파일 3개(서로 다른 월)를 넣었을 때 그룹과 순서가 맞고, 삭제된 날짜는 목록에 없는 것을 테스트로 확인한다.

### T-029 ReadmeUpdater·README 초안
- **Purpose**: 저장소 첫 화면에 최신 뉴스레터와 웹 주소를 보여 준다(FR-7.3).
- **Required**: Yes · **Depends**: T-025 · **Ref**: plan §4.3
- **작업**:
  - `README.md` 초안: 프로젝트 소개, 수집 언론사, 웹 주소 자리, `<!-- LATEST:START -->…<!-- LATEST:END -->` 마커
  - `ReadmeUpdater`: 마커 사이만 교체한다. 마커가 없으면 파일 끝에 추가한다.
- **완료 조건**: 마커 안만 바뀌고 나머지 내용은 그대로이며, 두 번 실행해도 결과가 같은지(멱등) 테스트로 확인한다.

---

## G10. 앱 조립·검증 (Medium)

### T-030 DailyNewsApp CLI·파이프라인 조립
- **Purpose**: 모든 단계를 하나의 실행으로 엮고 운영 규칙(종료 코드, 로그)을 적용한다(FR-1.3, NFR-2, NFR-8).
- **Required**: Yes · **Depends**: T-011, T-015, T-019, T-023, T-024, T-026~T-029 · **Ref**: plan §3, §5.1, §7
- **작업**:
  - 옵션 파싱: `--date`, `--root`, `--config`, `--dry-run`
  - `Clock`은 Asia/Seoul로 둔다.
  - 실행 순서: 수집 → 정제 → 필터 → 클러스터 → 선정 → 요약 → 키워드 → Markdown/HTML → Retention → Archive → README
  - `RunStats`를 요약 로그로 남긴다.
  - 종료 코드: 0 / 1(수집 0건·설정 오류) / 2(출력 실패)
- **완료 조건**: 생성자로 의존성을 주입받는 구조다(테스트 가능). `--dry-run`이면 파일을 쓰지 않는다. 잘못된 옵션이면 사용법과 종료 코드 1을 낸다. 위 내용을 테스트로 확인한다.

### T-031 PipelineIntegrationTest
- **Purpose**: fixture만으로 전체 흐름이 명세대로 동작하는지 검증한다(SC3, SC3-1, SC4, SC9, FR-7.4).
- **Required**: Yes · **Depends**: T-030 · **Ref**: plan §9
- **작업**: FakeHttpFetcher, `@TempDir`, 고정 Clock으로 전체를 실행하고 산출물을 검사한다.
- **완료 조건**: 다음을 모두 검증한다.
  - `news/…md`, `site/index.html`, `site/archive/…html`, `site/archive.html`, README 마커가 생성된다.
  - 카테고리 5개가 있다.
  - 모든 기사에 원문 URL이 있다.
  - fixture 기준 `BODY` 요약 비율이 90% 이상이다.
  - 두 번 실행하면 결과 파일이 바이트 단위로 같다.
  - 이전 날짜 파일 31개를 미리 넣으면 웹 30개만 남고 Markdown은 유지된다.

### T-032 장애 시나리오 테스트
- **Purpose**: 부분 실패와 전체 실패에서 정해진 대로 동작하는지 확인한다(SC6, NFR-2).
- **Required**: Yes · **Depends**: T-030 · **Ref**: plan §9
- **작업**: 다음 시나리오를 테스트한다.
  - (a) 한 언론사의 피드 전체가 500
  - (b) 기사 본문이 모두 404
  - (c) 모든 피드 실패
- **완료 조건**:
  - (a) 성공하고 `failedFeeds`에 기록된다.
  - (b) 성공하고 요약은 `RSS`로 대체된다.
  - (c) 종료 코드 1이고, 출력 파일과 README가 바뀌지 않는다.

### T-033 LiveFeedSmokeTest·liveTest 태스크
- **Purpose**: 실제 사이트의 RSS와 HTML 구조가 바뀌었는지 수동으로 점검할 수단을 둔다(A1, R1).
- **Required**: Yes · **Depends**: T-030 · **Ref**: plan §9, §11
- **작업**: `@Tag("live")` 테스트를 만든다. 설정된 피드 15개가 200을 돌려주고 item이 1개 이상인지, 피드별 첫 기사에서 본문 80자 이상이 추출되는지 본다. `liveTest` Gradle 태스크도 등록한다.
- **완료 조건**: `./gradlew liveTest`가 로컬에서 통과한다. `./gradlew test`에서는 실행되지 않는다.

### T-034 로컬 실제 실행·화면 점검·임계값 조정
- **Purpose**: 실제 데이터로 결과 품질과 화면을 확인하고 설정값을 보정한다(SC7, R4, R5).
- **Required**: Yes · **Depends**: T-033 · **Ref**: plan §10.1, §12
- **작업**:
  - `./gradlew run`으로 실제 실행한다.
  - 카테고리별 선정 결과를 사람이 검토한다(같은 사안 묶음이 맞는지, IT 카테고리 품질).
  - 필요하면 클러스터 임계값, itKeywords, stopwords를 조정한다.
  - 브라우저에서 360px 폭과 라이트·다크 모드를 확인한다.
  - 1회 실행 시간을 측정한다(NFR-1, 5분 이내).
- **완료 조건**: 점검 결과(실행 시간, 본문 요약 비율, 조정한 값, 스크린샷 또는 확인 메모)를 Progress의 Notes에 남긴다. 360px에서 가로 스크롤이 없다.

---

## G11. 배포·운영 (Medium)

### T-035 일일 워크플로(daily-news.yml)
- **Purpose**: 매일 09:00 KST에 자동으로 실행·커밋·배포한다(FR-1.1, FR-1.2, FR-8, G1, G5).
- **Required**: Yes · **Depends**: T-030 · **Ref**: plan §10.3, §8
- **작업**: plan §10.3대로 작성한다.
  - 트리거: `schedule 0 0 * * *` + `workflow_dispatch`, `permissions: {}`, concurrency
  - build job: checkout(main, 배포 키) → setup-java 21 → setup-gradle → `./gradlew run` → `git add -A news site README.md` → 변경이 없으면 skip → commit → rebase 재시도 push → upload-pages-artifact
  - deploy job: `deploy-pages`(pages·id-token write)
  - 커밋 작성자는 `vars.NEWS_COMMIT_NAME`/`NEWS_COMMIT_EMAIL`이다.
- **완료 조건**: YAML 문법을 검증한다. 비밀값이 로그에 출력되지 않는 구성이다. 실제 동작은 T-037에서 확인한다.

### T-036 GitHub 1회 설정 가이드
- **Purpose**: 사용자가 직접 해야 하는 GitHub 설정을 빠짐없이 따라 할 수 있게 한다(plan §10.2, R3).
- **Required**: Yes · **Depends**: T-035 · **Ref**: plan §8, §10.2
- **작업**: `docs/feature/daily-news/setup-guide.md`를 작성한다.
  - 저장소 생성·push
  - 배포 키 생성과 등록(공개키는 Deploy keys에 write로, 개인키는 Secret `NEWS_DEPLOY_KEY`로)
  - Variables(noreply 이메일 확인법)
  - main Ruleset(PR 필수, `ci / test`, force push 차단, Bypass: Deploy keys)
  - Pages Source를 GitHub Actions로 설정
  - Bypass 메뉴가 없을 때의 PAT 대안
- **완료 조건**: 가이드 순서대로 설정한 뒤 체크리스트의 모든 항목을 체크할 수 있다(T-037에서 실제 수행).

### T-037 첫 PR 병합·수동 실행 E2E
- **Purpose**: 실제 GitHub 환경에서 전체 흐름(PR → main → 일일 실행 → Pages → 잔디)을 검증한다.
- **Required**: Yes · **Depends**: T-003, T-034, T-036 · **Ref**: plan §9 E2E, §10.4
- **작업**:
  - `feature/daily-news`의 PR을 만들고, `ci / test`가 통과하면 병합한다.
  - Actions에서 `workflow_dispatch`로 수동 실행한다.
  - main에 뉴스 커밋이 생겼는지, Pages URL로 접속되는지, 작성자 이메일, 잔디 반영을 확인한다.
  - 사람이 main에 직접 push하면 거부되는지 확인한다(Ruleset).
- **완료 조건**: 위 확인 항목이 모두 통과한다. Pages URL과 README 웹 주소를 기록한다.

### T-038 7일 연속 운영 관찰
- **Purpose**: 성공 기준을 실제 운영으로 확인한다(SC1, SC2, SC3, SC8, R2).
- **Required**: Yes · **Depends**: T-037 · **Ref**: spec §8
- **작업**: 7일간 매일 다음을 기록한다.
  - 예약 실행 시작 시각과 커밋 시각(10:00 이전인지)
  - 잔디 반영
  - 카테고리별 기사 수
  - 본문 요약 비율
  - 실패 피드
  - Actions 사용 시간(비용 0원 확인)
- **완료 조건**: 7일 연속 수동 개입 없이 SC1·SC2를 충족한다. 지연이 반복되면 cron 조정 여부를 결정해서 Notes에 남긴다.

---

## G12. 선택 개선 (Low)

### T-039 JS 탭 전환(점진적 향상)
- **Purpose**: 카테고리 탭을 클릭하면 해당 섹션만 보이게 해서 모바일 가독성을 높인다(plan §6.2 선택 사항).
- **Required**: No · **Depends**: T-026 · **Ref**: plan §6.2, NFR-6
- **작업**: 작은 인라인 JS를 넣는다: `role="tab"`/`aria-selected`/`tabpanel`, 방향키 이동. JS가 없으면 기존처럼 앵커 스크롤로 동작한다.
- **완료 조건**: 키보드만으로 탭 전환이 되고, JS를 끈 상태에서도 모든 섹션에 접근할 수 있다. 렌더 테스트에서 ARIA 속성을 확인한다.
