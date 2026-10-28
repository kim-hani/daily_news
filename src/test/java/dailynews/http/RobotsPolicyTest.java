package dailynews.http;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.config.ConfigLoader;
import java.net.ConnectException;
import java.net.URI;
import org.junit.jupiter.api.Test;

/**
 * 실제 언론사 robots.txt(2026-10-28 수집, src/test/resources/fixtures/robots)로 판단을 검증한다.
 */
class RobotsPolicyTest {

    private final FakeHttpFetcher fetcher = new FakeHttpFetcher()
            .respondWithFixture("https://www.yna.co.kr/robots.txt", "/fixtures/robots/yonhap.txt", "text/plain")
            .respondWithFixture("https://www.donga.com/robots.txt", "/fixtures/robots/donga.txt", "text/plain")
            .respondWithFixture("https://rss.donga.com/robots.txt", "/fixtures/robots/donga-rss.txt", "text/plain")
            .respondWithFixture("https://www.khan.co.kr/robots.txt", "/fixtures/robots/khan.txt", "text/plain")
            .respondWithFixture("https://www.seoul.co.kr/robots.txt", "/fixtures/robots/seoul.txt", "text/plain");
    private final RobotsPolicy policy = new RobotsPolicy(fetcher);

    @Test
    void 서울신문_RSS_경로는_차단된다() {
        assertThat(allowed("https://www.seoul.co.kr/xml/rss/rss_politics.xml")).isFalse();
        assertThat(allowed("https://www.seoul.co.kr/news/politics/2026/10/28/20261028001001")).isTrue();
    }

    @Test
    void 연합뉴스_국내_기사는_허용되고_영문_기사는_차단된다() {
        assertThat(allowed("https://www.yna.co.kr/view/AKR20261028000100001")).isTrue();
        assertThat(allowed("https://www.yna.co.kr/view/AEN20261028000100315")).isFalse();
        assertThat(allowed("https://www.yna.co.kr/view/AKR20261028000100001?section=politics&cp=naver")).isFalse();
        assertThat(allowed("https://www.yna.co.kr/view/AKR20150422172700003")).isFalse();
    }

    @Test
    void 동아일보_기사는_허용되고_대문자_NEWS_경로는_차단된다() {
        assertThat(allowed("https://www.donga.com/news/Economy/article/all/20261028/132600001/1")).isTrue();
        assertThat(allowed("https://www.donga.com/NEWS/article/1")).isFalse();
        assertThat(allowed("https://www.donga.com/news/search?query=AI")).isFalse();
    }

    @Test
    void 경향신문_기사는_허용되고_AI_봇_전용_차단은_적용되지_않는다() {
        assertThat(allowed("https://www.khan.co.kr/article/202610281200001")).isTrue();
        assertThat(allowed("https://www.khan.co.kr/login")).isFalse();
        assertThat(allowed("https://www.khan.co.kr/print.html?art_id=201709021732001&media=khan&type=basic")).isFalse();
        assertThat(allowed("https://www.khan.co.kr/print.html?art_id=202610281200001&media=khan&type=basic")).isTrue();
    }

    @Test
    void 기본_설정의_피드_15개는_모두_허용된다() {
        assertThat(ConfigLoader.loadDefault().sources().stream().flatMap(source -> source.feeds().values().stream()))
                .hasSize(15)
                .allSatisfy(feed -> assertThat(policy.isAllowed(feed)).as(feed.toString()).isTrue());
    }

    @Test
    void robots_txt는_호스트마다_한_번만_받는다() {
        allowed("https://www.yna.co.kr/view/AKR1");
        allowed("https://www.yna.co.kr/view/AKR2");
        allowed("https://www.yna.co.kr/rss/politics.xml");
        allowed("https://www.khan.co.kr/article/1");

        assertThat(fetcher.requests()).containsExactly(
                URI.create("https://www.yna.co.kr/robots.txt"),
                URI.create("https://www.khan.co.kr/robots.txt"));
    }

    @Test
    void robots_txt가_404면_전부_허용한다() {
        assertThat(allowed("https://none.example.com/anything")).isTrue();
    }

    @Test
    void robots_txt가_5xx면_전부_차단한다() {
        fetcher.respond("https://broken.example.com/robots.txt", 503, "");

        assertThat(allowed("https://broken.example.com/article/1")).isFalse();
    }

    @Test
    void robots_txt_요청이_실패하면_전부_차단한다() {
        fetcher.fail("https://down.example.com/robots.txt", new ConnectException("연결 실패"));

        assertThat(allowed("https://down.example.com/article/1")).isFalse();
    }

    @Test
    void robots_txt_자체는_항상_허용한다() {
        fetcher.respond("https://broken.example.com/robots.txt", 503, "");

        assertThat(allowed("https://broken.example.com/robots.txt")).isTrue();
        assertThat(fetcher.requests()).isEmpty();
    }

    @Test
    void 경로가_없는_URL은_루트로_본다() {
        assertThat(allowed("https://www.seoul.co.kr")).isTrue();
    }

    private boolean allowed(String uri) {
        return policy.isAllowed(URI.create(uri));
    }
}
