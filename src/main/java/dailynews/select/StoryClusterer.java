package dailynews.select;

import dailynews.clean.TitleNormalizer;
import dailynews.model.Article;
import dailynews.model.Category;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 카테고리 안에서 같은 사안의 기사를 묶는다(FR-4.1).
 * 최신 기사부터 순회하며, 기존 클러스터 구성원 중 하나와라도 유사도가 임계값 이상이면 그 클러스터에 넣고
 * 아니면 새 클러스터를 만든다(single-link). 새 기사가 여러 클러스터와 이어지면 그 클러스터들을 합치므로
 * 결과는 유사도 그래프의 연결 요소와 같고 순회 순서에 좌우되지 않는다.
 */
public final class StoryClusterer {

    private final TitleSimilarity similarity;
    private final double threshold;

    public StoryClusterer(TitleSimilarity similarity, double threshold) {
        this.similarity = similarity;
        this.threshold = threshold;
    }

    /** 카테고리별 클러스터(Category 순서, 카테고리 안은 만들어진 순서) */
    public Map<Category, List<ArticleCluster>> cluster(List<Article> articles) {
        Map<Category, List<Article>> byCategory = new EnumMap<>(Category.class);
        for (Article article : articles) {
            byCategory.computeIfAbsent(article.category(), category -> new ArrayList<>()).add(article);
        }
        Map<Category, List<ArticleCluster>> result = new EnumMap<>(Category.class);
        byCategory.forEach((category, members) -> result.put(category, clusterCategory(category, members)));
        return result;
    }

    private List<ArticleCluster> clusterCategory(Category category, List<Article> articles) {
        List<Article> newestFirst = new ArrayList<>(articles);
        newestFirst.sort(Comparator.comparing(Article::publishedAt).reversed()
                .thenComparing(article -> article.url().toString()));

        List<Group> groups = new ArrayList<>();
        for (Article article : newestFirst) {
            TitleSimilarity.Features features = similarity.features(TitleNormalizer.normalize(article.title()));
            List<Group> matched = groups.stream()
                    .filter(group -> group.maxSimilarity(features) >= threshold)
                    .toList();
            Group target;
            if (matched.isEmpty()) {
                target = new Group();
                groups.add(target);
            } else {
                // 새 기사가 여러 클러스터와 이어지면 그 클러스터들은 같은 사안이므로 하나로 합친다.
                target = matched.getFirst();
                for (Group other : matched.subList(1, matched.size())) {
                    target.addAll(other);
                    groups.remove(other);
                }
            }
            target.add(article, features);
        }
        return groups.stream().map(group -> new ArticleCluster(category, group.articles)).toList();
    }

    private final class Group {
        private final List<Article> articles = new ArrayList<>();
        private final List<TitleSimilarity.Features> features = new ArrayList<>();

        void add(Article article, TitleSimilarity.Features feature) {
            articles.add(article);
            features.add(feature);
        }

        void addAll(Group other) {
            articles.addAll(other.articles);
            features.addAll(other.features);
        }

        double maxSimilarity(TitleSimilarity.Features other) {
            return features.stream().mapToDouble(feature -> similarity.similarity(feature, other)).max().orElse(0);
        }
    }
}
