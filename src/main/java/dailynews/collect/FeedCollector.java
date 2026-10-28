package dailynews.collect;

import dailynews.http.FetchResult;
import dailynews.http.HttpFetcher;
import dailynews.http.RobotsPolicy;
import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.Source;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 설정된 피드를 순서대로 하나씩 수집한다. 실패한 피드는 건너뛰고 기록한다(FR-2.5, NFR-2).
 * 실제 실행에서는 {@link dailynews.http.HostThrottle}로 감싼 fetcher를 넘긴다.
 */
public final class FeedCollector {

    private static final Logger log = LoggerFactory.getLogger(FeedCollector.class);

    private final HttpFetcher fetcher;
    private final RobotsPolicy robots;
    private final RssParser parser;

    public FeedCollector(HttpFetcher fetcher, RobotsPolicy robots, RssParser parser) {
        this.fetcher = fetcher;
        this.robots = robots;
        this.parser = parser;
    }

    public CollectResult collect(List<Source> sources) {
        List<Article> articles = new ArrayList<>();
        List<String> failedFeeds = new ArrayList<>();
        int feedCount = 0;
        for (Source source : sources) {
            for (Map.Entry<Category, URI> feed : source.feeds().entrySet()) {
                feedCount++;
                String name = source.id() + "/" + feed.getKey();
                try {
                    List<Article> fetched = fetch(source, feed.getKey(), feed.getValue());
                    log.info("{}: {}건", name, fetched.size());
                    articles.addAll(fetched);
                } catch (FeedFailure e) {
                    log.warn("{} 수집 실패, 건너뜁니다: {}", name, e.getMessage());
                    failedFeeds.add(name);
                }
            }
        }
        log.info("피드 수집 완료: {}개 중 {}개 성공, 기사 {}건", feedCount, feedCount - failedFeeds.size(), articles.size());
        return new CollectResult(articles, failedFeeds);
    }

    private List<Article> fetch(Source source, Category category, URI uri) throws FeedFailure {
        if (!robots.isAllowed(uri)) {
            throw new FeedFailure("robots.txt가 막고 있습니다: " + uri);
        }
        FetchResult result;
        try {
            result = fetcher.fetch(uri);
        } catch (IOException e) {
            throw new FeedFailure("요청 실패(" + e + "): " + uri);
        }
        if (!result.isSuccess()) {
            throw new FeedFailure("응답 " + result.status() + ": " + uri);
        }
        try {
            return parser.parse(result.body(), source, category);
        } catch (RssParseException e) {
            throw new FeedFailure(e.getMessage());
        }
    }

    /** 피드 하나의 실패 사유 */
    private static final class FeedFailure extends Exception {

        FeedFailure(String message) {
            super(message);
        }
    }
}
