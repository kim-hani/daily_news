package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.render.CalendarModel.CalendarDay;
import dailynews.render.CalendarModel.CalendarMonth;
import dailynews.render.CalendarModel.CalendarPage;
import dailynews.render.Chrome.Nav;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalendarModelTest {

    private static final Chrome CHROME = new Chrome("뉴스 캘린더", "", "", Nav.CALENDAR, "", "https://github.com/x/y");

    @Test
    void 두_달에_걸친_보관은_오래된_달부터_이어지고_경계_달의_이전_다음은_null이다() {
        List<DayDigest> digests = new ArrayList<>();
        for (LocalDate date = LocalDate.of(2026, 10, 15); !date.isAfter(LocalDate.of(2026, 11, 2)); date = date.plusDays(1)) {
            digests.add(new DayDigest(date, 25, List.of("AI", "트럼프", "이란", "환율")));
        }

        CalendarPage page = build(digests);

        assertThat(page.empty()).isFalse();
        assertThat(page.months()).extracting(CalendarMonth::id).containsExactly("2026-10", "2026-11");
        assertThat(page.months()).extracting(CalendarMonth::label).containsExactly("2026년 10월", "2026년 11월");
        assertThat(page.latestMonthId()).isEqualTo("2026-11");
        CalendarMonth october = page.months().get(0);
        CalendarMonth november = page.months().get(1);
        assertThat(october.prevId()).isNull();
        assertThat(october.nextId()).isEqualTo("2026-11");
        assertThat(november.prevId()).isEqualTo("2026-10");
        assertThat(november.nextId()).isNull();
        assertThat(activeDays(october)).hasSize(17);
        assertThat(activeDays(november)).extracting(CalendarDay::isoDate).containsExactly("2026-11-01", "2026-11-02");
    }

    @Test
    void 사이에_보관이_없는_달도_빠지지_않는다() {
        CalendarPage page = build(List.of(digest(LocalDate.of(2026, 9, 30)), digest(LocalDate.of(2026, 11, 1))));

        assertThat(page.months()).extracting(CalendarMonth::id).containsExactly("2026-09", "2026-10", "2026-11");
        assertThat(activeDays(page.months().get(1))).isEmpty();
    }

    @Test
    void 한_달만_있으면_이전_다음_달이_모두_null이다() {
        CalendarPage page = build(List.of(digest(LocalDate.of(2026, 10, 28)), digest(LocalDate.of(2026, 10, 27))));

        assertThat(page.months()).hasSize(1);
        assertThat(page.months().get(0).prevId()).isNull();
        assertThat(page.months().get(0).nextId()).isNull();
    }

    @Test
    void 보관이_없으면_empty다_FR_4_9() {
        CalendarPage page = build(List.of());

        assertThat(page.empty()).isTrue();
        assertThat(page.months()).isEmpty();
        assertThat(page.latestMonthId()).isNull();
    }

    @Test
    void 주는_일요일부터_7칸이고_1일이_토요일이면_앞_빈_칸이_6개다() {
        // 2026-08-01은 토요일
        CalendarMonth august = build(List.of(digest(LocalDate.of(2026, 8, 1)))).months().get(0);

        assertThat(august.weeks()).allSatisfy(week -> assertThat(week).hasSize(7));
        List<CalendarDay> first = august.weeks().get(0);
        assertThat(first.subList(0, 6)).allSatisfy(day -> assertThat(day.dayOfMonth()).isZero());
        assertThat(first.get(6).dayOfMonth()).isEqualTo(1);
        // 31일(월)까지 6주
        assertThat(august.weeks()).hasSize(6);
        assertThat(august.weeks().get(5).get(1).dayOfMonth()).isEqualTo(31);
        assertThat(august.weeks().get(5).subList(2, 7)).allSatisfy(day -> assertThat(day.dayOfMonth()).isZero());
    }

    @Test
    void 보관된_날만_링크가_있고_최신_날이_표시된다_FR_4_2_FR_4_3() {
        CalendarMonth october = build(List.of(digest(LocalDate.of(2026, 10, 30)), digest(LocalDate.of(2026, 10, 31))))
                .months().get(0);

        CalendarDay thirtieth = day(october, 30);
        CalendarDay last = day(october, 31);
        assertThat(thirtieth.href()).isEqualTo("archive/2026-10-30.html");
        assertThat(thirtieth.latest()).isFalse();
        assertThat(last.latest()).isTrue();
        assertThat(day(october, 29).href()).isNull();
    }

    @Test
    void 칸에는_대표_키워드와_상위_3개_미리보기가_있고_aria_label이_읽을_문장이다() {
        CalendarMonth october = build(List.of(
                new DayDigest(LocalDate.of(2026, 10, 31), 25, List.of("AI", "트럼프", "이란", "환율")),
                new DayDigest(LocalDate.of(2026, 10, 30), 0, List.of()))).months().get(0);

        CalendarDay last = day(october, 31);
        assertThat(last.topKeyword()).isEqualTo("AI");
        assertThat(last.previewKeywords()).containsExactly("AI", "트럼프", "이란");
        assertThat(last.ariaLabel()).isEqualTo("2026년 10월 31일 토요일, 최신 뉴스레터, 선정 25건, 키워드 AI·트럼프·이란");
        // 읽지 못한 페이지도 링크는 유지하고 문장에는 있는 정보만 넣는다
        assertThat(day(october, 30).ariaLabel()).isEqualTo("2026년 10월 30일 금요일, 뉴스레터 있음");
        assertThat(day(october, 30).topKeyword()).isNull();
        assertThat(day(october, 29).ariaLabel()).isEqualTo("2026년 10월 29일 목요일, 뉴스레터 없음");
    }

    private static CalendarPage build(List<DayDigest> digests) {
        return CalendarModel.build(CHROME, 30, "https://github.com/x/y/tree/main/news", digests);
    }

    private static DayDigest digest(LocalDate date) {
        return new DayDigest(date, 5, List.of("AI"));
    }

    private static List<CalendarDay> activeDays(CalendarMonth month) {
        return month.weeks().stream().flatMap(List::stream).filter(day -> day.href() != null).toList();
    }

    private static CalendarDay day(CalendarMonth month, int dayOfMonth) {
        return month.weeks().stream().flatMap(List::stream).filter(day -> day.dayOfMonth() == dayOfMonth)
                .findFirst().orElseThrow();
    }
}
