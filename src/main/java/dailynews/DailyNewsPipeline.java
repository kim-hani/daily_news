package dailynews;

import dailynews.clean.ArticleCleaner;
import dailynews.clean.CategoryFilter;
import dailynews.clean.UrlNormalizer;
import dailynews.collect.CollectResult;
import dailynews.collect.FeedCollector;
import dailynews.collect.RssParser;
import dailynews.config.AppConfig;
import dailynews.http.HttpFetcher;
import dailynews.http.RobotsPolicy;
import dailynews.keyword.KeywordExtractor;
import dailynews.keyword.KomoranAnalyzer;
import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.NewsItem;
import dailynews.model.Newsletter;
import dailynews.model.RunStats;
import dailynews.model.Source;
import dailynews.model.Story;
import dailynews.render.CalendarRenderer;
import dailynews.render.FeedRenderer;
import dailynews.render.HomeRenderer;
import dailynews.render.LegacyArchiveUpgrader;
import dailynews.render.MarkdownNewsletterReader;
import dailynews.render.MarkdownRenderer;
import dailynews.render.OutputPaths;
import dailynews.render.PublishedHistory;
import dailynews.render.ReadmeUpdater;
import dailynews.render.RetentionCleaner;
import dailynews.select.StoryClusterer;
import dailynews.select.StoryRanker;
import dailynews.select.StorySelector;
import dailynews.select.TitleSimilarity;
import dailynews.summary.BodyExtractor;
import dailynews.summary.LeadSummarizer;
import dailynews.summary.SentenceSplitter;
import dailynews.summary.SummaryService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 뉴스레터 한 번 만들기(plan §3): 수집 → 지난 뉴스레터 URL 모으기 → 정제(지난 URL 빼기 포함, history-dedup plan §3.1) →
 * IT 필터 → 클러스터 → 선정 → 요약 → 키워드 → Markdown → 뉴스피드 → 보관 정리 → 옛 날짜별 페이지 갱신 → 캘린더 → 홈 → README(page-ui plan §3.1, §3.3).
 * 네트워크({@link HttpFetcher})와 시각({@link Clock})을 주입받으므로 테스트에서 fixture와 고정 시각으로 실행할 수 있다.
 */
public final class DailyNewsPipeline {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    public static final int EXIT_OK = 0;
    /** 수집한 기사가 0건이거나 설정·옵션 오류 */
    public static final int EXIT_FAILURE = 1;
    /** 파일 출력 실패 */
    public static final int EXIT_OUTPUT_ERROR = 2;

    private static final Logger log = LoggerFactory.getLogger(DailyNewsPipeline.class);
    private static final int KEYWORD_COUNT = 10;

    private final AppConfig config;
    private final Clock clock;
    private final FeedCollector collector;
    private final PublishedHistory publishedHistory;
    private final ArticleCleaner cleaner;
    private final CategoryFilter categoryFilter;
    private final StoryClusterer clusterer;
    private final StorySelector selector;
    private final SummaryService summaryService;
    private final KeywordExtractor keywordExtractor;
    private final MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    private final FeedRenderer feedRenderer;
    private final RetentionCleaner retentionCleaner;
    private final LegacyArchiveUpgrader legacyArchiveUpgrader;
    private final CalendarRenderer calendarRenderer;
    private final HomeRenderer homeRenderer;
    private final ReadmeUpdater readmeUpdater;

    public DailyNewsPipeline(AppConfig config, HttpFetcher fetcher, Clock clock) {
        this.config = config;
        this.clock = clock;
        KomoranAnalyzer analyzer = KomoranAnalyzer.getInstance();
        RobotsPolicy robots = new RobotsPolicy(fetcher);
        this.collector = new FeedCollector(fetcher, robots, new RssParser(KST));
        this.publishedHistory = new PublishedHistory(config.historyDays(), new MarkdownNewsletterReader());
        this.cleaner = new ArticleCleaner(new UrlNormalizer(), clock, Duration.ofHours(config.windowHours()));
        this.categoryFilter = new CategoryFilter(config.itKeywords());
        this.clusterer = new StoryClusterer(new TitleSimilarity(analyzer), config.clusterThreshold());
        this.selector = new StorySelector(new StoryRanker(config.sources()), config.perCategory());
        this.summaryService = new SummaryService(fetcher, robots, new BodyExtractor(),
                new LeadSummarizer(new SentenceSplitter()));
        this.keywordExtractor = new KeywordExtractor(analyzer::nouns, config.stopwords());
        this.feedRenderer = new FeedRenderer(config.repositoryUrl());
        this.retentionCleaner = new RetentionCleaner(config.retention());
        this.legacyArchiveUpgrader = new LegacyArchiveUpgrader(feedRenderer);
        this.calendarRenderer = new CalendarRenderer(config.repositoryUrl(), config.retention().siteArchiveDays());
        this.homeRenderer = new HomeRenderer(config.repositoryUrl());
        this.readmeUpdater = new ReadmeUpdater(config.siteUrl());
    }

