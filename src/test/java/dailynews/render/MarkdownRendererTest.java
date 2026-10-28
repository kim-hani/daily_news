package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.Fixtures;
import dailynews.model.Newsletter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();
    private final Newsletter newsletter = TestNewsletters.sample();

    @TempDir
    Path root;

    @Test
    void golden_file과_같다() {
        assertThat(renderer.render(newsletter)).isEqualTo(Fixtures.text("/golden/newsletter.md"));
    }

    @Test
    void 모든_기사에_원문_URL_문자열이_있다_SC3_1() {
        String markdown = renderer.render(newsletter);

        long articles = Pattern.compile("(?m)^### \\d+\\. ").matcher(markdown).results().count();
        long urls = Pattern.compile("(?m)^- 원문: https://").matcher(markdown).results().count();
        assertThat(articles).isEqualTo(5);
        assertThat(urls).isEqualTo(articles);
    }

    @Test
    void 제목과_요약의_Markdown_특수문자를_이스케이프한다() {
        String markdown = renderer.render(newsletter);

        assertThat(markdown).contains("### 2. \\[단독\\] \\<script\\>alert(1)\\</script\\> \\*주의\\* 국회\\_예산 \\#1")
                .contains("> 요약에 \\<b\\>태그\\</b\\>와 \\[괄호\\]가 있다. _(RSS 요약)_")
                .doesNotContain("<script>");
    }

    @Test
    void news_날짜_경로에_덮어쓴다() throws IOException {
        OutputPaths paths = new OutputPaths(root);
        Files.createDirectories(root.resolve("news/2026/10"));
        Files.writeString(root.resolve("news/2026/10/2026-10-28.md"), "이전 내용");

        Path written = renderer.write(newsletter, paths);

        assertThat(written).isEqualTo(root.resolve("news/2026/10/2026-10-28.md"));
        assertThat(Files.readString(written, StandardCharsets.UTF_8)).isEqualTo(renderer.render(newsletter));
    }
}
