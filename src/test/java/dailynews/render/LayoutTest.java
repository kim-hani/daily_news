package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.render.Chrome.Nav;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

/**
 * 공통 head·내비게이션·푸터 조각(T-006, T-008, FR-1.4, FR-1.5, FR-7.3).
 */
class LayoutTest {

    private static final String REPOSITORY = "https://github.com/kim-hani/daily_news";

    @Test
    void head에서_theme_init은_스타일시트보다_먼저_동기로_실행되고_app_js는_defer다() {
        Document page = Jsoup.parse(Templates.process("newsletter",
                Map.of("page", new FeedRenderer(REPOSITORY).page(TestNewsletters.sample(), false))));

        List<Element> resources = page.head().select("script[src], link[rel=stylesheet]");
        assertThat(resources.get(0).attr("src")).isEqualTo("assets/theme-init.js");
        assertThat(resources.get(0).hasAttr("defer") || resources.get(0).hasAttr("async")).isFalse();
        assertThat(page.head().selectFirst("script[src=assets/app.js]").hasAttr("defer")).isTrue();
        assertThat(page.head().selectFirst("meta[name=color-scheme]").attr("content")).isEqualTo("light dark");
        assertThat(page.head().selectFirst("meta[http-equiv=Content-Security-Policy]").attr("content"))
                .contains("default-src 'self'", "script-src 'self'", "style-src 'self'", "object-src 'none'");
        assertThat(page.head().selectFirst("meta[name=referrer]").attr("content")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(page.head().selectFirst("meta[name=description]").attr("content")).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(Nav.class)
    void 지금_페이지만_aria_current가_붙는다_FR_1_4(Nav current) {
        Document nav = navbar(current);

        List<Element> marked = nav.select("[aria-current]");
        switch (current) {
            case HOME -> assertThat(marked).extracting(e -> e.attr("href")).containsExactly("index.html");
            case TODAY -> assertThat(marked).extracting(e -> e.attr("href")).containsExactly("today.html");
            case CALENDAR -> assertThat(marked).extracting(e -> e.attr("href")).containsExactly("calendar.html");
            case NONE -> assertThat(marked).isEmpty();
        }
        assertThat(marked).allSatisfy(e -> assertThat(e.attr("aria-current")).isEqualTo("page"));
        assertThat(nav.select("header a[href]")).extracting(e -> e.attr("href"))
                .containsExactly("#main", "index.html", "today.html", "calendar.html");
    }

    @Test
    void 배경_전환은_라디오_3개이고_JS_없이는_숨는다_FR_7_3_FR_7_7() {
        Element switcher = navbar(Nav.HOME).selectFirst("fieldset[data-theme-switcher]");

        // JS가 없을 때만 CSS(.no-js .js-only)로 숨긴다. hidden이면 app.js가 보여 줄 때 메뉴가 밀린다(CLS).
        assertThat(switcher.hasClass("js-only")).isTrue();
        assertThat(switcher.hasAttr("hidden")).isFalse();
        assertThat(switcher.selectFirst("legend").text()).isEqualTo("배경");
        assertThat(switcher.select("input[type=radio]")).extracting(e -> e.attr("value"), e -> e.attr("aria-label"),
                        e -> e.attr("name"))
                .containsExactly(org.assertj.core.groups.Tuple.tuple("light", "White", "dn-theme"),
                        org.assertj.core.groups.Tuple.tuple("dark", "Dark", "dn-theme"),
                        org.assertj.core.groups.Tuple.tuple("system", "System", "dn-theme"));
    }

    @Test
    void 날짜별_페이지는_상위_경로로_링크한다() {
        assertThat(navbar(Nav.NONE, "../").select("header a[href]")).extracting(e -> e.attr("href"))
                .containsExactly("#main", "../index.html", "../today.html", "../calendar.html");
    }

    @Test
    void 푸터에_인용_고지와_생성_시각과_저장소_링크가_있다_FR_1_5() {
        Document page = Jsoup.parse(new HomeRenderer(REPOSITORY).render(TestNewsletters.sample()));

        Element footer = page.selectFirst("footer");
        assertThat(footer.text()).contains("저작권은 각 언론사에 있습니다", "생성: 2026-10-28 09:00 KST");
        assertThat(footer.selectFirst("a").attr("href")).isEqualTo(REPOSITORY);
    }

    @Test
    void 새_경로는_OutputPaths에서만_정한다() {
        OutputPaths paths = new OutputPaths(Path.of("root"));

        assertThat(paths.siteToday()).isEqualTo(Path.of("root", "site", "today.html"));
        assertThat(paths.calendarPage()).isEqualTo(Path.of("root", "site", "calendar.html"));
        assertThat(paths.archiveIndex()).isEqualTo(Path.of("root", "site", "archive.html"));
        assertThat(paths.dataDir()).isEqualTo(Path.of("root", "site", "data"));
        assertThat(paths.archiveJson()).isEqualTo(Path.of("root", "site", "data", "archive.json"));
        assertThat(paths.fontsDir()).isEqualTo(Path.of("root", "site", "assets", "fonts"));
    }

    private static Document navbar(Nav current) {
        return navbar(current, "");
    }

    private static Document navbar(Nav current, String base) {
        Chrome chrome = new Chrome("t", "d", base, current, "2026-10-28 09:00 KST", REPOSITORY);
        HomeRenderer.HomePage home = new HomeRenderer.HomePage(chrome, "2026-10-28 (수)", "2026-10-28", "today.html",
                "calendar.html");
        return Jsoup.parse(Templates.process("home", Map.of("home", home)));
    }
}
