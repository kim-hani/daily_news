# 테스트 fixture

실제 언론사 사이트에서 받은 오프라인 테스트 데이터다. 모든 파일은 UTF-8이고 **2026-10-28**에 수집했다.
요청에는 `DailyNewsBot/1.0 (+https://github.com/kim-hani/daily_news)` User-Agent를 썼고 robots.txt가 허용하는 경로만 받았다.

## 줄인 방법
- **RSS**: 채널 머리글은 그대로 두고 앞 item 15개만 남겼다(원본 item 수는 아래 표).
- **기사 HTML**: 저작권과 용량 때문에 본문 컨테이너(설정의 `bodySelector` 영역)만 남겼다.
  - 앞 4문단과 요약 테스트에 필요한 잡음 요소(부제목, 사진 설명, 광고 자리, 기자 이메일, 저작권 문구, 관련기사)는 남겼다.
  - 나머지 문단, 스크립트, 본문 밖의 메뉴·댓글은 지웠다. 지운 자리에는 `<!-- fixture: 이하 원문 문단 생략 -->` 주석을 넣었다.
  - 파일 맨 위 주석에 원문 URL을 적었다.

## 파일
| 파일 | 출처 | 비고 |
|---|---|---|
| `yonhap/rss.xml` | https://www.yna.co.kr/rss/politics.xml | 원본 120개 → 15개. `pubDate`, CDATA 제목 |
| `yonhap/article-1.html` | https://www.yna.co.kr/view/AKR20261028090952504 | 한미 국방장관, SCM 내년 초 개최. `khan/article-2.html`과 같은 사안 |
| `yonhap/article-2.html` | https://www.yna.co.kr/view/AKR20261028162251004 | 사진 설명에도 `(서울=연합뉴스)` 크레디트가 있음 |
| `donga/rss.xml` | https://rss.donga.com/politics.xml | 원본 50개 → 15개. CDATA 링크, description 안에 `<img>` |
| `donga/article-1.html` | https://www.donga.com/news/Politics/article/all/20261028/134927665/1 | 본문이 `<p>` 없이 텍스트 + `<br>`로 들어 있음 |
| `donga/article-2.html` | https://www.donga.com/news/Politics/article/all/20261028/134928713/1 | |
| `khan/rss.xml` | https://www.khan.co.kr/rss/rssdata/politic_news.xml | 원본 50개 → 15개. `dc:date`(ISO 8601), 링크에 `utm_*` |
| `khan/article-1.html` | https://www.khan.co.kr/article/202610282137015 | 첫 `p.content_text`가 소제목(`b.strapline`) |
| `khan/article-2.html` | https://www.khan.co.kr/article/202610282137005 | |
| `robots/yonhap.txt` | https://www.yna.co.kr/robots.txt | `/view/AEN*` 등 차단, `/*?*cp=` |
| `robots/donga.txt` | https://www.donga.com/robots.txt | 대문자 `/NEWS` 차단 |
| `robots/donga-rss.txt` | https://rss.donga.com/robots.txt | |
| `robots/khan.txt` | https://www.khan.co.kr/robots.txt | AI 봇 전용 그룹과 `*` 그룹이 따로 있음 |
| `robots/seoul.txt` | https://www.seoul.co.kr/robots.txt | `/xml/`(RSS) 차단 사례. `Disallow :` 형식 |

## 수집하며 확인한 점
- 경향신문 기사 페이지는 1초 간격으로 연달아 요청하면 403을 돌려주고, 1~2분 뒤에는 다시 200을 돌려줬다. 빈도 제한으로 보인다.
