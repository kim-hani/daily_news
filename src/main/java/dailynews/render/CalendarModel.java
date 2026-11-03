package dailynews.render;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 보관된 날짜 목록으로 월 달력 구조를 계산한다(spec FR-4.1~4.4, FR-4.7~4.9, plan §4.4).
 * 가장 오래된 달부터 최신 달까지 빈 달 없이 이어지고, 주는 일요일부터 7칸이다.
 */
public final class CalendarModel {

    /** 칸에 미리 보여 줄 키워드 수(FR-4.8) */
    static final int PREVIEW_KEYWORDS = 3;

    private CalendarModel() {
    }

    /**
     * @param digests 보관된 날짜별 페이지의 요약(순서 무관)
     */
    static CalendarPage build(Chrome chrome, int keepDays, String markdownArchiveUrl, List<DayDigest> digests) {
        if (digests.isEmpty()) {
            return new CalendarPage(chrome, keepDays, markdownArchiveUrl, List.of(), null, true);
        }
        Map<LocalDate, DayDigest> byDate = digests.stream()
                .collect(Collectors.toMap(DayDigest::date, Function.identity(), (a, b) -> a));
        LocalDate first = byDate.keySet().stream().min(Comparator.naturalOrder()).orElseThrow();
        LocalDate latest = byDate.keySet().stream().max(Comparator.naturalOrder()).orElseThrow();

        List<YearMonth> range = new ArrayList<>();
        for (YearMonth month = YearMonth.from(first); !month.isAfter(YearMonth.from(latest)); month = month.plusMonths(1)) {
            range.add(month);
        }
        List<CalendarMonth> months = new ArrayList<>();
        for (int i = 0; i < range.size(); i++) {
            YearMonth month = range.get(i);
            months.add(new CalendarMonth(id(month), label(month),
                    i == 0 ? null : id(range.get(i - 1)),
                    i == range.size() - 1 ? null : id(range.get(i + 1)),
                    weeks(month, byDate, latest)));
        }
        return new CalendarPage(chrome, keepDays, markdownArchiveUrl, months, id(YearMonth.from(latest)), false);
    }

    private static List<List<CalendarDay>> weeks(YearMonth month, Map<LocalDate, DayDigest> byDate, LocalDate latest) {
        List<CalendarDay> cells = new ArrayList<>();
        int leading = month.atDay(1).getDayOfWeek().getValue() % 7;      // 일요일 = 0
        for (int i = 0; i < leading; i++) {
            cells.add(CalendarDay.BLANK);
        }
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            cells.add(day(date, byDate.get(date), date.equals(latest)));
        }
        while (cells.size() % 7 != 0) {
            cells.add(CalendarDay.BLANK);
        }
        List<List<CalendarDay>> weeks = new ArrayList<>();
        for (int i = 0; i < cells.size(); i += 7) {
            weeks.add(List.copyOf(cells.subList(i, i + 7)));
        }
        return weeks;
    }

    private static CalendarDay day(LocalDate date, DayDigest digest, boolean latest) {
        String spoken = date.getYear() + "년 " + date.getMonthValue() + "월 " + date.getDayOfMonth() + "일 "
                + date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
        if (digest == null) {
            return new CalendarDay(date.getDayOfMonth(), date.toString(), null, false, 0, null, List.of(),
                    spoken + ", 뉴스레터 없음");
        }
        List<String> preview = digest.keywords().stream().limit(PREVIEW_KEYWORDS).toList();
        StringBuilder label = new StringBuilder(spoken).append(latest ? ", 최신 뉴스레터" : ", 뉴스레터 있음");
        if (digest.selected() > 0) {
            label.append(", 선정 ").append(digest.selected()).append("건");
        }
        if (!preview.isEmpty()) {
            label.append(", 키워드 ").append(String.join("·", preview));
        }
        return new CalendarDay(date.getDayOfMonth(), date.toString(), "archive/" + date + ".html", latest,
                digest.selected(), preview.isEmpty() ? null : preview.get(0), preview, label.toString());
    }

    private static String id(YearMonth month) {
        return month.toString();
    }

    private static String label(YearMonth month) {
        return month.getYear() + "년 " + month.getMonthValue() + "월";
    }

    /**
     * 캘린더 페이지 값.
     *
     * @param months        오래된 달 → 최신 달, 빈 달 없이 이어진다
     * @param latestMonthId 처음 보여 줄 달(최신 뉴스레터가 있는 달). 보관이 없으면 null
     * @param empty         보관된 날짜가 하나도 없음(FR-4.9)
     */
    public record CalendarPage(Chrome chrome, int keepDays, String markdownArchiveUrl, List<CalendarMonth> months,
                        String latestMonthId, boolean empty) {

        /** 표 머리의 요일(일요일 시작). 이름은 "일요일", 칸에는 "일"을 쓴다. */
        public List<String> weekdays() {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                names.add(DayOfWeek.SUNDAY.plus(i).getDisplayName(TextStyle.FULL, Locale.KOREAN));
            }
            return names;
        }
    }

    /**
     * @param id     "2026-10"(data-calendar-month, 앵커)
     * @param prevId 이전 달. 보관 범위 밖이면 null(FR-4.4)
     * @param nextId 다음 달. 보관 범위 밖이면 null
     * @param weeks  일요일 시작 7칸씩, 앞뒤 빈 칸 포함
     */
    public record CalendarMonth(String id, String label, String prevId, String nextId, List<List<CalendarDay>> weeks) {
    }

    /**
     * @param dayOfMonth      0이면 이번 달이 아닌 빈 칸
     * @param href            보관된 날만 "archive/2026-10-31.html", 아니면 null(FR-4.2)
     * @param latest          최신 뉴스레터 날(FR-4.3)
     * @param topKeyword      칸에 보여 줄 대표 키워드(FR-4.7). 없으면 null
     * @param previewKeywords 상위 3개(FR-4.8)
     * @param ariaLabel       "2026년 10월 31일 토요일, 뉴스레터 있음, 선정 25건, 키워드 AI·트럼프·이란"
     */
    public record CalendarDay(int dayOfMonth, String isoDate, String href, boolean latest, int selected, String topKeyword,
                       List<String> previewKeywords, String ariaLabel) {

        static final CalendarDay BLANK = new CalendarDay(0, null, null, false, 0, null, List.of(), null);
    }
}
