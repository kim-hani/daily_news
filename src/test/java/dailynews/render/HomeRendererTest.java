package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HomeRendererTest {

    private final HomeRenderer renderer = new HomeRenderer("https://github.com/kim-hani/daily_news");
    private final Document page = Jsoup.parse(renderer.render(TestNewsletters.sample()));

    @TempDir
    Path root;

    @Test
    void 히어로는_h1_하나에_두_줄이고_스크린리더는_aria_label을_읽는다_FR_2_1() {
        Element h1 = page.selectFirst("main h1");
        assertThat(page.select("h1")).hasSize(1);
        assertThat(h1.attr("aria-label")).isEqualTo("EveryDay. News Feed");
        assertThat(h1.attr("lang")).isEqualTo("en");
        assertThat(h1.hasAttr("data-variable-hover")).isTrue();
        assertThat(h1.select(".hero-line")).hasSize(2).allSatisfy(line -> assertThat(line.attr("aria-hidden")).isEqualTo("true"));
        assertThat(h1.select(".hero-line").get(0).text()).isEqualTo("EveryDay.");
        assertThat(h1.select(".hero-line").get(0).select("> span")).hasSize(9);
        assertThat(h1.select(".hero-line").get(1).wholeText()).isEqualTo("News Feed");
        assertThat(h1.select(".hero-line").get(1).attr("data-stagger")).isEqualTo("last");
    }

    @Test
    void 히어로_아래에_Today와_Calendar_버튼만_이_순서로_있다_FR_2_2() {
        assertThat(page.select("main a[data-cta]")).hasSize(2);
        assertThat(page.select("main a[data-cta]")).extracting(link -> link.attr("data-cta"), Element::text,
                        link -> link.attr("href"))
                .containsExactly(org.assertj.core.groups.Tuple.tuple("primary", "Today", "today.html"),
                        org.assertj.core.groups.Tuple.tuple("secondary", "Calendar", "calendar.html"));
        assertThat(page.select("main a[data-cta]")).allSatisfy(link -> assertThat(link.hasClass("flow-btn")).isTrue());
    }

    @Test
    void 버튼_아래에_최신_날짜를_보여_준다_FR_2_3() {
        Element latest = page.selectFirst("main p.latest");
        assertThat(latest.text()).isEqualTo("Latest · 2026-10-28 (수)");
        assertThat(latest.selectFirst("time").attr("datetime")).isEqualTo("2026-10-28");
    }

    @Test
    void 키워드_통계_헤드라인은_홈에_없다_FR_2_5() {
        assertThat(page.select("ul.chips, [data-count-up], article.card, [role=tablist], p.stats")).isEmpty();
        assertThat(page.select("main").text()).doesNotContain("SCM", "반도체");
    }

    @Test
    void 홈이_현재_페이지로_표시된다() {
        assertThat(page.select("[aria-current]")).hasSize(1);
        assertThat(page.selectFirst("[aria-current=page]").attr("href")).isEqualTo("index.html");
    }

    @Test
    void index_html에_쓴다() throws IOException {
        OutputPaths paths = new OutputPaths(root);

        renderer.write(TestNewsletters.sample(), paths);

        assertThat(paths.siteIndex()).exists();
    }
}
