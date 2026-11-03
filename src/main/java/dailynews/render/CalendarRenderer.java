package dailynews.render;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dailynews.render.CalendarModel.CalendarPage;
import dailynews.render.Chrome.Nav;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * {@code site/archive/}에 남아 있는 날짜별 페이지로 캘린더를 만든다(spec FR-4, plan §4.4·§4.5).
 * <ul>
 *   <li>{@code site/calendar.html}: 월 달력. 보관된 날만 그날 뉴스피드로 링크한다.</li>
 *   <li>{@code site/archive.html}: 옛 주소. 캘린더로 리다이렉트한다(FR-1.3).</li>
 *   <li>{@code site/data/archive.json}: 날짜별 페이지의 이전/다음 이동용 날짜 목록(최신순).</li>
 * </ul>
 * 보관 정리({@link RetentionCleaner}) 뒤에 실행하므로 남은 파일만 나온다(FR-6.3).
 */
public final class CalendarRenderer {

    private static final Pattern HTML_NAME = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})\\.html$");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String repositoryUrl;
    private final int keepDays;
    private final DayDigestReader reader = new DayDigestReader();

    public CalendarRenderer(String repositoryUrl, int keepDays) {
        this.repositoryUrl = repositoryUrl;
        this.keepDays = keepDays;
    }

    /** 남아 있는 날짜별 페이지의 날짜(최신순). 이름이 {@code YYYY-MM-DD.html}이 아닌 파일은 뺀다. */
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

    public void write(OutputPaths paths, ZonedDateTime generatedAt) throws IOException {
        List<LocalDate> dates = dates(paths);
        List<DayDigest> digests = dates.stream().map(date -> reader.read(paths.archivePage(date), date)).toList();
        Formats.write(paths.calendarPage(), render(page(digests, generatedAt)));
        Formats.write(paths.archiveIndex(), renderRedirect());
        Formats.write(paths.archiveJson(), archiveJson(dates, generatedAt));
    }

    CalendarPage page(List<DayDigest> digests, ZonedDateTime generatedAt) {
        Chrome chrome = new Chrome("뉴스 캘린더 · Daily News",
                "최근 " + keepDays + "일 동안의 뉴스피드를 달력에서 날짜별로 골라 볼 수 있습니다.", "", Nav.CALENDAR,
                Formats.generatedAt(generatedAt), repositoryUrl);
        return CalendarModel.build(chrome, keepDays, repositoryUrl + "/tree/main/news", digests);
    }

    String render(CalendarPage page) {
        return Templates.process("calendar", Map.of("calendar", page));
    }

    /** 옛 날짜 목록 주소를 캘린더로 보낸다(meta refresh + 누를 수 있는 링크) */
    String renderRedirect() {
        return Templates.process("redirect", Map.of("target", "calendar.html"));
    }

    /** {@code {"generatedAt":…, "latest":…, "dates":[최신순]}} */
    static String archiveJson(List<LocalDate> datesNewestFirst, ZonedDateTime generatedAt) throws JsonProcessingException {
        Map<String, Object> archive = new LinkedHashMap<>();
        archive.put("generatedAt", generatedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        archive.put("latest", datesNewestFirst.isEmpty() ? null : datesNewestFirst.get(0).toString());
        archive.put("dates", datesNewestFirst.stream().map(LocalDate::toString).toList());
        return JSON.writeValueAsString(archive) + "\n";
    }
}
