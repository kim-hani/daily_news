package dailynews.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * classpath {@code static/}의 정적 자원을 {@code site/assets/}에 복사한다(FR-6.4).
 * 새 디자인 자원과 함께, 전환 기간에 남은 옛 날짜별 페이지가 쓰는 {@code style.css}·{@code tabs.js}도 복사한다(plan §3.3).
 */
final class SiteAssets {

    /** 새 자원(app.css는 buildCss가 만든다)과 옛 자원 */
    static final List<String> FILES = List.of("app.css", "app.js", "theme-init.js", "style.css", "tabs.js");
    static final String FONT_CSS = "fonts/pretendard.css";
    static final String FONT_LICENSE = "fonts/LICENSE";

    private static final Pattern FONT_URL = Pattern.compile("url\\(\\./([\\w.-]+\\.woff2)\\)");

    private SiteAssets() {
    }

    static void copy(OutputPaths paths) throws IOException {
        Files.createDirectories(paths.fontsDir());
        for (String file : FILES) {
            copyResource(file, paths.assetsDir().resolve(file));
        }
        copyResource(FONT_CSS, paths.assetsDir().resolve(FONT_CSS));
        copyResource(FONT_LICENSE, paths.assetsDir().resolve(FONT_LICENSE));
        for (String font : fontFiles()) {
            copyResource("fonts/" + font, paths.fontsDir().resolve(font));
        }
    }

    /** {@code pretendard.css}가 참조하는 woff2 파일 이름(목록을 따로 관리하지 않도록 CSS에서 읽는다) */
    static List<String> fontFiles() throws IOException {
        Matcher matcher = FONT_URL.matcher(new String(read(FONT_CSS), StandardCharsets.UTF_8));
        List<String> files = new ArrayList<>();
        while (matcher.find()) {
            files.add(matcher.group(1));
        }
        return files;
    }

    private static byte[] read(String name) throws IOException {
        try (InputStream in = open(name)) {
            return in.readAllBytes();
        }
    }

    private static void copyResource(String name, Path target) throws IOException {
        try (InputStream in = open(name)) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static InputStream open(String name) throws IOException {
        InputStream in = SiteAssets.class.getClassLoader().getResourceAsStream("static/" + name);
        if (in == null) {
            throw new IOException("리소스가 없습니다: static/" + name);
        }
        return in;
    }
}
