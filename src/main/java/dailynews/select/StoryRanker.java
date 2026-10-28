package dailynews.select;

import dailynews.model.Article;
import dailynews.model.Source;
import dailynews.model.Story;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 클러스터에 점수를 매기고 대표 기사와 같은 사안 링크를 정한다(FR-4.2, FR-4.3).
 * <ul>
 *   <li>점수 = 보도 언론사 수 × 10 + 최신성. 최신성은 가장 최근 기사 기준 {@code 1 − 경과시간/24h}(0~1)다.</li>
 *   <li>점수가 같으면 기사 수가 많은 쪽을 앞에 둔다.</li>
 *   <li>대표 기사는 가장 먼저 보도된 기사다. 시각이 같으면 설정의 언론사 순서를 따른다.
 *       단 {@code [속보]} 기사는 본문이 거의 없으므로 같은 사안에 다른 기사가 있으면 대표로 고르지 않는다.</li>
 *   <li>같은 사안 링크는 대표 기사의 언론사를 뺀 언론사마다 1건이다(고르는 기준은 대표와 같다).</li>
 * </ul>
 */
public final class StoryRanker {

    private static final Duration RECENCY_WINDOW = Duration.ofHours(24);

    private final Map<String, Integer> sourceOrder = new HashMap<>();

    public StoryRanker(List<Source> sources) {
        for (int i = 0; i < sources.size(); i++) {
            sourceOrder.put(sources.get(i).id(), i);
        }
    }

    /** 점수가 높은 순으로 정렬한 사안 */
    public List<Ranked> rank(List<ArticleCluster> clusters, ZonedDateTime now) {
        return clusters.stream()
                .map(cluster -> new Ranked(toStory(cluster, now), cluster))
                .sorted(Comparator.comparingDouble((Ranked ranked) -> ranked.story().score()).reversed()
                        .thenComparing(ranked -> ranked.cluster().articles().size(), Comparator.reverseOrder())
                        .thenComparing(ranked -> ranked.story().representative().publishedAt(), Comparator.reverseOrder())
                        .thenComparing(ranked -> ranked.story().representative().url().toString()))
                .toList();
    }

    public Story toStory(ArticleCluster cluster, ZonedDateTime now) {
        Comparator<Article> earliest = Comparator.comparing(StoryRanker::isBreakingFlash)
                .thenComparing(Article::publishedAt)
                .thenComparing(article -> order(article.source()))
                .thenComparing(article -> article.url().toString());
        Article representative = cluster.articles().stream().min(earliest).orElseThrow();

        List<Article> related = new ArrayList<>();
        cluster.articles().stream()
                .map(Article::source)
                .distinct()
                .filter(source -> !source.id().equals(representative.source().id()))
                .sorted(Comparator.comparing(this::order))
                .forEach(source -> earliestOf(cluster, source, earliest).ifPresent(related::add));

        int outletCount = cluster.outletCount();
        return new Story(cluster.category(), representative, related, outletCount,
                outletCount * 10 + recency(cluster, now));
    }

    private static Optional<Article> earliestOf(ArticleCluster cluster, Source source, Comparator<Article> earliest) {
        return cluster.articles().stream()
                .filter(article -> article.source().id().equals(source.id()))
                .min(earliest);
    }

    static double recency(ArticleCluster cluster, ZonedDateTime now) {
        ZonedDateTime latest = cluster.articles().stream().map(Article::publishedAt)
                .max(Comparator.naturalOrder()).orElseThrow();
        double elapsed = (double) Duration.between(latest, now).toSeconds() / RECENCY_WINDOW.toSeconds();
        return Math.clamp(1 - elapsed, 0.0, 1.0);
    }

    /** 본문 없이 한 줄로 나가는 속보(실제 연합뉴스 속보는 본문 문단과 RSS 설명문이 비어 있다) */
    private static boolean isBreakingFlash(Article article) {
        return article.title().strip().startsWith("[속보]");
    }

    private int order(Source source) {
        return sourceOrder.getOrDefault(source.id(), Integer.MAX_VALUE);
    }

    /** 점수를 매긴 사안과 원래 클러스터(카테고리 간 중복 제거에 쓴다) */
    public record Ranked(Story story, ArticleCluster cluster) {
    }
}
