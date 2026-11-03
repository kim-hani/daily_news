package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/**
 * 디자인 토큰(src/main/frontend/app.css)의 글자·배경 조합이 White·Dark 모두 4.5:1 이상인지 계산한다(T-004, NFR-3, SC9).
 */
class ThemeTokensTest {

    private static final Pattern TOKEN = Pattern.compile("--([a-z-]+):\\s*(#[0-9a-fA-F]{6});");

    private static final Map<String, String> WHITE = new HashMap<>();
    private static final Map<String, String> DARK = new HashMap<>();

    static {
        try {
            String css = Files.readString(Path.of("src/main/frontend/app.css"), StandardCharsets.UTF_8);
            int dark = css.indexOf("@variant dark {");
            read(css.substring(css.indexOf(":root {"), dark), WHITE);
            read(css.substring(dark, css.indexOf('}', dark)), DARK);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @ParameterizedTest(name = "{0} / {1}")
    @CsvSource({
            "foreground, background",
            "foreground, card",
            "muted-foreground, background",
            "muted-foreground, card",
            "muted-foreground, muted",
            "foreground, muted",
            "primary-foreground, primary",
            "accent, background",
            "accent, card",
            "accent-foreground, accent",
            "calendar-latest-foreground, calendar-latest-to",
            "calendar-latest-foreground, calendar-latest-from",
    })
    void 텍스트로_쓰는_토큰_조합은_대비가_4_5_이상이다(String text, String background) {
        assertThat(contrast(WHITE.get(text), WHITE.get(background))).as("White").isGreaterThanOrEqualTo(4.5);
        assertThat(contrast(DARK.get(text), DARK.get(background))).as("Dark").isGreaterThanOrEqualTo(4.5);
    }

    @Test
    void 생성된_app_css에_클래스_방식_다크와_JS_없는_다크_기기_규칙이_있다_FR_7_7() throws IOException {
        String css;
        try (InputStream in = getClass().getResourceAsStream("/static/app.css")) {
            css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(css).contains(":root:where(.dark,.dark *)", "@media (prefers-color-scheme:dark)",
                ":root:where(.no-js,.no-js *)", "--background:#09090b", "word-break:keep-all");
    }

    /** WCAG 2.x 대비 비율 */
    static double contrast(String a, String b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(String hex) {
        double[] rgb = new double[3];
        for (int i = 0; i < 3; i++) {
            double c = Integer.parseInt(hex.substring(1 + i * 2, 3 + i * 2), 16) / 255.0;
            rgb[i] = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
    }

    private static void read(String block, Map<String, String> into) {
        Matcher matcher = TOKEN.matcher(block);
        while (matcher.find()) {
            into.put(matcher.group(1), matcher.group(2));
        }
    }
}
