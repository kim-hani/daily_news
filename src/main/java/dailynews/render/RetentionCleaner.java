package dailynews.render;

import dailynews.config.AppConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 보관 기간이 지난 날짜별 파일을 지운다(FR-7.5, SC9).
 * <ul>
 *   <li>{@code site/archive/YYYY-MM-DD.html} 중 날짜가 {@code 기준일 − (siteArchiveDays − 1)}보다 이전인 파일을 지운다.
 *       30일이면 기준일을 포함해 30개가 남는다.</li>
 *   <li>{@code newsDays}가 0이면 Markdown({@code news/})은 지우지 않는다. 0보다 크면 같은 규칙을
 *       {@code news/YYYY/MM/YYYY-MM-DD.md}에 적용하고 비게 된 월·연 폴더도 지운다.</li>
 *   <li>이름 패턴이 다른 파일, 기준일보다 미래 날짜 파일, 하위 폴더는 건드리지 않는다.</li>
 * </ul>
 */
public final class RetentionCleaner {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleaner.class);
    private static final Pattern HTML_NAME = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})\\.html$");
    private static final Pattern MARKDOWN_NAME = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})\\.md$");

    private final AppConfig.Retention retention;

    public RetentionCleaner(AppConfig.Retention retention) {
        this.retention = retention;
    }

    /** @return 지운 파일 */
    public List<Path> clean(OutputPaths paths, LocalDate today) throws IOException {
        List<Path> deleted = new ArrayList<>(deleteOld(paths.archiveDir(), HTML_NAME, keepFrom(today, retention.siteArchiveDays())));
        if (retention.newsDays() > 0 && Files.isDirectory(paths.newsDir())) {
            LocalDate keepFrom = keepFrom(today, retention.newsDays());
            for (Path monthDir : monthDirs(paths.newsDir())) {
                deleted.addAll(deleteOld(monthDir, MARKDOWN_NAME, keepFrom));
                deleteIfEmpty(monthDir);
                deleteIfEmpty(monthDir.getParent());
            }
        }
        deleted.forEach(path -> log.info("보관 기간이 지나 삭제: {}", paths.root().relativize(path)));
        return deleted;
    }

    private static LocalDate keepFrom(LocalDate today, int days) {
        return today.minusDays(days - 1L);
    }

    private static List<Path> deleteOld(Path dir, Pattern namePattern, LocalDate keepFrom) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<Path> deleted = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Optional<LocalDate> date = dateOf(file, namePattern);
                if (date.isPresent() && date.get().isBefore(keepFrom)) {
                    Files.delete(file);
                    deleted.add(file);
                }
            }
        }
        return deleted;
    }

    /** 이름이 패턴에 맞고 실제 날짜이면 그 날짜 */
    static Optional<LocalDate> dateOf(Path file, Pattern namePattern) {
        Matcher matcher = namePattern.matcher(file.getFileName().toString());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(matcher.group(1)));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /** news/YYYY/MM 폴더들 */
    private static List<Path> monthDirs(Path newsDir) throws IOException {
        try (Stream<Path> dirs = Files.walk(newsDir, 2)) {
            return dirs.filter(Files::isDirectory)
                    .filter(dir -> newsDir.relativize(dir).getNameCount() == 2)
                    .toList();
        }
    }

    private static void deleteIfEmpty(Path dir) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            if (entries.findAny().isEmpty()) {
                Files.delete(dir);
            }
        }
    }
}
