package dailynews.render;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * {@code site/archive/}에 남아 있는 날짜별 페이지로 목록 페이지 {@code site/archive.html}을 만든다(FR-7.2, US4).
 * 최신순으로 정렬하고 연·월별로 묶는다. 보관 정리({@link RetentionCleaner}) 뒤에 실행하므로 남은 파일만 나온다.
 */
public final class ArchiveRenderer {

    private static final Pattern HTML_NAME = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})\\.html$");

    private final String repositoryUrl;
    private final int keepDays;

    public ArchiveRenderer(String repositoryUrl, int keepDays) {
        this.repositoryUrl = repositoryUrl;
        this.keepDays = keepDays;
    }

    /** 남아 있는 날짜별 페이지의 날짜(최신순) */
    public List<LocalDate> dates(OutputPaths paths) throws IOException {
        if (!Files.isDirectory(paths.archiveDir())) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(paths.archiveDir())) {
            return files.filter(Files::isRegularFile)
                    .map(file -> RetentionCleaner.dateOf(file, HTML_NAME))
                    .flatMap(Optional::stream)
                    .sorted(Comparator.reverseOrder())
                    .toList();
        }
    }

    public String render(List<LocalDate> dates, ZonedDateTime generatedAt) {
        Map<YearMonth, List<Day>> byMonth = new LinkedHashMap<>();
        for (LocalDate date : dates) {
            String label = date.getMonthValue() + "월 " + date.getDayOfMonth() + "일 ("
                    + date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN) + ")";
            byMonth.computeIfAbsent(YearMonth.from(date), month -> new ArrayList<>())
                    .add(new Day("archive/" + date + ".html", label));
        }
        List<Month> months = byMonth.entrySet().stream()
                .map(entry -> new Month(entry.getKey().getYear() + "년 " + entry.getKey().getMonthValue() + "월",
                        entry.getValue()))
                .toList();
        Archive archive = new Archive(months, keepDays, repositoryUrl + "/tree/main/news",
                Formats.generatedAt(generatedAt), repositoryUrl);
        return Templates.process("archive", Map.of("archive", archive));
    }

    public void write(OutputPaths paths, ZonedDateTime generatedAt) throws IOException {
        Formats.write(paths.archiveIndex(), render(dates(paths), generatedAt));
    }

    public record Archive(List<Month> months, int keepDays, String markdownArchiveUrl, String generatedAt,
                          String repositoryUrl) {
    }

    public record Month(String label, List<Day> days) {
    }

    public record Day(String href, String label) {
    }
}
