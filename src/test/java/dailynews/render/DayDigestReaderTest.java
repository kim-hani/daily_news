package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DayDigestReaderTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 31);

    private final DayDigestReader reader = new DayDigestReader();

    @TempDir
    Path dir;

    @Test
    void 새_페이지는_dn_메타_태그에서_읽는다() throws IOException {
        Path page = dir.resolve("new.html");
        Files.writeString(page, new FeedRenderer("https://github.com/x/y").render(TestNewsletters.sample(), true));

        DayDigest digest = reader.read(page, TestNewsletters.DATE);

        assertThat(digest).isEqualTo(new DayDigest(TestNewsletters.DATE, 5, java.util.List.of("SCM", "반도체", "국회")));
    }

    @Test
    void 옛_디자인_페이지는_키워드_칩과_카드_수에서_읽는다() throws IOException {
        Path page = dir.resolve("old.html");
        try (InputStream old = getClass().getResourceAsStream("/fixtures/archive/old-design.html")) {
            Files.copy(old, page);
        }

        DayDigest digest = reader.read(page, DATE);

        assertThat(digest.selected()).isEqualTo(25);
        assertThat(digest.keywords()).hasSize(10).startsWith("AI", "트럼프");
    }

    @Test
    void 깨진_파일이나_없는_파일은_선정_0건_키워드_없음이다() throws IOException {
        Path broken = dir.resolve("broken.html");
        Files.write(broken, new byte[]{(byte) 0xC3, (byte) 0x28, (byte) 0xFF});
        Path badNumber = dir.resolve("bad-number.html");
        Files.writeString(badNumber, "<meta name=\"dn:selected\" content=\"many\">");

        assertThat(reader.read(broken, DATE)).isEqualTo(new DayDigest(DATE, 0, java.util.List.of()));
        assertThat(reader.read(badNumber, DATE)).isEqualTo(new DayDigest(DATE, 0, java.util.List.of()));
        assertThat(reader.read(dir.resolve("missing.html"), DATE)).isEqualTo(new DayDigest(DATE, 0, java.util.List.of()));
    }
}
