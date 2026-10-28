package dailynews;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 명령줄 옵션(plan §5.1).
 *
 * @param date   출력 파일 날짜. null이면 실행일(KST)
 * @param root   출력 루트
 * @param config 설정 파일. null이면 classpath의 sources.yml
 * @param dryRun true면 파일을 쓰지 않고 결과만 로그로 남긴다
 */
public record CliOptions(LocalDate date, Path root, Path config, boolean dryRun) {

    public static final String USAGE =
            "사용법: ./gradlew run --args=\"[--date YYYY-MM-DD] [--root 경로] [--config sources.yml] [--dry-run]\"";

    /** @throws IllegalArgumentException 알 수 없는 옵션이거나 값이 없거나 잘못되었을 때 */
    public static CliOptions parse(String[] args) {
        LocalDate date = null;
        Path root = Path.of(".");
        Path config = null;
        boolean dryRun = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--date" -> date = parseDate(value(args, ++i, "--date"));
                case "--root" -> root = Path.of(value(args, ++i, "--root"));
                case "--config" -> config = Path.of(value(args, ++i, "--config"));
                case "--dry-run" -> dryRun = true;
                default -> throw new IllegalArgumentException("알 수 없는 옵션입니다: " + args[i]);
            }
        }
        return new CliOptions(date, root, config, dryRun);
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException(option + " 옵션에 값이 없습니다");
        }
        return args[index];
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("--date는 YYYY-MM-DD 형식이어야 합니다: " + text);
        }
    }
}
