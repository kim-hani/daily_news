package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.config.AppConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RetentionCleanerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

    @TempDir
    Path root;

    private OutputPaths paths;

    @BeforeEach
    void setUp() {
        paths = new OutputPaths(root);
    }

    @Test
    void 날짜_파일_31개_중_가장_오래된_1개만_지운다() throws IOException {
        for (int i = 0; i <= 30; i++) {
            touch(paths.archivePage(TODAY.minusDays(i)));
        }

        List<Path> deleted = new RetentionCleaner(new AppConfig.Retention(30, 0)).clean(paths, TODAY);

        assertThat(deleted).containsExactly(paths.archivePage(TODAY.minusDays(30)));
        assertThat(paths.archivePage(TODAY.minusDays(29))).exists();
        assertThat(archiveFiles()).hasSize(30);
    }

    @Test
    void 패턴_외_파일_미래_날짜_파일_하위_폴더는_지우지_않는다() throws IOException {
        Path old = touch(paths.archivePage(TODAY.minusDays(100)));
        Path future = touch(paths.archivePage(TODAY.plusDays(3)));
        Path other = touch(paths.archiveDir().resolve("notes.html"));
        Path wrongName = touch(paths.archiveDir().resolve("2026-01-01.htm"));
        Path invalidDate = touch(paths.archiveDir().resolve("2026-13-45.html"));
        Path subDir = Files.createDirectories(paths.archiveDir().resolve("2020-01-01.html.d"));

        new RetentionCleaner(new AppConfig.Retention(30, 0)).clean(paths, TODAY);

        assertThat(old).doesNotExist();
        assertThat(future).exists();
        assertThat(other).exists();
        assertThat(wrongName).exists();
        assertThat(invalidDate).exists();
        assertThat(subDir).exists();
    }

    @Test
    void newsDays가_0이면_Markdown은_지우지_않는다() throws IOException {
        Path oldMarkdown = touch(paths.markdown(TODAY.minusDays(400)));

        new RetentionCleaner(new AppConfig.Retention(30, 0)).clean(paths, TODAY);

        assertThat(oldMarkdown).exists();
    }

    @Test
    void newsDays가_있으면_Markdown도_지우고_빈_월_폴더를_정리한다() throws IOException {
        Path keep = touch(paths.markdown(TODAY.minusDays(6)));
        Path drop = touch(paths.markdown(LocalDate.of(2025, 12, 31)));

        List<Path> deleted = new RetentionCleaner(new AppConfig.Retention(30, 7)).clean(paths, TODAY);

        assertThat(deleted).containsExactly(drop);
        assertThat(keep).exists();
        assertThat(root.resolve("news/2025/12")).doesNotExist();
        assertThat(root.resolve("news/2025")).doesNotExist();
    }

    @Test
    void 폴더가_없어도_실패하지_않는다() throws IOException {
        assertThat(new RetentionCleaner(new AppConfig.Retention(30, 7)).clean(paths, TODAY)).isEmpty();
    }

    private List<Path> archiveFiles() throws IOException {
        try (Stream<Path> files = Files.list(paths.archiveDir())) {
            return files.toList();
        }
    }

    private static Path touch(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, "x");
    }
}
