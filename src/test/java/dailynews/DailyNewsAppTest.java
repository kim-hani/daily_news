package dailynews;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.http.FakeHttpFetcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DailyNewsAppTest {

    @TempDir
    Path root;

    @Test
    void 잘못된_옵션이면_사용법을_남기고_종료_코드_1이다() {
        assertThat(run("--verbose")).isEqualTo(1);
        assertThat(run("--date", "어제")).isEqualTo(1);
    }

    @Test
    void 설정_파일이_없거나_잘못되면_종료_코드_1이다() throws IOException {
        assertThat(run("--config", root.resolve("missing.yml").toString())).isEqualTo(1);

        Path broken = Files.writeString(root.resolve("broken.yml"), "perCategory: 0\n");
        assertThat(run("--config", broken.toString())).isEqualTo(1);
    }

    @Test
    void dry_run이면_파일을_쓰지_않는다() throws IOException {
        assertThat(run("--root", root.toString(), "--dry-run")).isZero();

        try (Stream<Path> files = Files.list(root)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void 날짜를_주면_그_날짜_경로에_쓴다() {
        assertThat(run("--root", root.toString(), "--date", "2026-10-29")).isZero();

        assertThat(root.resolve("news/2026/10/2026-10-29.md")).exists();
        assertThat(root.resolve("site/archive/2026-10-29.html")).exists();
    }

    @Test
    void 날짜를_주지_않으면_실행일_KST로_쓴다() {
        assertThat(run("--root", root.toString())).isZero();

        assertThat(root.resolve("news/2026/10/2026-10-28.md")).exists();
    }

    private int run(String... args) {
        FakeHttpFetcher fetcher = FixtureWeb.withArticles();
        return DailyNewsApp.run(args, FixtureWeb.CLOCK, config -> fetcher);
    }
}
