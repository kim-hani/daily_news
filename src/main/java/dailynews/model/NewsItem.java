package dailynews.model;

/**
 * 뉴스레터에 실리는 기사 1건(사안 + 요약).
 */
public record NewsItem(Story story, Summary summary) {
}
