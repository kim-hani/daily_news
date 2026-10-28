package dailynews.model;

/**
 * 기사 요약. 원문(또는 RSS 설명문)에 있는 문장만 담는다(FR-5.5).
 */
public record Summary(String text, SummaryOrigin origin) {
}
