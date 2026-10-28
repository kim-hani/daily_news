package dailynews.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * 렌더러가 함께 쓰는 날짜 표기와 파일 쓰기.
 */
final class Formats {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Formats() {
    }

    /** 2026-10-28 (수) */
    static String dateLabel(LocalDate date) {
        return date + " (" + date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN) + ")";
    }

    /** 08:12 */
    static String time(ZonedDateTime dateTime) {
        return dateTime.format(TIME);
    }

    /** 2026-10-28 09:00 KST */
    static String generatedAt(ZonedDateTime dateTime) {
        return dateTime.format(DATE_TIME) + " KST";
    }

    /** 폴더를 만들고 UTF-8로 덮어쓴다. */
    static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
