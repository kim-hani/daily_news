package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeedRendererTest {

    private final FeedRenderer renderer = new FeedRenderer("https://github.com/kim-hani/daily_news");
    private final Document page = Jsoup.parse(renderer.render(TestNewsletters.sample(), false));
    private final Document archivePage = Jsoup.parse(renderer.render(TestNewsletters.sample(), true));

    @TempDir
    Path root;

    @Test
    void 카테고리_섹션이_5개이고_순서가_정해져_있다() {
        assertThat(page.select("main section.category")).extracting(Element::id)
                .containsExactly("politics", "society", "economy", "it", "global");
        assertThat(page.select("section#it p.empty").text()).isEqualTo("오늘은 선정된 기사가 없습니다.");
        assertThat(page.select("section#it article.card")).isEmpty();
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
    void 기사_링크에는_noopener_noreferrer가_있다() {
        assertThat(page.select("article.card a[href^=https]")).isNotEmpty()
                .allSatisfy(link -> assertThat(link.attr("rel")).isEqualTo("noopener noreferrer"));
    }

    @Test
    void 카드마다_카테고리_안의_순위가_있다() {
        assertThat(page.select("section#politics article.card .rank")).extracting(Element::text)
                .containsExactly("1", "2");
        assertThat(page.select("section#economy article.card .rank")).extracting(Element::text).containsExactly("1");
    }

    @Test
    void script가_든_제목과_요약은_텍스트로_출력된다() {
        assertThat(page.select("script:not([src])")).isEmpty();
        assertThat(page.select("article.card h3").get(1).text()).isEqualTo(TestNewsletters.XSS_TITLE);
        assertThat(renderer.render(TestNewsletters.sample(), false)).contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<b>태그</b>");
    }

    @Test
    void lang_ko와_시맨틱_태그가_있다() {
        assertThat(page.selectFirst("html").attr("lang")).isEqualTo("ko");
        assertThat(page.selectFirst("html").hasClass("no-js")).isTrue();
        assertThat(page.select("header, nav, main, article, footer")).isNotEmpty();
        assertThat(page.selectFirst("meta[name=viewport]")).isNotNull();
        assertThat(page.select("h1")).hasSize(1);
        assertThat(page.selectFirst("h1 time").attr("datetime")).isEqualTo("2026-10-28");
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
    void 키워드_배지와_실패_피드와_통계_문장을_보여_준다_FR_3_9() {
        assertThat(page.select("ul.chips li")).extracting(Element::text).containsExactly("SCM", "반도체", "국회");
        assertThat(page.select("p.failed").text()).contains("khan/IT");
        assertThat(page.select("p.stats").text())
                .isEqualTo("수집 연합뉴스 · 동아일보 · 경향신문 · 수집 512건 → 중복 제거 431건 → 선정 5건");
    }

    @Test
    void 키워드는_최대_10개다() {
        Document many = Jsoup.parse(renderer.render(TestNewsletters.on(TestNewsletters.DATE, 0, false), false));
        assertThat(many.select("ul.chips li")).hasSize(FeedRenderer.MAX_KEYWORDS);
        assertThat(many.select("p.failed")).isEmpty();
    }

    @Test
    void 카운트업_숫자_3개는_HTML에_최종_값이_있고_data_value와_같다() {
        List<Element> numbers = page.select("[data-count-up]");
        assertThat(numbers).extracting(el -> el.attr("data-value")).containsExactly("512", "431", "5");
        assertThat(numbers).allSatisfy(el -> assertThat(el.text().replace(",", "")).isEqualTo(el.attr("data-value")));
        assertThat(page.selectFirst("[data-count-up]").closest("[aria-hidden=true]")).isNotNull();

        Document big = Jsoup.parse(renderer.render(TestNewsletters.on(TestNewsletters.DATE, 0, false), false));
        assertThat(big.selectFirst("[data-count-up]").text()).isEqualTo("1,100");
    }

    @Test
    void 캘린더가_읽을_dn_메타_태그가_있다() {
        assertThat(page.selectFirst("meta[name=dn:date]").attr("content")).isEqualTo("2026-10-28");
        assertThat(page.selectFirst("meta[name=dn:selected]").attr("content")).isEqualTo("5");
        assertThat(page.select("meta[name=dn:keyword]").eachAttr("content")).containsExactly("SCM", "반도체", "국회");
    }

    @Test
    void 탭은_ARIA_역할과_섹션_앵커와_기사_수를_가진다_T039() {
        assertThat(page.select("nav[role=tablist] a[role=tab]")).hasSize(5).allSatisfy(tab -> {
            String target = tab.attr("aria-controls");
            assertThat(tab.attr("href")).isEqualTo("#" + target);
            assertThat(page.getElementById(target).attr("role")).isEqualTo("tabpanel");
            assertThat(page.getElementById(target).attr("aria-labelledby")).isEqualTo(tab.id());
        });
        assertThat(page.select("a[role=tab]")).extracting(tab -> tab.attr("aria-selected"))
                .containsExactly("true", "false", "false", "false", "false");
        assertThat(page.select("a[role=tab] .count")).extracting(Element::text)
                .containsExactly("2건", "1건", "1건", "0건", "1건");
        assertThat(page.select("nav[role=tablist] [data-tabs-indicator]")).hasSize(1);
    }

    @Test
    void 오늘의_뉴스피드는_루트_경로로_자원을_참조하고_Today가_현재_페이지다() {
        assertThat(page.select("link[rel=stylesheet]")).extracting(link -> link.attr("href"))
                .containsExactly("assets/fonts/pretendard.css", "assets/app.css");
        assertThat(page.selectFirst("a[aria-current=page]").attr("href")).isEqualTo("today.html");
        assertThat(page.select("[data-day-nav]")).isEmpty();
    }

    @Test
    void 날짜별_페이지는_상위_경로로_자원을_참조하고_캘린더로_돌아가는_링크가_있다_FR_3_6() {
        assertThat(archivePage.selectFirst("link[rel=stylesheet]").attr("href")).isEqualTo("../assets/fonts/pretendard.css");
        assertThat(archivePage.selectFirst("script[src]").attr("src")).isEqualTo("../assets/theme-init.js");
        assertThat(archivePage.select("a[aria-current]")).isEmpty();

        Element dayNav = archivePage.selectFirst("nav[data-day-nav]");
        assertThat(dayNav.attr("data-date")).isEqualTo("2026-10-28");
        assertThat(dayNav.attr("data-base")).isEqualTo("../");
        assertThat(dayNav.select("a[href=../calendar.html]").text()).isEqualTo("캘린더로 돌아가기");
        assertThat(dayNav.select("[data-day-prev], [data-day-next]"))
                .hasSize(2).allSatisfy(link -> assertThat(link.hasAttr("hidden")).isTrue());
    }

    @Test
    void today와_날짜별_페이지와_자원을_쓴다() throws IOException {
        OutputPaths paths = new OutputPaths(root);

        renderer.write(TestNewsletters.sample(), paths);

        assertThat(paths.siteToday()).exists();
        assertThat(paths.siteIndex()).doesNotExist();
        assertThat(paths.archivePage(TestNewsletters.DATE)).exists();
        for (String asset : List.of("app.css", "app.js", "theme-init.js", "style.css", "tabs.js",
                "fonts/pretendard.css", "fonts/LICENSE")) {
            assertThat(paths.assetsDir().resolve(asset)).as(asset).exists();
        }
        assertThat(SiteAssets.fontFiles()).hasSize(92)
                .allSatisfy(font -> assertThat(paths.fontsDir().resolve(font)).exists());
        assertThat(Files.readString(paths.siteToday(), StandardCharsets.UTF_8)).contains("2026-10-28 (수)");
    }
}
