package dailynews.clean;

import dailynews.model.Article;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 수집한 기사를 정제한다(FR-2.3, FR-3.1, FR-3.2).
 * <ol>
 *   <li>URL을 정규화하고, 허용되지 않는 링크의 기사는 버린다.</li>
 *   <li>실행 시각 기준 {@code window} 안(경계 포함)에 발행된 기사만 남기고 미래 시각 기사는 버린다.</li>
 *   <li>같은 카테고리 안에서 URL이 같거나, 같은 언론사의 정규화 제목이 같으면 가장 이른 기사 하나만 남긴다.</li>
 * </ol>
 * 카테고리가 다른 같은 기사는 여기서 합치지 않는다. 카테고리 간 중복은 선정 단계에서 정리한다.
 * 결과는 입력 순서를 유지한다.
 */
public final class ArticleCleaner {

    private static final Logger log = LoggerFactory.getLogger(ArticleCleaner.class);

    private final UrlNormalizer urlNormalizer;
    private final Clock clock;
    private final Duration window;

    public ArticleCleaner(UrlNormalizer urlNormalizer, Clock clock, Duration window) {
        this.urlNormalizer = urlNormalizer;
        this.clock = clock;
        this.window = window;
    }

    public List<Article> clean(List<Article> articles) {
        Instant now = clock.instant();
        Instant from = now.minus(window);
        List<Article> candidates = new ArrayList<>();
        int invalidUrl = 0;
        int outOfWindow = 0;
        for (Article article : articles) {
            Optional<URI> url = urlNormalizer.normalize(article.url(), article.source());
            if (url.isEmpty()) {
                invalidUrl++;
                continue;
            }
            Instant published = article.publishedAt().toInstant();
            if (published.isBefore(from) || published.isAfter(now)) {
                outOfWindow++;
                continue;
            }
            candidates.add(article.withUrl(url.get()));
        }

        List<Article> kept = deduplicate(candidates);
        log.info("정제: {}건 → {}건 (허용되지 않는 링크 {}, 기간 밖 {}, 중복 {})", articles.size(), kept.size(),
                invalidUrl, outOfWindow, candidates.size() - kept.size());
        return kept;
    }

    private static List<Article> deduplicate(List<Article> candidates) {
        List<Article> earliestFirst = new ArrayList<>(candidates);
        earliestFirst.sort(Comparator.comparing(Article::publishedAt));
        Set<String> seen = new HashSet<>();
        Set<Article> keep = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Article article : earliestFirst) {
            String urlKey = article.category() + "|" + article.url();
            String titleKey = article.category() + "|" + article.source().id() + "|"
                    + TitleNormalizer.normalize(article.title());
            if (seen.contains(urlKey) || seen.contains(titleKey)) {
                continue;
            }
            seen.add(urlKey);
            seen.add(titleKey);
            keep.add(article);
        }
        return candidates.stream().filter(keep::contains).toList();
    }
}
