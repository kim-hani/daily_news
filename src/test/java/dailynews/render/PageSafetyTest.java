package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 새 디자인의 모든 페이지가 보안·접근성·배경 전환 규칙을 지키는지 검사한다(NFR-3, NFR-8, FR-7.3, plan §8·§9.1).
 * 미리보기 사이트(두 달, XSS 제목, 빈 카테고리, 실패 피드)를 만들어 검사한다.
 */
class PageSafetyTest {

    private static final List<String> PAGES = List.of("index.html", "today.html", "archive/2026-11-02.html",
            "archive/2026-10-20.html", "calendar.html", "archive.html");
    private static final List<String> FEEDS = List.of("today.html", "archive/2026-11-02.html", "archive/2026-10-20.html");

    @TempDir
    static Path root;
    static Path site;

    @BeforeAll
    static void buildSite() throws IOException {
        site = PreviewSite.build(root);
    }

    static Stream<Arguments> pages() {
        return PAGES.stream().map(Arguments::of);
    }

    static Stream<Arguments> fullPages() {
        return PAGES.stream().filter(page -> !page.equals("archive.html")).map(Arguments::of);
    }

    static Stream<Arguments> feeds() {
        return FEEDS.stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("pages")
    void 언어_뷰포트_색_구성표_CSP가_있다(String name) throws IOException {
        Document page = read(name);

        assertThat(page.selectFirst("html").attr("lang")).isEqualTo("ko");
        assertThat(page.selectFirst("meta[name=viewport]").attr("content")).contains("width=device-width");
        assertThat(page.selectFirst("meta[name=color-scheme]").attr("content")).isEqualTo("light dark");
        assertThat(page.selectFirst("meta[http-equiv=Content-Security-Policy]").attr("content"))
                .contains("script-src 'self'", "style-src 'self'");
        assertThat(page.select("title").text()).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("pages")
    void 인라인_스크립트_스타일_이벤트와_외부_자원과_남은_th_속성이_없다(String name) throws IOException {
        Document page = read(name);

        assertThat(page.select("script:not([src])")).as("인라인 스크립트").isEmpty();
        assertThat(page.select("[style]")).as("style 속성").isEmpty();
        assertThat(page.select("style")).as("style 요소").isEmpty();
        for (Element element : page.getAllElements()) {
            for (Attribute attribute : element.attributes()) {
                assertThat(attribute.getKey()).as(element.tagName() + " 속성")
                        .doesNotStartWith("on").doesNotStartWith("th:").isNotEqualTo("xmlns:th");
            }
        }
        List<String> resources = page.select("script[src], link[href], img[src], source[src], iframe, object, embed")
                .stream().map(e -> e.hasAttr("src") ? e.attr("src") : e.attr("href")).toList();
        assertThat(resources).allSatisfy(url -> assertThat(url).doesNotContain("//").doesNotStartWith("http"));
    }

    @ParameterizedTest
    @MethodSource("fullPages")
    void theme_init은_defer_async_없이_스타일시트보다_먼저_나온다(String name) throws IOException {
        Document page = read(name);

        Element first = page.head().selectFirst("script[src], link[rel=stylesheet]");
        assertThat(first.tagName()).isEqualTo("script");
        assertThat(first.attr("src")).endsWith("assets/theme-init.js");
        assertThat(first.hasAttr("defer") || first.hasAttr("async")).isFalse();
        assertThat(page.selectFirst("html").hasClass("no-js")).isTrue();
    }

    @ParameterizedTest
    @MethodSource("fullPages")
    void 배경_스위처는_라디오_3개와_접근_가능한_이름을_가진다(String name) throws IOException {
        Element switcher = read(name).selectFirst("fieldset[data-theme-switcher]");

        assertThat(switcher.select("input[type=radio]")).extracting(e -> e.attr("value"))
                .containsExactly("light", "dark", "system");
        assertThat(switcher.select("input[type=radio]")).allSatisfy(radio -> assertThat(radio.attr("aria-label")).isNotBlank());
    }

    @ParameterizedTest
    @MethodSource("feeds")
    void script가_든_제목은_모든_뉴스피드에서_텍스트로_나온다(String name) throws IOException {
        String html = Files.readString(site.resolve(name), StandardCharsets.UTF_8);

        assertThat(Jsoup.parse(html).select("article.card h3").eachText()).contains(TestNewsletters.XSS_TITLE);
        assertThat(html).doesNotContain("<script>alert").contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    private static Document read(String name) throws IOException {
        return Jsoup.parse(Files.readString(site.resolve(name), StandardCharsets.UTF_8));
    }
}
