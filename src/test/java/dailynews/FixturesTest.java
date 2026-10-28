package dailynews;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FixturesTest {

    private static final Pattern ITEM = Pattern.compile("<item\\b");

    @ParameterizedTest
    @ValueSource(strings = {
            "/fixtures/yonhap/rss.xml", "/fixtures/yonhap/article-1.html", "/fixtures/yonhap/article-2.html",
            "/fixtures/donga/rss.xml", "/fixtures/donga/article-1.html", "/fixtures/donga/article-2.html",
            "/fixtures/khan/rss.xml", "/fixtures/khan/article-1.html", "/fixtures/khan/article-2.html",
            "/fixtures/robots/yonhap.txt", "/fixtures/robots/donga.txt", "/fixtures/robots/donga-rss.txt",
            "/fixtures/robots/khan.txt", "/fixtures/robots/seoul.txt", "/fixtures/README.md"})
    void fixture는_UTF_8로_읽힌다(String path) {
        byte[] bytes = Fixtures.bytes(path);

        assertThat(bytes).isNotEmpty();
        assertThatCode(() -> StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"yonhap", "donga", "khan"})
    void RSS에는_item이_10개_이상_있다(String source) {
        String rss = Fixtures.text("/fixtures/" + source + "/rss.xml");

        assertThat(ITEM.matcher(rss).results().count()).isGreaterThanOrEqualTo(10);
    }
}
