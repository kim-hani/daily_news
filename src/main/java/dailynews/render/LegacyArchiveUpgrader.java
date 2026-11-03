package dailynews.render;

import dailynews.model.Newsletter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 옛 디자인으로 남은 날짜별 페이지를 그날의 Markdown으로 다시 만들어 새 디자인으로 바꾼다(plan §3.3).
 * <ul>
 *   <li>옛 페이지는 캘린더용 {@code meta[name=dn:selected]}가 없는 페이지다. 새 페이지는 건드리지 않는다.</li>
 *   <li>Markdown이 없거나 읽지 못하면 옛 페이지를 그대로 두고 경고만 남긴다(옛 자원과 옛 마크업 읽기가 계속 받쳐 준다).</li>
 * </ul>
 * 이미 바꾼 페이지는 다시 바꾸지 않으므로 매일 실행해도 결과가 같다(FR-7.4).
 */
public final class LegacyArchiveUpgrader {

    private static final Logger log = LoggerFactory.getLogger(LegacyArchiveUpgrader.class);

    private final FeedRenderer feedRenderer;
    private final MarkdownNewsletterReader reader = new MarkdownNewsletterReader();

    public LegacyArchiveUpgrader(FeedRenderer feedRenderer) {
        this.feedRenderer = feedRenderer;
    }

    /** @return 새 디자인으로 바꾼 날짜 */
    public List<LocalDate> upgrade(OutputPaths paths) throws IOException {
        if (!Files.isDirectory(paths.archiveDir())) {
            return List.of();
        }
        List<Path> pages;
        try (Stream<Path> files = Files.list(paths.archiveDir())) {
            pages = files.filter(file -> file.getFileName().toString().endsWith(".html")).sorted().toList();
        }
        List<LocalDate> upgraded = new ArrayList<>();
        for (Path page : pages) {
            LocalDate date = dateOf(page);
            if (date != null && isLegacy(page) && upgrade(paths, date)) {
                upgraded.add(date);
            }
        }
        if (!upgraded.isEmpty()) {
            log.info("옛 디자인 날짜별 페이지 {}개를 새 디자인으로 바꿨습니다: {}", upgraded.size(), upgraded);
        }
        return upgraded;
    }

    private boolean upgrade(OutputPaths paths, LocalDate date) throws IOException {
        Path markdown = paths.markdown(date);
        if (!Files.exists(markdown)) {
            log.warn("{}의 Markdown이 없어 옛 디자인 페이지를 그대로 둡니다", date);
            return false;
        }
        Newsletter newsletter;
        try {
            newsletter = reader.read(Files.readString(markdown, StandardCharsets.UTF_8), date);
        } catch (RuntimeException e) {
            log.warn("{}의 Markdown을 읽지 못해 옛 디자인 페이지를 그대로 둡니다 ({})", date, e.toString());
            return false;
        }
        feedRenderer.writeArchive(newsletter, paths);
        return true;
    }

    private static boolean isLegacy(Path page) throws IOException {
        return Jsoup.parse(Files.readString(page, StandardCharsets.UTF_8)).selectFirst("meta[name=dn:selected]") == null;
    }

    private static LocalDate dateOf(Path page) {
        String name = page.getFileName().toString();
        try {
            return LocalDate.parse(name.substring(0, name.length() - ".html".length()));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
