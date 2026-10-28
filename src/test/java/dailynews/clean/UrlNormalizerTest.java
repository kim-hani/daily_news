package dailynews.clean;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.config.ConfigLoader;
import dailynews.model.Source;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

    private static final List<Source> SOURCES = ConfigLoader.loadDefault().sources();
    private static final Source YONHAP = SOURCES.get(0);
    private static final Source DONGA = SOURCES.get(1);
    private static final Source KHAN = SOURCES.get(2);

    private final UrlNormalizer normalizer = new UrlNormalizer();

    @Test
    void 경향_RSS_링크의_utm과_끝의_슬래시를_지운다() {
        assertThat(normalize("https://www.khan.co.kr/article/202610282137015/"
                + "?utm_source=khan_rss&utm_medium=rss&utm_campaign=politic_news", KHAN))
                .contains(URI.create("https://www.khan.co.kr/article/202610282137015"));
    }

    @Test
    void amp_엔티티를_디코딩하고_추적용이_아닌_파라미터는_남긴다() {
        assertThat(normalize("https://www.yna.co.kr/view/AKR1?section=politics&amp;utm_source=naver&amp;page=2"
                + "&fbclid=abc", YONHAP))
                .contains(URI.create("https://www.yna.co.kr/view/AKR1?section=politics&page=2"));
    }

    @Test
    void 스킴과_호스트를_소문자로_바꾸고_fragment와_기본_포트를_지운다() {
        assertThat(normalize("HTTPS://WWW.YNA.CO.KR:443/view/AKR1#comments", YONHAP))
                .contains(URI.create("https://www.yna.co.kr/view/AKR1"));
        assertThat(normalize("http://www.donga.com:8080/news/1", DONGA))
                .contains(URI.create("http://www.donga.com:8080/news/1"));
    }

    @Test
    void 언론사_하위_도메인은_허용한다() {
        assertThat(normalize("https://m.yna.co.kr/view/AKR1", YONHAP))
                .contains(URI.create("https://m.yna.co.kr/view/AKR1"));
    }

    @ParameterizedTest
    @CsvSource({
            "https://www.yna.co.kr/view/AKR20261028090952504, 0",
            "https://www.donga.com/news/Politics/article/all/20261028/134927665/1, 1",
            "https://www.khan.co.kr/article/202610282137015, 2",
            "https://www.yna.co.kr/view/AKR1?section=politics&page=2, 0"})
    void 이미_정규화된_URL은_바뀌지_않는다(String url, int sourceIndex) {
        assertThat(normalize(url, SOURCES.get(sourceIndex))).contains(URI.create(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "ftp://www.yna.co.kr/view/AKR1",
            "https://evil.example.com/view/AKR1",
            "https://notyna.co.kr/view/AKR1",
            "https://yna.co.kr.evil.com/view/AKR1",
            "https://www.donga.com/news/1",
            "/view/AKR1",
            "https://www.yna.co.kr/view/AKR 1",
            ""})
    void http가_아니거나_다른_도메인이거나_깨진_링크는_거부한다(String url) {
        assertThat(normalize(url, YONHAP)).isEmpty();
    }

    @Test
    void null_링크는_거부한다() {
        assertThat(normalizer.normalize((String) null, YONHAP)).isEmpty();
    }

    private Optional<URI> normalize(String url, Source source) {
        return normalizer.normalize(url, source);
    }
}
