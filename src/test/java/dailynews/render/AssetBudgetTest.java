package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

/**
 * 첫 로드 자원의 gzip 크기 예산(NFR-6, plan §9.1)과 웹폰트 규칙(NFR-11).
 */
class AssetBudgetTest {

    private static final int KB = 1024;

    @Test
    void 페이지당_CSS와_JS는_gzip_150KB_이하이고_파일별_목표를_지킨다() throws IOException {
        int css = gzipSize("app.css");
        int js = gzipSize("app.js");
        int themeInit = gzipSize("theme-init.js");
        int fontCss = gzipSize("fonts/pretendard.css");

        assertThat(css).as("app.css gzip").isLessThanOrEqualTo(30 * KB);
        assertThat(js).as("app.js gzip").isLessThanOrEqualTo(6 * KB);
        assertThat(themeInit).as("theme-init.js gzip").isLessThanOrEqualTo(KB / 2);
        assertThat(css + js + themeInit + fontCss).as("합계 gzip").isLessThanOrEqualTo(150 * KB);
    }

    @Test
    void 폰트는_모두_swap이고_외부_URL을_쓰지_않으며_OFL_라이선스가_있다() throws IOException {
        String fontCss = new String(read("fonts/pretendard.css"), StandardCharsets.UTF_8);
        int faces = fontCss.split("@font-face", -1).length - 1;

        assertThat(faces).isEqualTo(92);
        assertThat(fontCss.split("font-display: swap", -1).length - 1).isEqualTo(faces);
        assertThat(fontCss).doesNotContainPattern("url\\(\\s*['\"]?(https?:)?//");
        assertThat(new String(read("fonts/LICENSE"), StandardCharsets.UTF_8)).contains("SIL Open Font License");
    }

    private static int gzipSize(String name) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(read(name));
        }
        return out.size();
    }

    private static byte[] read(String name) throws IOException {
        try (InputStream in = AssetBudgetTest.class.getResourceAsStream("/static/" + name)) {
            assertThat(in).as(name).isNotNull();
            return in.readAllBytes();
        }
    }
}
