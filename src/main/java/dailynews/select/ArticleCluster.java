package dailynews.select;

import dailynews.model.Article;
import dailynews.model.Category;
import java.util.List;

/**
 * 같은 사안으로 묶인 기사들(점수를 매기기 전).
 */
public record ArticleCluster(Category category, List<Article> articles) {

    public ArticleCluster {
        if (articles.isEmpty()) {
            throw new IllegalArgumentException("기사가 없는 클러스터");
        }
        articles = List.copyOf(articles);
    }

    /** 보도한 서로 다른 언론사 수 */
    public int outletCount() {
        return (int) articles.stream().map(article -> article.source().id()).distinct().count();
    }
}
