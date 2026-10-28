package dailynews;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import dailynews.clean.UrlNormalizer;
import dailynews.collect.RssParser;
import dailynews.config.AppConfig;
import dailynews.config.ConfigLoader;
import dailynews.http.FetchResult;
import dailynews.http.HttpFetcher;
import dailynews.http.RobotsPolicy;
import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.Source;
import dailynews.summary.BodyExtractor;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 실제 언론사 사이트에 접속해 RSS와 기사 HTML 구조가 바뀌지 않았는지 점검한다(A1, R1).
 * 네트워크를 쓰므로 {@code ./gradlew test}에서는 빠지고 {@code ./gradlew liveTest}로만 실행한다.
 * 운영과 같은 요청기(User-Agent, 호스트별 간격, robots.txt 준수)를 쓴다.
 */
@Tag("live")
class LiveFeedSmokeTest {

    private static final AppConfig CONFIG = ConfigLoader.loadDefault();
    private static final HttpFetcher FETCHER = DailyNewsApp.networkFetcher(CONFIG);
    private static final RobotsPolicy ROBOTS = new RobotsPolicy(FETCHER);

    static Stream<Arguments> feeds() {
        return CONFIG.sources().stream().flatMap(source -> source.feeds().entrySet().stream()
                .map(feed -> Arguments.of(source.id() + "/" + feed.getKey(), source, feed.getKey(), feed.getValue())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("feeds")
    void 피드가_열리고_첫_기사에서_본문을_뽑을_수_있다(String name, Source source, Category category, URI feed)
            throws Exception {
        assertThat(ROBOTS.isAllowed(feed)).as("robots.txt 허용: " + feed).isTrue();
        FetchResult rss = FETCHER.fetch(feed);
        assertThat(rss.status()).as("피드 응답: " + feed).isEqualTo(200);
        List<Article> articles = new RssParser().parse(rss.body(), source, category);
        assertThat(articles).as("피드 item: " + feed).isNotEmpty();

        URI url = new UrlNormalizer().normalize(articles.getFirst().url(), source).orElseThrow();
        assertThat(ROBOTS.isAllowed(url)).as("robots.txt 허용: " + url).isTrue();
        FetchResult page = FETCHER.fetch(url);
        // 빈도 제한(경향신문 등)은 구조 변경이 아니므로 실패 대신 건너뜀으로 표시한다. 운영에서는 RSS 요약으로 대체된다.
        assumeFalse(page.status() == 403 || page.status() == 429,
                "사이트 접근 제한(" + page.status() + ")으로 본문 점검을 건너뜁니다: " + url);
        assertThat(page.status()).as("기사 응답: " + url).isEqualTo(200);
        List<String> paragraphs = new BodyExtractor().extract(page.body(), page.contentType(), page.finalUri(), source);
        assertThat(String.join("", paragraphs).length()).as("본문 길이(selector %s): %s", source.bodySelector(), url)
                .isGreaterThanOrEqualTo(80);
    }
}
