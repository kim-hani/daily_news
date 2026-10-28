package dailynews;

import dailynews.clean.UrlNormalizer;
import dailynews.collect.RssParseException;
import dailynews.collect.RssParser;
import dailynews.http.FakeHttpFetcher;
import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.Source;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * fixture만으로 "실제 인터넷"을 흉내 내는 가짜 HTTP 응답 묶음.
 * 기본 설정의 피드 15개는 언론사별 정치 RSS fixture를, 그 RSS에 나온 기사 링크는 언론사별 기사 HTML fixture를 돌려준다.
 */
public final class FixtureWeb {

    /** fixture RSS(2026-10-28 15:27~21:55 발행)를 모두 24시간 안에 두는 실행 시각: 2026-10-28 22:00 KST */
    public static final ZonedDateTime NOW = ZonedDateTime.of(2026, 10, 28, 22, 0, 0, 0, TestArticles.KST);
    public static final Clock CLOCK = Clock.fixed(NOW.toInstant(), TestArticles.KST);

    private FixtureWeb() {
    }

    /** robots.txt + 피드 + 기사 본문 */
    public static FakeHttpFetcher withArticles() {
        FakeHttpFetcher fetcher = feedsOnly();
        for (Source source : TestArticles.SOURCES) {
            for (Article article : articles(source)) {
                new UrlNormalizer().normalize(article.url(), source).ifPresent(url -> fetcher.respondWithFixture(
                        url.toString(), "/fixtures/" + source.id() + "/article-1.html", "text/html; charset=UTF-8"));
            }
        }
        return fetcher;
    }

    /** robots.txt + 피드만(기사 본문은 모두 404) */
    public static FakeHttpFetcher feedsOnly() {
        FakeHttpFetcher fetcher = new FakeHttpFetcher()
                .respondWithFixture("https://www.yna.co.kr/robots.txt", "/fixtures/robots/yonhap.txt", "text/plain")
                .respondWithFixture("https://rss.donga.com/robots.txt", "/fixtures/robots/donga-rss.txt", "text/plain")
                .respondWithFixture("https://www.donga.com/robots.txt", "/fixtures/robots/donga.txt", "text/plain")
                .respondWithFixture("https://www.khan.co.kr/robots.txt", "/fixtures/robots/khan.txt", "text/plain");
        for (Source source : TestArticles.SOURCES) {
            source.feeds().values().forEach(feed -> fetcher.respondWithFixture(feed.toString(),
                    "/fixtures/" + source.id() + "/rss.xml", "application/xml"));
        }
        return fetcher;
    }

    private static List<Article> articles(Source source) {
        try {
            return new RssParser().parse(Fixtures.bytes("/fixtures/" + source.id() + "/rss.xml"), source,
                    Category.POLITICS);
        } catch (RssParseException e) {
            throw new IllegalStateException(e);
        }
    }
}
