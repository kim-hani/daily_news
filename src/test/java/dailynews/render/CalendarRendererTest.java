package dailynews.render;

import static dailynews.TestArticles.at;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dailynews.config.AppConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CalendarRendererTest {

    private final CalendarRenderer renderer = new CalendarRenderer("https://github.com/kim-hani/daily_news", 30);
    private final FeedRenderer feed = new FeedRenderer("https://github.com/kim-hani/daily_news");

    @TempDir
    Path root;

    @Test
    void 달력은_일요일부터_7열이고_보관된_날만_그날_뉴스피드로_링크한다_SC2() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        feed.write(TestNewsletters.on(LocalDate.of(2026, 10, 30), 0, false), paths);
        feed.write(TestNewsletters.on(LocalDate.of(2026, 11, 2), 1, false), paths);

        renderer.write(paths, at(12, 3));

        Document page = read(paths.calendarPage());
        assertThat(page.select("[data-calendar-month]")).extracting(month -> month.attr("data-calendar-month"))
                .containsExactly("2026-10", "2026-11");
        assertThat(page.select("[data-calendar-month]")).allSatisfy(month -> assertThat(month.hasAttr("hidden")).isFalse());
        assertThat(page.select("table thead th[scope=col]")).hasSize(14);
        assertThat(page.select("table thead").first().select("th")).extracting(Element::text)
                .containsExactly("일", "월", "화", "수", "목", "금", "토");
        assertThat(page.select("table tbody tr")).allSatisfy(row -> assertThat(row.select("td")).hasSize(7));

        List<Element> links = page.select("a.calendar-day");
        assertThat(links).extracting(link -> link.attr("href"))
                .containsExactly("archive/2026-10-30.html", "archive/2026-11-02.html");
        assertThat(links).allSatisfy(link -> assertThat(paths.siteDir().resolve(link.attr("href"))).exists());
        assertThat(page.select(".day-off")).allSatisfy(day -> {
            assertThat(day.tagName()).isEqualTo("span");
            assertThat(day.select("a")).isEmpty();
        });
        assertThat(page.select(".day-off")).hasSize(31 + 30 - 2);
    }

    @Test
    void 최신_날은_강조하고_aria_label로_읽을_수_있다_FR_4_3() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        feed.write(TestNewsletters.on(LocalDate.of(2026, 10, 30), 0, false), paths);
        feed.write(TestNewsletters.on(LocalDate.of(2026, 10, 31), 1, false), paths);

        renderer.write(paths, at(12, 3));

        Document page = read(paths.calendarPage());
        assertThat(page.select("a.calendar-day.latest")).hasSize(1).first().satisfies(latest -> {
            assertThat(latest.attr("href")).isEqualTo("archive/2026-10-31.html");
            assertThat(latest.hasClass("calendar-day-latest")).isTrue();
            assertThat(latest.attr("aria-label"))
                    .isEqualTo("2026년 10월 31일 토요일, 최신 뉴스레터, 선정 5건, 키워드 트럼프·반도체·국회");
            assertThat(latest.select(".calendar-popover span")).extractingResultOf("text")
                    .containsExactly("키워드", "트럼프", "반도체", "국회");
        });
        assertThat(page.selectFirst("a[href=archive/2026-10-30.html]").attr("aria-label"))
                .startsWith("2026년 10월 30일 금요일, 뉴스레터 있음, 선정 5건, 키워드 AI·");
        assertThat(page.selectFirst("[data-calendar]").attr("data-latest")).isEqualTo("2026-10");
        // JS가 있으면 첫 화면부터 이 달만 그린다(app.css, CLS)
        assertThat(page.select("[data-calendar-month].calendar-month-latest")).extracting(month -> month.id())
                .containsExactly("2026-10");
    }

    @Test
    void 월_이동_버튼과_배경_전환은_JS가_있을_때만_보인다_FR_4_6() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        feed.write(TestNewsletters.sample(), paths);

        renderer.write(paths, at(12, 3));

        Document page = read(paths.calendarPage());
        Element nav = page.selectFirst("[data-calendar-nav]");
        assertThat(nav.hasClass("js-only")).isTrue();
        assertThat(nav.hasAttr("hidden")).isFalse();
        assertThat(nav.select("button[data-calendar-prev][aria-label=이전 달]")).hasSize(1);
        assertThat(nav.select("button[data-calendar-next][aria-label=다음 달]")).hasSize(1);
        assertThat(nav.selectFirst("[data-calendar-label]").attr("aria-live")).isEqualTo("polite");
        assertThat(page.selectFirst("a[aria-current=page]").attr("href")).isEqualTo("calendar.html");
    }

    @Test
    void 보관_안내와_Markdown_아카이브_링크와_빈_보관_안내가_있다_FR_4_5_FR_4_9() throws IOException {
        OutputPaths paths = new OutputPaths(root);

        renderer.write(paths, at(12, 3));

        Document page = read(paths.calendarPage());
        assertThat(page.select("p.note a").attr("href")).isEqualTo("https://github.com/kim-hani/daily_news/tree/main/news");
        assertThat(page.select("p.note").text()).contains("최근 30일치");
        assertThat(page.select("p.empty").text()).isEqualTo("아직 보관된 뉴스레터가 없습니다.");
        assertThat(page.select("[data-calendar]")).isEmpty();
        JsonNode json = new ObjectMapper().readTree(Files.readString(paths.archiveJson()));
        assertThat(json.get("latest").isNull()).isTrue();
        assertThat(json.get("dates")).isEmpty();
    }

    @Test
    void 옛_디자인_페이지도_캘린더에_키워드와_함께_나온다() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        Files.createDirectories(paths.archiveDir());
        try (var old = getClass().getResourceAsStream("/fixtures/archive/old-design.html")) {
            Files.copy(old, paths.archivePage(LocalDate.of(2026, 10, 31)));
        }

        renderer.write(paths, at(12, 3));

        Element day = read(paths.calendarPage()).selectFirst("a[href=archive/2026-10-31.html]");
        assertThat(day.attr("aria-label")).contains("선정 25건", "키워드 AI·트럼프");
    }

    @Test
    void archive_json은_보관_파일과_같은_날짜를_최신순으로_담고_패턴_밖_파일은_뺀다() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        touch(paths.archivePage(LocalDate.of(2026, 9, 30)));
        touch(paths.archivePage(LocalDate.of(2026, 10, 28)));
        touch(paths.archivePage(LocalDate.of(2026, 10, 1)));
        touch(paths.archiveDir().resolve("notes.html"));
        touch(paths.archiveDir().resolve("2026-13-01.html"));

        renderer.write(paths, at(12, 3));

        JsonNode json = new ObjectMapper().readTree(Files.readString(paths.archiveJson(), StandardCharsets.UTF_8));
        assertThat(json.get("generatedAt").asText()).isEqualTo("2026-10-28T12:03:00+09:00");
        assertThat(json.get("latest").asText()).isEqualTo("2026-10-28");
        assertThat(json.get("dates")).extracting(JsonNode::asText)
                .containsExactly("2026-10-28", "2026-10-01", "2026-09-30");
    }

    @Test
    void 보관_정리로_지운_날짜는_캘린더와_archive_json에_없다_FR_6_3() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        LocalDate today = LocalDate.of(2026, 10, 28);
        touch(paths.archivePage(today));
        touch(paths.archivePage(today.minusDays(30)));

        new RetentionCleaner(new AppConfig.Retention(30, 0)).clean(paths, today);
        renderer.write(paths, at(12, 3));

        assertThat(Files.readString(paths.calendarPage())).contains("archive/2026-10-28.html").doesNotContain("2026-09-28.html");
        assertThat(Files.readString(paths.archiveJson())).contains("2026-10-28").doesNotContain("2026-09-28");
    }

    @Test
    void 옛_주소_archive_html은_캘린더로_리다이렉트한다_FR_1_3() throws IOException {
        OutputPaths paths = new OutputPaths(root);

        renderer.write(paths, at(12, 3));

        Document redirect = read(paths.archiveIndex());
        assertThat(redirect.selectFirst("meta[http-equiv=refresh]").attr("content")).isEqualTo("0; url=calendar.html");
        assertThat(redirect.selectFirst("link[rel=canonical]").attr("href")).isEqualTo("calendar.html");
        assertThat(redirect.select("main a[href=calendar.html]").text()).isEqualTo("뉴스 캘린더");
        assertThat(redirect.selectFirst("html").attr("lang")).isEqualTo("ko");
    }

    private static Document read(Path path) throws IOException {
        return Jsoup.parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static void touch(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, "x");
    }
}
