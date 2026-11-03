package dailynews.render;

/**
 * 모든 페이지가 함께 쓰는 머리(head)·내비게이션·푸터 값(plan §4.1).
 *
 * @param base    site 루트까지의 상대 경로. 루트 페이지는 "", archive/ 아래 페이지는 "../"
 * @param current 내비게이션에서 지금 페이지로 표시할 항목(FR-1.4)
 */
public record Chrome(String title, String description, String base, Nav current, String generatedAt,
                     String repositoryUrl) {

    /** 내비게이션 항목의 {@code aria-current} 값. 지금 페이지이면 "page", 아니면 null(속성을 쓰지 않는다) */
    public String ariaCurrent(String nav) {
        return current.name().equals(nav) ? "page" : null;
    }

    /** {@code aria-current}가 붙는 내비게이션 항목 */
    public enum Nav {
        HOME, TODAY, CALENDAR,
        /** 날짜별 뉴스피드처럼 내비게이션 항목이 아닌 페이지 */
        NONE
    }
}
