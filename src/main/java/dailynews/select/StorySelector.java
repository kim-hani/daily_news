package dailynews.select;

import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.Story;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 카테고리별 상위 사안을 고른다(FR-4.3, FR-4.4).
 * 카테고리는 정치 → 사회 → 경제 → 글로벌 → IT 순서로 고르고, 앞 카테고리에서 고른 사안의 기사 URL은
 * 뒤 카테고리 후보에서 뺀다(예: 동아 경제·과학 피드에 같은 기사가 있으면 경제에만 남는다).
 * 후보가 {@code perCategory}보다 적으면 있는 만큼만 고른다.
 */
public final class StorySelector {

    static final List<Category> SELECTION_ORDER =
            List.of(Category.POLITICS, Category.SOCIETY, Category.ECONOMY, Category.GLOBAL, Category.IT);

    private static final Logger log = LoggerFactory.getLogger(StorySelector.class);

    private final StoryRanker ranker;
    private final int perCategory;

    public StorySelector(StoryRanker ranker, int perCategory) {
        this.ranker = ranker;
        this.perCategory = perCategory;
    }

    /** 카테고리별 선정 사안. 5개 카테고리가 모두 들어 있고(없으면 빈 목록) Category 순서를 따른다. */
    public Map<Category, List<Story>> select(Map<Category, List<ArticleCluster>> clusters, ZonedDateTime now) {
        Set<URI> used = new HashSet<>();
        Map<Category, List<Story>> selected = new EnumMap<>(Category.class);
        for (Category category : SELECTION_ORDER) {
            List<ArticleCluster> candidates = withoutUsed(clusters.getOrDefault(category, List.of()), used);
            List<StoryRanker.Ranked> top = ranker.rank(candidates, now).stream().limit(perCategory).toList();
            top.forEach(ranked -> ranked.cluster().articles().forEach(article -> used.add(article.url())));
            selected.put(category, top.stream().map(StoryRanker.Ranked::story).toList());
            log.info("{}: 후보 사안 {}개 중 {}개 선정", category.label(), candidates.size(), top.size());
        }
        return selected;
    }

    private static List<ArticleCluster> withoutUsed(List<ArticleCluster> clusters, Set<URI> used) {
        List<ArticleCluster> result = new ArrayList<>();
        for (ArticleCluster cluster : clusters) {
            List<Article> remaining = cluster.articles().stream()
                    .filter(article -> !used.contains(article.url()))
                    .toList();
            if (!remaining.isEmpty()) {
                result.add(new ArticleCluster(cluster.category(), remaining));
            }
        }
        return result;
    }
}
