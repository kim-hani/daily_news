package dailynews.summary;

import static dailynews.TestArticles.DONGA;
import static dailynews.TestArticles.KHAN;
import static dailynews.TestArticles.YONHAP;
import static org.assertj.core.api.Assertions.assertThat;

import dailynews.Fixtures;
import dailynews.model.Source;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BodyExtractorTest {

    private final BodyExtractor extractor = new BodyExtractor();

    static Stream<Arguments> fixtures() {
        return Stream.of(
                Arguments.of(YONHAP, 1, "[국방부 제공. 재판매 및 DB 금지]"),
                Arguments.of(YONHAP, 2, "재판매 및 DB 금지"),
                Arguments.of(DONGA, 1, "2026.10.28 뉴스1"),
                Arguments.of(DONGA, 2, "크게보기"),
                Arguments.of(KHAN, 1, "청와대사진기자단"),
                Arguments.of(KHAN, 2, "양국 연례 회의 미룬 건"));
    }

    @ParameterizedTest(name = "{0} article-{1}")
    @MethodSource("fixtures")
    void 언론사마다_본문을_80자_이상_뽑고_사진_설명과_부제목은_뺀다(Source source, int n, String excluded) {
        List<String> paragraphs = extract(source, n);

        assertThat(String.join("", paragraphs).length()).isGreaterThanOrEqualTo(80);
        assertThat(paragraphs).noneMatch(paragraph -> paragraph.contains(excluded));
    }

    @Test
    void 연합뉴스는_본문_문단만_뽑는다() {
        List<String> paragraphs = extract(YONHAP, 1);

        assertThat(paragraphs.getFirst()).startsWith("(서울=연합뉴스) 김효정 전명훈 기자 = 한미 국방장관이");
        assertThat(paragraphs).noneMatch(paragraph -> paragraph.contains("강신철, 美헤그세스와 필리핀서"))
                .noneMatch(paragraph -> paragraph.contains("관련 뉴스"))
                .noneMatch(paragraph -> paragraph.contains("저작권자"));
    }

    @Test
    void 동아일보는_br로_문단을_나누고_중간_제목을_뺀다() {
        List<String> paragraphs = extract(DONGA, 1);

        assertThat(paragraphs).hasSize(3);
        assertThat(paragraphs.getFirst()).startsWith("더불어민주당이 조희대 대법원장에 대한");
        assertThat(paragraphs).noneMatch(paragraph -> paragraph.startsWith("●"));
    }

    @Test
    void 경향신문은_소제목_strapline을_뺀다() {
        List<String> paragraphs = extract(KHAN, 1);

        assertThat(paragraphs.getFirst()).startsWith("이재명 대통령은 28일 다문화 가정에 대한");
        assertThat(paragraphs).noneMatch(paragraph -> paragraph.contains("누가 하든 좋은 지적은 좋은 것"));
    }

    @Test
    void 본문_요소가_없으면_빈_목록이다() {
        byte[] html = "<html><body><div class='other'><p>다른 영역</p></div></body></html>"
                .getBytes(StandardCharsets.UTF_8);

        assertThat(extractor.extract(html, "text/html", URI.create("https://www.yna.co.kr/"), YONHAP)).isEmpty();
    }

    @Test
    void Content_Type의_charset으로_디코딩한다() {
        byte[] html = "<html><body><section class='news_view'>한글 본문입니다.</section></body></html>"
                .getBytes(Charset.forName("EUC-KR"));

        assertThat(extractor.extract(html, "text/html; charset=EUC-KR", URI.create("https://www.donga.com/"), DONGA))
                .containsExactly("한글 본문입니다.");
    }

    private List<String> extract(Source source, int n) {
        return extractor.extract(Fixtures.bytes("/fixtures/" + source.id() + "/article-" + n + ".html"),
                "text/html; charset=UTF-8", URI.create("https://example.com/"), source);
    }
}
