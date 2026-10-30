# 📰 Daily News

매일 낮 12시(KST)에 국내 언론사 RSS를 모아 **정치 · 사회 · 경제 · IT · 글로벌** 5개 분야의 주요 기사를 골라 요약하는 뉴스레터입니다.
GitHub Actions가 매일 자동으로 실행하고, 결과를 이 저장소에 커밋한 뒤 GitHub Pages로 게시합니다.

- 웹에서 보기: https://kim-hani.github.io/daily_news/
- 지난 뉴스레터: [`news/`](news) (Markdown, 영구 보관) · 웹은 최근 30일

<!-- LATEST:START -->
### 📰 최신 뉴스레터: 2026-10-30 (금)

- 웹에서 보기: https://kim-hani.github.io/daily_news/
- Markdown: [news/2026/10/2026-10-30.md](news/2026/10/2026-10-30.md)
- 선정 25건 · 🔑 AI · 투자 · 확대 · 선거 · 센터 · 반도체 · 내달 · 무죄 · 협력 · 지역
<!-- LATEST:END -->

## 어떻게 만드나요
- **수집**: 연합뉴스 · 동아일보 · 경향신문의 분야별 RSS 15개를 읽습니다. robots.txt를 지키고, 같은 사이트에는 간격을 두고 요청합니다.
- **선정**: 최근 24시간 기사에서 중복을 지우고, 제목이 비슷한 기사를 같은 사안으로 묶은 뒤
  `보도한 언론사 수 × 10 + 최신성` 점수가 높은 사안을 분야별 5개 고릅니다.
- **요약**: 대표 기사 원문의 앞 2~3문장(최대 200자)을 그대로 인용합니다. AI로 문장을 새로 만들지 않습니다.
  원문을 읽지 못하면 RSS 설명문을 씁니다. 경향신문은 실행 서버에서 기사 페이지 접근이 막혀 있어 처음부터 RSS 설명문을 씁니다.
- **키워드**: 그날 기사 제목에서 많이 나온 명사 10개를 KOMORAN 형태소 분석으로 뽑습니다.

요약은 원문의 짧은 인용이며 저작권은 각 언론사에 있습니다. 기사 전문은 원문 링크에서 읽어 주세요.

## 직접 실행하기
JDK 21이 필요합니다.

```bash
./gradlew test                         # 단위·통합 테스트(네트워크 없이 fixture로 실행)
./gradlew run --args="--dry-run"       # 실제 수집·선정·요약을 하되 파일은 쓰지 않음
./gradlew run                          # news/, site/, README.md 생성
./gradlew liveTest                     # 실제 언론사 RSS·본문 구조 점검(네트워크 사용)
```

옵션: `--date YYYY-MM-DD`(출력 날짜), `--root 경로`(출력 위치), `--config 경로`(설정 파일 교체), `--dry-run`.
언론사·피드·보관 기간·키워드는 [`src/main/resources/sources.yml`](src/main/resources/sources.yml)에서 바꿉니다.

## 문서
- [요구사항 명세](docs/feature/daily-news/spec.md) · [구현 계획](docs/feature/daily-news/plan.md) · [작업 목록](docs/feature/daily-news/tasks.md)
- [구현 보고서와 전체 흐름](docs/feature/daily-news/implementation.md)
- [GitHub 1회 설정 가이드](docs/feature/daily-news/setup-guide.md)
