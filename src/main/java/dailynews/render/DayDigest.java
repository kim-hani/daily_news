package dailynews.render;

import java.time.LocalDate;
import java.util.List;

/**
 * 날짜별 뉴스피드 하나의 요약(plan §4.4). 뉴스피드 {@code <head>}의 {@code dn:*} 메타 태그로 쓰고,
 * 캘린더는 {@link DayDigestReader}로 보관된 페이지에서 다시 읽는다.
 *
 * @param selected 선정 기사 수
 * @param keywords 오늘의 키워드(중요한 것부터)
 */
public record DayDigest(LocalDate date, int selected, List<String> keywords) {

    public DayDigest {
        keywords = List.copyOf(keywords);
    }
}
