package dailynews.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dailynews.Fixtures;
import dailynews.config.ConfigLoader;
import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.Source;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RssParserTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final List<Source> SOURCES = ConfigLoader.loadDefault().sources();
    private static final Source TEST_SOURCE = new Source("test", "테스트일보", "div", List.of(), Map.of(), Set.of());

    private final RssParser parser = new RssParser();

    @Test
    void 연합뉴스_RSS의_pubDate와_CDATA_제목을_읽는다() throws RssParseException {
        List<Article> articles = parseFixture("yonhap", 0);

        assertThat(articles).hasSize(15);
        Article first = articles.getFirst();
        assertThat(first.title()).isEqualTo("한미 국방장관, SCM은 내년초 개최키로…\"전작권 전환 적기에\"(종합2보)");
        assertThat(first.url()).isEqualTo(URI.create("https://www.yna.co.kr/view/AKR20261028090952504"));
        assertThat(first.publishedAt()).isEqualTo(ZonedDateTime.of(2026, 10, 28, 21, 55, 43, 0, KST));
        assertThat(first.rssDescription()).startsWith("(서울=연합뉴스) 김효정 전명훈 기자 = 한미 국방장관이");
        assertThat(first.source().id()).isEqualTo("yonhap");
        assertThat(first.category()).isEqualTo(Category.POLITICS);
    }

    @Test
    void 동아일보_RSS의_CDATA_링크를_읽고_description의_HTML을_걷어낸다() throws RssParseException {
        List<Article> articles = parseFixture("donga", 1);

        assertThat(articles).hasSize(15);
        Article first = articles.getFirst();
        assertThat(first.title()).isEqualTo("與 ‘조작기소 특검법’ 재시동…野 “공소취소 빌드업” 반발");
        assertThat(first.url()).isEqualTo(
                URI.create("https://www.donga.com/news/Politics/article/all/20261028/134927665/1"));
        assertThat(first.publishedAt()).isEqualTo(ZonedDateTime.of(2026, 10, 28, 21, 34, 0, 0, KST));
        assertThat(first.rssDescription()).startsWith("더불어민주당이 조희대 대법원장에 대한 국정조사 추진에 이어");
        assertThat(articles).allSatisfy(article ->
                assertThat(article.rssDescription()).doesNotContain("<", ">", "img src"));
    }

    @Test
    void 경향신문_RSS의_dc_date를_읽는다() throws RssParseException {
        List<Article> articles = parseFixture("khan", 2);

        assertThat(articles).hasSize(15);
        Article first = articles.getFirst();
        assertThat(first.title()).isEqualTo("이 대통령 “다문화 가족 우대까지 할 필요성 큰지 검토”");
        assertThat(first.url()).isEqualTo(URI.create("https://www.khan.co.kr/article/202610282137015/"
                + "?utm_source=khan_rss&utm_medium=rss&utm_campaign=politic_news"));
        assertThat(first.publishedAt()).isEqualTo(ZonedDateTime.of(2026, 10, 28, 21, 37, 1, 0, KST));
    }

    @Test
    void 발행_시각은_모두_KST로_바꾼다() throws RssParseException {
        assertThat(List.of(parseFixture("yonhap", 0), parseFixture("donga", 1), parseFixture("khan", 2)))
                .allSatisfy(articles -> assertThat(articles)
                        .allSatisfy(article -> assertThat(article.publishedAt().getZone()).isEqualTo(KST)));
    }

    @Test
    void UTC_dc_date도_KST로_바꾼다() throws RssParseException {
        List<Article> articles = parse(item("""
                <title>기사</title><link>https://a.example.com/1</link>
                <dc:date>2026-10-28T00:30:00Z</dc:date>
                """));

        assertThat(articles.getFirst().publishedAt()).isEqualTo(ZonedDateTime.of(2026, 10, 28, 9, 30, 0, 0, KST));
    }

    @Test
    void 제목의_CDATA_앞뒤_공백과_줄바꿈을_정리한다() throws RssParseException {
        List<Article> articles = parse(item("""
                <title><![CDATA[
                    [속보]  국회,   예산안
                    통과   ]]></title>
                <link> https://a.example.com/1 </link>
                <pubDate>Wed, 28 Oct 2026 08:00:00 +0900</pubDate>
                """));

        assertThat(articles.getFirst().title()).isEqualTo("[속보] 국회, 예산안 통과");
        assertThat(articles.getFirst().url()).isEqualTo(URI.create("https://a.example.com/1"));
    }

    @Test
    void description이_없으면_빈_문자열이다() throws RssParseException {
        List<Article> articles = parse(item("""
                <title>기사</title><link>https://a.example.com/1</link>
                <pubDate>Wed, 28 Oct 2026 08:00:00 +0900</pubDate>
                """));

        assertThat(articles.getFirst().rssDescription()).isEmpty();
    }

    @Test
    void 날짜_제목_링크가_없거나_잘못된_item은_건너뛴다() throws RssParseException {
        List<Article> articles = parse(
                item("<title>날짜 없음</title><link>https://a.example.com/1</link>")
                        + item("""
                        <title>날짜 형식 오류</title><link>https://a.example.com/2</link>
                        <pubDate>어제 오후</pubDate>
                        """)
                        + item("<link>https://a.example.com/3</link><pubDate>Wed, 28 Oct 2026 08:00:00 +0900</pubDate>")
                        + item("""
                        <title>위험한 링크</title><link>javascript:alert(1)</link>
                        <pubDate>Wed, 28 Oct 2026 08:00:00 +0900</pubDate>
                        """)
                        + item("""
                        <title>정상</title><link>https://a.example.com/5</link>
                        <pubDate>Wed, 28 Oct 2026 08:00:00 +0900</pubDate>
                        """));

        assertThat(articles).extracting(Article::title).containsExactly("정상");
    }

    @Test
    void 깨진_XML이면_예외를_던진다() {
        byte[] broken = "<rss version=\"2.0\"><channel><item><title>끝나지 않음".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> parser.parse(broken, TEST_SOURCE, Category.POLITICS))
                .isInstanceOf(RssParseException.class);
    }

    @Test
    void RSS가_아닌_HTML_응답이면_예외를_던진다() {
        byte[] html = "<html><body><h1>403 Forbidden</h1></body></html>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> parser.parse(html, TEST_SOURCE, Category.POLITICS))
                .isInstanceOf(RssParseException.class);
    }

    private List<Article> parseFixture(String source, int sourceIndex) throws RssParseException {
        Source config = SOURCES.get(sourceIndex);
        assertThat(config.id()).isEqualTo(source);
        return parser.parse(Fixtures.bytes("/fixtures/" + source + "/rss.xml"), config, Category.POLITICS);
    }

    private List<Article> parse(String items) throws RssParseException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/">
                <channel><title>테스트</title><link>https://a.example.com</link><description>테스트</description>
                %s
                </channel></rss>
                """.formatted(items);
        return parser.parse(xml.getBytes(StandardCharsets.UTF_8), TEST_SOURCE, Category.POLITICS);
    }

    private static String item(String body) {
        return "<item>" + body + "</item>\n";
    }
}
