package dailynews.collect;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.config.ConfigLoader;
import dailynews.http.FakeHttpFetcher;
import dailynews.http.RobotsPolicy;
import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.Source;
import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 기본 설정의 피드 15개에 언론사별 RSS fixture(item 15개)를 돌려주는 가짜 fetcher로 검증한다.
 */
class FeedCollectorTest {

    private static final List<Source> SOURCES = ConfigLoader.loadDefault().sources();
    private static final int ITEMS_PER_FEED = 15;

    private final FakeHttpFetcher fetcher = new FakeHttpFetcher()
            .respondWithFixture("https://www.yna.co.kr/robots.txt", "/fixtures/robots/yonhap.txt", "text/plain")
            .respondWithFixture("https://rss.donga.com/robots.txt", "/fixtures/robots/donga-rss.txt", "text/plain")
            .respondWithFixture("https://www.khan.co.kr/robots.txt", "/fixtures/robots/khan.txt", "text/plain")
            .respondWithFixture("https://www.seoul.co.kr/robots.txt", "/fixtures/robots/seoul.txt", "text/plain");
    private final FeedCollector collector = new FeedCollector(fetcher, new RobotsPolicy(fetcher), new RssParser());

    FeedCollectorTest() {
        for (Source source : SOURCES) {
            source.feeds().values().forEach(feed ->
                    fetcher.respondWithFixture(feed.toString(), "/fixtures/" + source.id() + "/rss.xml", "application/xml"));
        }
    }

    @Test
    void 모든_피드가_정상이면_기사를_모두_모은다() {
        CollectResult result = collector.collect(SOURCES);

        assertThat(result.failedFeeds()).isEmpty();
        assertThat(result.articles()).hasSize(15 * ITEMS_PER_FEED);
        for (Category category : Category.values()) {
            assertThat(result.articles()).filteredOn(article -> article.category() == category)
                    .hasSize(3 * ITEMS_PER_FEED);
        }
    }

    @Test
    void 기사는_언론사_순서와_카테고리_순서대로_모인다() {
        List<Article> articles = collector.collect(SOURCES).articles();

        assertThat(articles.getFirst().source().id()).isEqualTo("yonhap");
        assertThat(articles.getFirst().category()).isEqualTo(Category.POLITICS);
        assertThat(articles.getLast().source().id()).isEqualTo("khan");
        assertThat(articles.getLast().category()).isEqualTo(Category.GLOBAL);
    }

    @Test
    void 피드_하나가_500이면_그_피드만_실패로_기록하고_나머지는_수집한다() {
        fetcher.respond("https://www.yna.co.kr/rss/politics.xml", 500, "");

        CollectResult result = collector.collect(SOURCES);

        assertThat(result.failedFeeds()).containsExactly("yonhap/POLITICS");
        assertThat(result.articles()).hasSize(14 * ITEMS_PER_FEED);
        assertThat(result.articles()).noneMatch(article ->
                article.source().id().equals("yonhap") && article.category() == Category.POLITICS);
    }

    @Test
    void 요청_실패나_RSS가_아닌_응답도_그_피드만_건너뛴다() {
        fetcher.fail("https://rss.donga.com/economy.xml", new ConnectException("연결 실패"))
                .respond("https://www.khan.co.kr/rss/rssdata/kh_world.xml", 200,
                        "<html><body>점검 중</body></html>");

        CollectResult result = collector.collect(SOURCES);

        assertThat(result.failedFeeds()).containsExactly("donga/ECONOMY", "khan/GLOBAL");
        assertThat(result.articles()).hasSize(13 * ITEMS_PER_FEED);
    }

    @Test
    void robots_txt에_막힌_피드는_요청하지_않고_실패로_기록한다() {
        URI blockedFeed = URI.create("https://www.seoul.co.kr/xml/rss/rss_politics.xml");
        Source seoul = new Source("seoul", "서울신문", List.of("seoul.co.kr"), "div", List.of(),
                Map.of(Category.POLITICS, blockedFeed), Set.of());
        List<Source> sources = new ArrayList<>(SOURCES);
        sources.add(seoul);

        CollectResult result = collector.collect(sources);

        assertThat(result.failedFeeds()).containsExactly("seoul/POLITICS");
        assertThat(result.articles()).hasSize(15 * ITEMS_PER_FEED);
        assertThat(fetcher.requests()).contains(URI.create("https://www.seoul.co.kr/robots.txt"))
                .doesNotContain(blockedFeed);
    }

    @Test
    void 모든_피드가_실패하면_기사는_없고_실패_피드가_모두_기록된다() {
        for (Source source : SOURCES) {
            source.feeds().values().forEach(feed -> fetcher.respond(feed.toString(), 503, ""));
        }

        CollectResult result = collector.collect(SOURCES);

        assertThat(result.articles()).isEmpty();
        assertThat(result.failedFeeds()).hasSize(15).startsWith("yonhap/POLITICS").endsWith("khan/GLOBAL");
    }
}
