package dailynews.collect;

import dailynews.model.Article;
import java.util.List;

/**
 * 피드 수집 결과.
 *
 * @param articles    수집한 기사(언론사 순서 × 카테고리 순서)
 * @param failedFeeds 실패한 피드(예: "yonhap/POLITICS")
 */
public record CollectResult(List<Article> articles, List<String> failedFeeds) {

    public CollectResult {
        articles = List.copyOf(articles);
        failedFeeds = List.copyOf(failedFeeds);
    }
}
