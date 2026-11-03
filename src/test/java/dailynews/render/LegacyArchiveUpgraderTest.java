package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyArchiveUpgraderTest {

    private static final LocalDate DATE = TestNewsletters.DATE;

    @TempDir
    Path root;

    private final FeedRenderer feed = new FeedRenderer("https://github.com/kim-hani/daily_news");
    private final LegacyArchiveUpgrader upgrader = new LegacyArchiveUpgrader(feed);

    @Test
    void Markdown이_있는_옛_페이지는_새_디자인으로_바꾼다() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        copyOldDesign(paths.archivePage(DATE));
        new MarkdownRenderer().write(TestNewsletters.sample(), paths);

        assertThat(upgrader.upgrade(paths)).containsExactly(DATE);

        Document page = Jsoup.parse(Files.readString(paths.archivePage(DATE), StandardCharsets.UTF_8));
        assertThat(page.selectFirst("link[href=../assets/app.css]")).isNotNull();
        assertThat(page.selectFirst("link[href$=style.css]")).isNull();
        assertThat(page.selectFirst("meta[name=dn:selected]").attr("content")).isEqualTo("5");
        assertThat(page.select("article.card")).hasSize(5);
        assertThat(page.select("[data-day-nav]")).hasSize(1);
        // 날짜별 페이지만 바꾸고 today.html은 만들지 않는다
        assertThat(paths.siteToday()).doesNotExist();
        // 원래 뉴스레터로 만든 페이지와 같다
        assertThat(Files.readString(paths.archivePage(DATE), StandardCharsets.UTF_8))
                .isEqualTo(feed.render(TestNewsletters.sample(), true));
    }

    @Test
    void 새_디자인_페이지는_건드리지_않는다() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        feed.writeArchive(TestNewsletters.sample(), paths);
        new MarkdownRenderer().write(TestNewsletters.sample(), paths);
        Files.writeString(paths.archivePage(DATE), Files.readString(paths.archivePage(DATE)) + "<!-- 표시 -->");
        String before = Files.readString(paths.archivePage(DATE), StandardCharsets.UTF_8);

        assertThat(upgrader.upgrade(paths)).isEmpty();

        assertThat(Files.readString(paths.archivePage(DATE), StandardCharsets.UTF_8)).isEqualTo(before);
    }

    @Test
    void Markdown이_없거나_읽지_못하면_옛_페이지를_그대로_둔다() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        LocalDate noMarkdown = DATE.minusDays(1);
        copyOldDesign(paths.archivePage(DATE));
        copyOldDesign(paths.archivePage(noMarkdown));
        Formats.write(paths.markdown(DATE), "깨진 Markdown");
        byte[] before = Files.readAllBytes(paths.archivePage(DATE));

        assertThat(upgrader.upgrade(paths)).isEmpty();

        assertThat(Files.readAllBytes(paths.archivePage(DATE))).isEqualTo(before);
        assertThat(Files.readString(paths.archivePage(noMarkdown), StandardCharsets.UTF_8)).contains("assets/style.css");
    }

    @Test
    void 날짜_페이지가_없으면_아무것도_하지_않는다() throws IOException {
        assertThat(upgrader.upgrade(new OutputPaths(root))).isEmpty();
    }

    private void copyOldDesign(Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (InputStream in = getClass().getResourceAsStream("/fixtures/archive/old-design.html")) {
            Files.copy(in, target);
        }
    }
}
