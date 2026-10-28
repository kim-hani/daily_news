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
import dailynews.render.ArchiveRenderer;
import dailynews.render.HtmlRenderer;
import dailynews.render.MarkdownRenderer;
import dailynews.render.OutputPaths;
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
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 뉴스레터 한 번 만들기(plan §3): 수집 → 정제 → IT 필터 → 클러스터 → 선정 → 요약 → 키워드 →
 * Markdown·HTML → 보관 정리 → 아카이브 목록 → README.
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
    private final ArticleCleaner cleaner;
    private final CategoryFilter categoryFilter;
    private final StoryClusterer clusterer;
    private final StorySelector selector;
    private final SummaryService summaryService;
    private final KeywordExtractor keywordExtractor;
    private final MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    private final HtmlRenderer htmlRenderer;
    private final RetentionCleaner retentionCleaner;
    private final ArchiveRenderer archiveRenderer;
    private final ReadmeUpdater readmeUpdater;

    public DailyNewsPipeline(AppConfig config, HttpFetcher fetcher, Clock clock) {
        this.config = config;
        this.clock = clock;
        KomoranAnalyzer analyzer = KomoranAnalyzer.getInstance();
        RobotsPolicy robots = new RobotsPolicy(fetcher);
        this.collector = new FeedCollector(fetcher, robots, new RssParser(KST));
        this.cleaner = new ArticleCleaner(new UrlNormalizer(), clock, Duration.ofHours(config.windowHours()));
        this.categoryFilter = new CategoryFilter(config.itKeywords());
        this.clusterer = new StoryClusterer(new TitleSimilarity(analyzer), config.clusterThreshold());
        this.selector = new StorySelector(new StoryRanker(config.sources()), config.perCategory());
        this.summaryService = new SummaryService(fetcher, robots, new BodyExtractor(),
                new LeadSummarizer(new SentenceSplitter()));
        this.keywordExtractor = new KeywordExtractor(analyzer::nouns, config.stopwords());
        this.htmlRenderer = new HtmlRenderer(config.repositoryUrl());
        this.retentionCleaner = new RetentionCleaner(config.retention());
        this.archiveRenderer = new ArchiveRenderer(config.repositoryUrl(), config.retention().siteArchiveDays());
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

        CollectResult collected = collector.collect(config.sources());
        if (collected.articles().isEmpty()) {
            log.error("수집한 기사가 없어 뉴스레터를 만들지 않습니다(실패 피드 {}개)", collected.failedFeeds().size());
            return new Result(EXIT_FAILURE, null);
        }
        List<Article> cleaned = cleaner.clean(collected.articles());
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
                write(newsletter, new OutputPaths(root));
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
        htmlRenderer.write(newsletter, paths);
        retentionCleaner.clean(paths, newsletter.date());
        archiveRenderer.write(paths, newsletter.generatedAt());
        readmeUpdater.update(paths, newsletter);
        log.info("출력: {}, {}", paths.root().relativize(markdown), paths.root().relativize(paths.siteIndex()));
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
