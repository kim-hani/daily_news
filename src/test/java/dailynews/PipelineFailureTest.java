package dailynews;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.config.ConfigLoader;
import dailynews.http.FakeHttpFetcher;
import dailynews.model.Newsletter;
import dailynews.model.Source;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 부분 실패와 전체 실패(SC6, NFR-2).
 */
class PipelineFailureTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 28);

    @TempDir
    Path root;

    @Test
    void 한_언론사의_피드가_모두_500이어도_뉴스레터를_만들고_실패를_기록한다() throws IOException {
        FakeHttpFetcher fetcher = FixtureWeb.withArticles();
        TestArticles.YONHAP.feeds().values().forEach(feed -> fetcher.respond(feed.toString(), 500, ""));

        DailyNewsPipeline.Result result = run(fetcher);

        assertThat(result.exitCode()).isZero();
        Newsletter newsletter = result.newsletter();
        assertThat(newsletter.stats().failedFeeds()).containsExactly(
                "yonhap/POLITICS", "yonhap/SOCIETY", "yonhap/ECONOMY", "yonhap/IT", "yonhap/GLOBAL");
        assertThat(newsletter.sections().values()).allSatisfy(items -> assertThat(items).isNotEmpty());
        assertThat(newsletter.sections().values().stream().flatMap(java.util.List::stream))
                .noneMatch(item -> item.story().representative().source().id().equals("yonhap"));
        assertThat(Files.readString(root.resolve("news/2026/10/2026-10-28.md"), StandardCharsets.UTF_8))
                .contains("수집하지 못한 피드: yonhap/POLITICS");
    }

    @Test
    void 기사_본문이_모두_404면_RSS_요약으로_대체한다() throws IOException {
        DailyNewsPipeline.Result result = run(FixtureWeb.feedsOnly());

        assertThat(result.exitCode()).isZero();
        assertThat(result.newsletter().stats().bodyOk()).isZero();
        assertThat(result.newsletter().stats().bodyFallback()).isEqualTo(result.newsletter().stats().selected());
        assertThat(Files.readString(root.resolve("site/index.html"), StandardCharsets.UTF_8)).contains("RSS 요약");
    }

    @Test
    void 모든_피드가_실패하면_종료_코드_1이고_파일과_README를_바꾸지_않는다() throws IOException {
        FakeHttpFetcher fetcher = FixtureWeb.withArticles();
        for (Source source : TestArticles.SOURCES) {
            source.feeds().values().forEach(feed -> fetcher.respond(feed.toString(), 503, ""));
        }
        Path readme = Files.writeString(root.resolve("README.md"), "# 기존 README\n");

        DailyNewsPipeline.Result result = run(fetcher);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.newsletter()).isNull();
        assertThat(root.resolve("news")).doesNotExist();
        assertThat(root.resolve("site")).doesNotExist();
        assertThat(Files.readString(readme, StandardCharsets.UTF_8)).isEqualTo("# 기존 README\n");
    }

    private DailyNewsPipeline.Result run(FakeHttpFetcher fetcher) {
        return new DailyNewsPipeline(ConfigLoader.loadDefault(), fetcher, FixtureWeb.CLOCK).run(DATE, root, false);
    }
}
