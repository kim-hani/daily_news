package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HtmlRendererTest {

    private final HtmlRenderer renderer = new HtmlRenderer("https://github.com/kim-hani/daily_news");
    private final Document page = Jsoup.parse(renderer.render(TestNewsletters.sample(), ""));

    @TempDir
    Path root;

    @Test
    void 카테고리_섹션이_5개이고_순서가_정해져_있다() {
        assertThat(page.select("main section.category")).extracting(Element::id)
                .containsExactly("politics", "society", "economy", "it", "global");
        assertThat(page.select("section#it p.empty").text()).isEqualTo("오늘은 선정된 기사가 없습니다.");
    }

    @Test
    void 모든_카드에_원문_링크와_URL_텍스트가_있다_SC3_1() {
        assertThat(page.select("article.card")).hasSize(5).allSatisfy(card -> {
            assertThat(card.select("h3 a[href^=https]")).hasSize(1);
            Element url = card.selectFirst("p.url a");
            assertThat(url).isNotNull();
            assertThat(url.text()).startsWith("https://").isEqualTo(url.attr("href"));
        });
    }

    @Test
    void script가_든_제목과_요약은_텍스트로_출력된다() {
        assertThat(page.select("script:not([src])")).isEmpty();
        assertThat(page.select("article.card h3").get(1).text()).isEqualTo(TestNewsletters.XSS_TITLE);
        assertThat(renderer.render(TestNewsletters.sample(), "")).contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<b>태그</b>");
    }

    @Test
    void lang_ko와_시맨틱_태그가_있다() {
        assertThat(page.selectFirst("html").attr("lang")).isEqualTo("ko");
        assertThat(page.select("header, nav, main, article, footer")).isNotEmpty();
        assertThat(page.selectFirst("meta[name=viewport]")).isNotNull();
    }

    @Test
    void 카드에_언론사_시각_보도_수_요약_출처와_같은_사안_링크가_있다() {
        Element first = page.selectFirst("article.card");
        assertThat(first.select(".badge.source").text()).isEqualTo("연합뉴스");
        assertThat(first.select("time").text()).isEqualTo("08:12");
        assertThat(first.select("time").attr("datetime")).isEqualTo("2026-10-28T08:12:00+09:00");
        assertThat(first.select(".badge.outlets").text()).isEqualTo("3개 언론사 보도");
        assertThat(first.select("p.related a")).extracting(Element::text).containsExactly("동아일보", "경향신문");

        Element rss = page.select("article.card").get(1);
        assertThat(rss.select("blockquote .origin").text()).isEqualTo("RSS 요약");
        assertThat(rss.select(".badge.outlets")).isEmpty();

        Element none = page.selectFirst("section#society article.card");
        assertThat(none.select("blockquote")).isEmpty();
    }

    @Test
    void 키워드_칩과_실패_피드를_보여_준다() {
        assertThat(page.select("ul.chips li")).extracting(Element::text).containsExactly("SCM", "반도체", "국회");
        assertThat(page.select("p.failed").text()).contains("khan/IT");
    }

    @Test
    void 탭은_ARIA_역할과_섹션_앵커를_가진다_T039() {
        assertThat(page.select("nav[role=tablist] a[role=tab]")).hasSize(5).allSatisfy(tab -> {
            String target = tab.attr("aria-controls");
            assertThat(tab.attr("href")).isEqualTo("#" + target);
            assertThat(page.getElementById(target).attr("role")).isEqualTo("tabpanel");
            assertThat(page.getElementById(target).attr("aria-labelledby")).isEqualTo(tab.id());
        });
        assertThat(page.select("a[role=tab][aria-selected=true]")).hasSize(1);
        assertThat(page.selectFirst("script[src=assets/tabs.js]")).isNotNull();
    }

    @Test
    void 날짜별_페이지는_상위_경로로_자원을_참조한다() {
        Document archivePage = Jsoup.parse(renderer.render(TestNewsletters.sample(), "../"));

        assertThat(archivePage.selectFirst("link[rel=stylesheet]").attr("href")).isEqualTo("../assets/style.css");
        assertThat(archivePage.selectFirst("a[href=../archive.html]")).isNotNull();
    }

    @Test
    void index와_날짜별_페이지_자원을_쓴다() throws IOException {
        OutputPaths paths = new OutputPaths(root);

        renderer.write(TestNewsletters.sample(), paths);

        assertThat(paths.siteIndex()).exists();
        assertThat(paths.archivePage(TestNewsletters.DATE)).exists();
        assertThat(paths.assetsDir().resolve("style.css")).exists();
        assertThat(paths.assetsDir().resolve("tabs.js")).exists();
        assertThat(Files.readString(paths.siteIndex(), StandardCharsets.UTF_8)).contains("2026-10-28 (수)");
    }
}
