package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReadmeUpdaterTest {

    private static final String BLOCK = ReadmeUpdater.START + "\n새 내용\n" + ReadmeUpdater.END;

    private final ReadmeUpdater updater = new ReadmeUpdater("https://kim-hani.github.io/daily_news/");

    @TempDir
    Path root;

    @Test
    void 마커_안만_바꾸고_나머지는_그대로_둔다() {
        String readme = "# 제목\n\n소개\n\n" + ReadmeUpdater.START + "\n이전 내용\n" + ReadmeUpdater.END + "\n\n## 사용법\n";

        assertThat(ReadmeUpdater.apply(readme, BLOCK)).isEqualTo("# 제목\n\n소개\n\n" + BLOCK + "\n\n## 사용법\n");
    }

    @Test
    void 마커가_없으면_파일_끝에_붙인다() {
        assertThat(ReadmeUpdater.apply("# 제목\n소개", BLOCK)).isEqualTo("# 제목\n소개\n\n" + BLOCK + "\n");
        assertThat(ReadmeUpdater.apply("# 제목\n\n", BLOCK)).isEqualTo("# 제목\n\n" + BLOCK + "\n");
    }

    @Test
    void 두_번_실행해도_결과가_같다() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        Files.writeString(paths.readme(), "# Daily News\n\n소개 문단\n");

        updater.update(paths, TestNewsletters.sample());
        String once = Files.readString(paths.readme(), StandardCharsets.UTF_8);
        updater.update(paths, TestNewsletters.sample());

        assertThat(Files.readString(paths.readme(), StandardCharsets.UTF_8)).isEqualTo(once);
        assertThat(once).startsWith("# Daily News\n\n소개 문단\n\n" + ReadmeUpdater.START);
    }

    @Test
    void 최신_뉴스레터_날짜_웹_주소_Markdown_링크_키워드를_넣는다() {
        String block = updater.block(TestNewsletters.sample(), "news/2026/10/2026-10-28.md");

        assertThat(block).startsWith(ReadmeUpdater.START).endsWith(ReadmeUpdater.END)
                .contains("최신 뉴스레터: 2026-10-28 (수)")
                .contains("https://kim-hani.github.io/daily_news/")
                .contains("[news/2026/10/2026-10-28.md](news/2026/10/2026-10-28.md)")
                .contains("선정 5건 · 🔑 SCM · 반도체 · 국회");
    }

    @Test
    void README가_없으면_새로_만든다() throws IOException {
        OutputPaths paths = new OutputPaths(root);

        updater.update(paths, TestNewsletters.sample());

        assertThat(Files.readString(paths.readme(), StandardCharsets.UTF_8)).startsWith("# Daily News\n\n" + ReadmeUpdater.START);
    }
}
