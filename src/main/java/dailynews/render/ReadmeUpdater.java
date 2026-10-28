package dailynews.render;

import dailynews.model.Newsletter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * README.md의 {@code <!-- LATEST:START -->} ~ {@code <!-- LATEST:END -->} 구간만 최신 뉴스레터 안내로 바꾼다(FR-7.3).
 * 마커가 없으면 파일 끝에 붙이고, README가 없으면 새로 만든다. 같은 입력이면 결과가 같다(멱등).
 */
public final class ReadmeUpdater {

    static final String START = "<!-- LATEST:START -->";
    static final String END = "<!-- LATEST:END -->";

    private final String siteUrl;

    public ReadmeUpdater(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public void update(OutputPaths paths, Newsletter newsletter) throws IOException {
        Path readme = paths.readme();
        String current = Files.exists(readme) ? Files.readString(readme, StandardCharsets.UTF_8) : "# Daily News\n";
        String updated = apply(current, block(newsletter, paths.markdownRelative(newsletter.date())));
        if (!updated.equals(current)) {
            Formats.write(readme, updated);
        }
    }

    String block(Newsletter newsletter, String markdownPath) {
        StringBuilder block = new StringBuilder(START).append('\n')
                .append("### 📰 최신 뉴스레터: ").append(Formats.dateLabel(newsletter.date())).append("\n\n")
                .append("- 웹에서 보기: ").append(siteUrl).append('\n')
                .append("- Markdown: [").append(markdownPath).append("](").append(markdownPath).append(")\n")
                .append("- 선정 ").append(newsletter.stats().selected()).append("건");
        if (!newsletter.keywords().isEmpty()) {
            block.append(" · 🔑 ").append(String.join(" · ", newsletter.keywords()));
        }
        return block.append('\n').append(END).toString();
    }

    static String apply(String readme, String block) {
        int start = readme.indexOf(START);
        int end = readme.indexOf(END, Math.max(start, 0));
        if (start >= 0 && end > start) {
            return readme.substring(0, start) + block + readme.substring(end + END.length());
        }
        StringBuilder appended = new StringBuilder(readme);
        while (!appended.isEmpty() && !appended.toString().endsWith("\n\n")) {
            appended.append('\n');
        }
        return appended.append(block).append('\n').toString();
    }
}
