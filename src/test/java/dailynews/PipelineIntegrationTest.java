package dailynews;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.config.ConfigLoader;
import dailynews.model.Category;
import dailynews.model.NewsItem;
import dailynews.model.Newsletter;
import dailynews.model.RunStats;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * fixture 웹, 임시 폴더, 고정 시각으로 전체 흐름을 실행한다(SC3, SC3-1, SC4, SC9, FR-7.4).
 */
class PipelineIntegrationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 28);

    @TempDir
    Path root;

    @Test
    void 산출물을_모두_만든다() throws IOException {
        DailyNewsPipeline.Result result = run();

        assertThat(result.exitCode()).isZero();
        assertThat(root.resolve("news/2026/10/2026-10-28.md")).exists();
        assertThat(root.resolve("site/index.html")).exists();
        assertThat(root.resolve("site/archive/2026-10-28.html")).exists();
        assertThat(root.resolve("site/archive.html")).exists();
        assertThat(root.resolve("site/assets/style.css")).exists();
        assertThat(read("README.md")).contains("<!-- LATEST:START -->", "최신 뉴스레터: 2026-10-28 (수)",
                "news/2026/10/2026-10-28.md", "<!-- LATEST:END -->");
        assertThat(read("site/archive.html")).contains("archive/2026-10-28.html");
    }

    @Test
    void 카테고리_5개가_모두_있고_기사가_있다_SC3() throws IOException {
        Newsletter newsletter = run().newsletter();

        assertThat(newsletter.sections().keySet()).containsExactly(Category.values());
        assertThat(newsletter.sections().values()).allSatisfy(items -> assertThat(items).isNotEmpty().hasSizeLessThanOrEqualTo(5));

        Document page = Jsoup.parse(read("site/index.html"));
        assertThat(page.select("main section.category")).hasSize(5);
    }

    @Test
    void 모든_기사에_원문_URL이_있다_SC3_1() throws IOException {
        Newsletter newsletter = run().newsletter();
        int selected = newsletter.stats().selected();

        Document page = Jsoup.parse(read("site/index.html"));
        assertThat(page.select("article.card")).hasSize(selected)
                .allSatisfy(card -> assertThat(card.select("p.url a").text()).startsWith("https://"));
        assertThat(read("news/2026/10/2026-10-28.md").split("\n- 원문: https://", -1)).hasSize(selected + 1);
    }

    @Test
    void 본문_기반_요약이_90퍼센트_이상이다_SC4() {
        RunStats stats = run().newsletter().stats();

        assertThat(stats.bodyOk() + stats.bodyFallback()).isEqualTo(stats.selected());
        assertThat((double) stats.bodyOk() / stats.selected()).isGreaterThanOrEqualTo(0.9);
    }

    @Test
    void 같은_기사는_여러_카테고리에_나오지_않는다() {
        Newsletter newsletter = run().newsletter();

        List<String> urls = newsletter.sections().values().stream().flatMap(List::stream)
                .map(NewsItem::story).map(story -> story.representative().url().toString()).toList();
        assertThat(urls).doesNotHaveDuplicates();
    }

    @Test
    void 두_번_실행해도_결과_파일이_바이트_단위로_같다_FR_7_4() throws IOException {
        run();
        Map<String, byte[]> first = snapshot();
        run();

        Map<String, byte[]> second = snapshot();
        assertThat(second.keySet()).isEqualTo(first.keySet());
        first.forEach((path, bytes) -> assertThat(second.get(path)).as(path).isEqualTo(bytes));
    }

    @Test
    void 이전_31일치가_있으면_웹은_30개만_남고_Markdown은_모두_남는다_SC9() throws IOException {
        for (int i = 1; i <= 31; i++) {
            LocalDate day = DATE.minusDays(i);
            write("site/archive/" + day + ".html", "old");
            write("news/%d/%02d/%s.md".formatted(day.getYear(), day.getMonthValue(), day), "old");
        }

        run();

        try (Stream<Path> pages = Files.list(root.resolve("site/archive"))) {
            assertThat(pages).hasSize(30);
        }
        assertThat(root.resolve("site/archive/" + DATE.minusDays(29) + ".html")).exists();
        assertThat(root.resolve("site/archive/" + DATE.minusDays(30) + ".html")).doesNotExist();
        try (Stream<Path> markdown = Files.walk(root.resolve("news"))) {
            assertThat(markdown.filter(path -> path.toString().endsWith(".md"))).hasSize(32);
        }
        assertThat(read("site/archive.html")).doesNotContain(DATE.minusDays(30).toString())
                .contains(DATE.minusDays(29).toString());
    }

    private DailyNewsPipeline.Result run() {
        return new DailyNewsPipeline(ConfigLoader.loadDefault(), FixtureWeb.withArticles(), FixtureWeb.CLOCK)
                .run(DATE, root, false);
    }

    private Map<String, byte[]> snapshot() throws IOException {
        Map<String, byte[]> files = new TreeMap<>();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                files.put(root.relativize(path).toString(), Files.readAllBytes(path));
            }
        }
        return files;
    }

    private String read(String path) throws IOException {
        return Files.readString(root.resolve(path), StandardCharsets.UTF_8);
    }

    private void write(String path, String content) throws IOException {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