    /**
     * @param date   출력 파일 날짜
     * @param root   출력 루트
     * @param dryRun true면 파일을 쓰지 않는다
     */
    public Result run(LocalDate date, Path root, boolean dryRun) {
        long start = System.nanoTime();
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(KST);
        OutputPaths paths = new OutputPaths(root);

        CollectResult collected = collector.collect(config.sources());
        if (collected.articles().isEmpty()) {
            log.error("수집한 기사가 없어 뉴스레터를 만들지 않습니다(실패 피드 {}개)", collected.failedFeeds().size());
            return new Result(EXIT_FAILURE, null);
        }
        // --dry-run에서도 읽기만 하므로 똑같이 뺀다. 당일 파일은 읽지 않아 같은 날 재실행 결과가 같다(FR-7.4).
        Set<URI> published = publishedHistory.urlsBefore(paths, date);
        List<Article> cleaned = cleaner.clean(collected.articles(), published);
        List<Article> candidates = categoryFilter.filter(cleaned);
        Map<Category, List<Story>> stories = selector.select(clusterer.cluster(candidates), now);
        SummaryService.Result summaries = summaryService.summarize(stories);
        List<String> keywords = keywordExtractor.extract(cleaned.stream().map(Article::title).toList(), KEYWORD_COUNT);

        int selected = stories.values().stream().mapToInt(List::size).sum();
        RunStats stats = new RunStats(collected.articles().size(), cleaned.size(), selected, collected.failedFeeds(),
                summaries.bodyOk(), summaries.bodyFallback());
        Newsletter newsletter = new Newsletter(date, now, config.sources().stream().map(Source::name).toList(),
                summaries.sections(), keywords, stats);
        logSummary(newsletter);

        if (dryRun) {
            log.info("--dry-run: 파일을 쓰지 않습니다");
        } else {
            try {
                write(newsletter, paths);
            } catch (IOException | UncheckedIOException e) {
                log.error("출력 실패", e);
                return new Result(EXIT_OUTPUT_ERROR, newsletter);
            }
        }
        log.info("완료: {}초", Duration.ofNanos(System.nanoTime() - start).toSeconds());
        return new Result(EXIT_OK, newsletter);
    }

    private void write(Newsletter newsletter, OutputPaths paths) throws IOException {
        Path markdown = markdownRenderer.write(newsletter, paths);
        feedRenderer.write(newsletter, paths);
        // 캘린더는 보관 정리 뒤에 만들어 남은 날짜만 보여 준다(FR-6.3).
        retentionCleaner.clean(paths, newsletter.date());
        // 남은 옛 디자인 날짜별 페이지는 그날 Markdown으로 다시 만든다. 캘린더가 새 페이지의 dn:* 값을 읽게 그 앞에 둔다.
        legacyArchiveUpgrader.upgrade(paths);
        calendarRenderer.write(paths, newsletter.generatedAt());
        homeRenderer.write(newsletter, paths);
        readmeUpdater.update(paths, newsletter);
        log.info("출력: {}, {}", paths.root().relativize(markdown), paths.root().relativize(paths.siteToday()));
    }

    private static void logSummary(Newsletter newsletter) {
        RunStats stats = newsletter.stats();
        log.info("결과: 수집 {}건 → 정제 {}건 → 선정 {}건, 본문 요약 {}건·대체 {}건, 실패 피드 {}", stats.collected(),
                stats.afterDedup(), stats.selected(), stats.bodyOk(), stats.bodyFallback(), stats.failedFeeds());
        log.info("키워드: {}", String.join(", ", newsletter.keywords()));
        newsletter.sections().forEach((category, items) -> log.info("{} {}건: {}", category.label(), items.size(),
                items.stream().map(DailyNewsPipeline::describe).collect(Collectors.joining(" / "))));
    }

    private static String describe(NewsItem item) {
        Story story = item.story();
        return story.representative().title() + " [" + story.outletCount() + "곳, " + item.summary().origin() + "]";
    }

    /**
     * @param exitCode   종료 코드({@link #EXIT_OK}, {@link #EXIT_FAILURE}, {@link #EXIT_OUTPUT_ERROR})
     * @param newsletter 만든 뉴스레터. 수집 0건이면 null
     */
    public record Result(int exitCode, Newsletter newsletter) {
    }
}
