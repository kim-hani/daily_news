package dailynews.render;

import static dailynews.TestArticles.at;
import static org.assertj.core.api.Assertions.assertThat;

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

class ArchiveRendererTest {

    private final ArchiveRenderer renderer = new ArchiveRenderer("https://github.com/kim-hani/daily_news", 30);

    @TempDir
    Path root;

    @Test
    void 연월별로_묶고_최신순으로_보여_준다() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        touch(paths.archivePage(LocalDate.of(2026, 9, 30)));
        touch(paths.archivePage(LocalDate.of(2026, 10, 28)));
        touch(paths.archivePage(LocalDate.of(2026, 10, 1)));
        touch(paths.archiveDir().resolve("notes.html"));

        renderer.write(paths, at(9, 0));

        Document page = Jsoup.parse(Files.readString(paths.archiveIndex(), StandardCharsets.UTF_8));
        assertThat(page.select("section.month h2")).extracting(Element::text).containsExactly("2026년 10월", "2026년 9월");
        assertThat(page.select("section.month li a")).extracting(link -> link.attr("href")).containsExactly(
                "archive/2026-10-28.html", "archive/2026-10-01.html", "archive/2026-09-30.html");
        assertThat(page.select("section.month li a").first().text()).isEqualTo("10월 28일 (수)");
    }

    @Test
    void 보관_정리로_지운_날짜는_목록에_없다() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        LocalDate today = LocalDate.of(2026, 10, 28);
        touch(paths.archivePage(today));
        touch(paths.archivePage(today.minusDays(30)));

        new RetentionCleaner(new AppConfig.Retention(30, 0)).clean(paths, today);
        renderer.write(paths, at(9, 0));

        String html = Files.readString(paths.archiveIndex(), StandardCharsets.UTF_8);
        assertThat(html).contains("archive/2026-10-28.html").doesNotContain("2026-09-28");
    }

    @Test
    void 오래된_뉴스레터는_Markdown_아카이브로_안내한다() {
        Document page = Jsoup.parse(renderer.render(List.of(), at(9, 0)));

        assertThat(page.select("p.note a").attr("href")).isEqualTo("https://github.com/kim-hani/daily_news/tree/main/news");
        assertThat(page.select("p.note").text()).contains("최근 30일치");
        assertThat(page.select("p.empty").text()).isEqualTo("아직 보관된 뉴스레터가 없습니다.");
    }

    private static void touch(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, "x");
    }
}
