package dailynews.summary;

import dailynews.http.FetchResult;
import dailynews.http.HttpFetcher;
import dailynews.http.RobotsPolicy;
import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.NewsItem;
import dailynews.model.Story;
import dailynews.model.Summary;
import dailynews.model.SummaryOrigin;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 선정된 사안마다 대표 기사의 원문을 받아 요약한다(FR-5, NFR-8).
 * 원문 가져오기를 끈 언론사({@link dailynews.model.Source#fetchBody()})는 원문을 요청하지 않는다.
 * 이 경우와 robots.txt가 막거나 요청이 실패한 경우에는 RSS 설명문으로 요약한다.
 * 원문 요청은 대표 기사당 한 번뿐이다(NFR-1). 원문은 메모리에서만 쓰고 저장하지 않는다(NFR-4).
 */
public final class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private final HttpFetcher fetcher;
    private final RobotsPolicy robots;
    private final BodyExtractor extractor;
    private final LeadSummarizer summarizer;

    public SummaryService(HttpFetcher fetcher, RobotsPolicy robots, BodyExtractor extractor, LeadSummarizer summarizer) {
        this.fetcher = fetcher;
        this.robots = robots;
        this.extractor = extractor;
        this.summarizer = summarizer;
    }

    public Result summarize(Map<Category, List<Story>> stories) {
        Map<Category, List<NewsItem>> sections = new EnumMap<>(Category.class);
        int bodyOk = 0;
        int bodyFallback = 0;
        for (Map.Entry<Category, List<Story>> entry : stories.entrySet()) {
            List<NewsItem> items = new ArrayList<>();
            for (Story story : entry.getValue()) {
                Summary summary = summarize(story.representative());
                if (summary.origin() == SummaryOrigin.BODY) {
                    bodyOk++;
                } else {
                    bodyFallback++;
                }
                items.add(new NewsItem(story, summary));
            }
            sections.put(entry.getKey(), items);
        }
        int total = bodyOk + bodyFallback;
        log.info("요약: {}건 중 본문 기반 {}건({}%), RSS 대체·요약 없음 {}건", total, bodyOk,
                total == 0 ? 0 : bodyOk * 100 / total, bodyFallback);
        return new Result(sections, bodyOk, bodyFallback);
    }

    private Summary summarize(Article article) {
        return summarizer.summarize(fetchBody(article), article.rssDescription(), article.source());
    }

    private List<String> fetchBody(Article article) {
        if (!article.source().fetchBody()) {
            return List.of();
        }
        if (!robots.isAllowed(article.url())) {
            log.info("robots.txt가 막고 있어 원문을 받지 않습니다: {}", article.url());
            return List.of();
        }
        try {
            FetchResult result = fetcher.fetch(article.url());
            if (!result.isSuccess()) {
                log.warn("원문 응답 {}: {}", result.status(), article.url());
                return List.of();
            }
            List<String> paragraphs = extractor.extract(result.body(), result.contentType(), result.finalUri(),
                    article.source());
            if (paragraphs.isEmpty()) {
                log.warn("본문을 찾지 못했습니다(selector {}): {}", article.source().bodySelector(), article.url());
            }
            return paragraphs;
        } catch (IOException e) {
            log.warn("원문 요청 실패({}): {}", e, article.url());
            return List.of();
        }
    }

    /**
     * @param sections     카테고리별 기사(입력과 같은 카테고리·순서)
     * @param bodyOk       본문 기반 요약 수
     * @param bodyFallback RSS 대체 또는 요약 없음 수
     */
    public record Result(Map<Category, List<NewsItem>> sections, int bodyOk, int bodyFallback) {

        public Result {
            EnumMap<Category, List<NewsItem>> copy = new EnumMap<>(Category.class);
            sections.forEach((category, items) -> copy.put(category, List.copyOf(items)));
            sections = Collections.unmodifiableMap(copy);
        }
    }
}
