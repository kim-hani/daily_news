package dailynews.render;

import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.NewsItem;
import dailynews.model.Newsletter;
import dailynews.model.RunStats;
import dailynews.model.SummaryOrigin;
import dailynews.render.Chrome.Nav;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 뉴스피드 페이지를 만든다(spec FR-3, plan §4.3): 최신 뉴스레터는 {@code site/today.html},
 * 날짜별은 {@code site/archive/YYYY-MM-DD.html}에 쓰고 정적 자원을 {@code site/assets/}에 복사한다.
 */
public final class FeedRenderer {

    /** 뉴스피드 상단에 보여 줄 키워드 수(FR-3.9) */
    static final int MAX_KEYWORDS = 10;

    private final String repositoryUrl;

    public FeedRenderer(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    /**
     * @param archive true면 날짜별 페이지({@code archive/} 아래, 이전/다음 이동 포함), false면 {@code today.html}
     */
    public String render(Newsletter newsletter, boolean archive) {
        return Templates.process("newsletter", Map.of("page", page(newsletter, archive)));
    }

    public void write(Newsletter newsletter, OutputPaths paths) throws IOException {
        Formats.write(paths.siteToday(), render(newsletter, false));
        writeArchive(newsletter, paths);
        SiteAssets.copy(paths);
    }

    /** 날짜별 페이지만 쓴다(today.html과 정적 자원은 건드리지 않는다). */
    public void writeArchive(Newsletter newsletter, OutputPaths paths) throws IOException {
        Formats.write(paths.archivePage(newsletter.date()), render(newsletter, true));
    }

    FeedPage page(Newsletter newsletter, boolean archive) {
        RunStats stats = newsletter.stats();
        String statsLine = "수집 %s · 수집 %d건 → 중복 제거 %d건 → 선정 %d건".formatted(
                String.join(" · ", newsletter.sourceNames()), stats.collected(), stats.afterDedup(), stats.selected());
        List<Stat> numbers = List.of(new Stat("수집", stats.collected()), new Stat("중복 제거", stats.afterDedup()),
                new Stat("선정", stats.selected()));
        List<Section> sections = Arrays.stream(Category.values())
                .map(category -> section(category, newsletter.sections().getOrDefault(category, List.of())))
                .toList();
        String dateLabel = Formats.dateLabel(newsletter.date());
        String heading = archive ? "지난 뉴스피드" : "오늘의 뉴스피드";
        Chrome chrome = new Chrome(dateLabel + " " + heading + " · Daily News",
                dateLabel + " 정치·사회·경제·IT·글로벌 주요 뉴스 " + stats.selected() + "건을 언론사 원문 링크와 함께 모았습니다.",
                archive ? "../" : "", archive ? Nav.NONE : Nav.TODAY, Formats.generatedAt(newsletter.generatedAt()),
                repositoryUrl);
        List<String> keywords = newsletter.keywords().stream().limit(MAX_KEYWORDS).toList();
        return new FeedPage(chrome, newsletter.date(), dateLabel, archive, heading, statsLine, numbers,
                stats.failedFeeds(), keywords, stats.selected(), sections);
    }

    private static Section section(Category category, List<NewsItem> items) {
        List<Card> cards = new ArrayList<>();
        for (NewsItem item : items) {
            cards.add(card(cards.size() + 1, item));
        }
        return new Section(category.slug(), category.label(), category.emoji(), cards.size(), cards);
    }

    private static Card card(int rank, NewsItem item) {
        Article article = item.story().representative();
        List<Link> related = item.story().related().stream()
                .map(other -> new Link(other.source().name(), other.url().toString()))
                .toList();
        return new Card(rank, article.title(), article.url().toString(), article.source().name(),
                Formats.time(article.publishedAt()), article.publishedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                item.story().outletCount(), item.summary().text(), item.summary().origin() == SummaryOrigin.RSS, related);
    }

    /**
     * 템플릿에 넘기는 뉴스피드 값(표시 문자열은 미리 만들어 둔다).
     *
     * @param heading   날짜 위의 작은 제목("오늘의 뉴스피드" / "지난 뉴스피드")
     * @param statsLine 언론사 이름이 든 통계 한 줄(스크린리더와 JS 없는 환경용 문장, FR-3.2)
     * @param stats     카운트업 숫자 3개(수집 → 중복 제거 → 선정, FR-3.9)
     * @param keywords  최대 {@value #MAX_KEYWORDS}개
     */
    public record FeedPage(Chrome chrome, LocalDate date, String dateLabel, boolean archive, String heading,
                           String statsLine, List<Stat> stats, List<String> failedFeeds, List<String> keywords,
                           int selected, List<Section> sections) {

        public String isoDate() {
            return date.toString();
        }

        /** 캘린더가 읽을 {@code dn:*} 메타 태그 값(plan §5.3) */
        public DayDigest digest() {
            return new DayDigest(date, selected, keywords);
        }
    }

    /** 통계 숫자 하나. {@link #display()}는 HTML에 처음부터 들어가는 최종 숫자다(NFR-7). */
    public record Stat(String label, int value) {

        public String display() {
            return String.format(Locale.ROOT, "%,d", value);
        }
    }

    /** @param count 탭에 보여 줄 기사 수 */
    public record Section(String slug, String label, String emoji, int count, List<Card> cards) {
    }

    /** @param rank 카테고리 안의 순위(1부터) */
    public record Card(int rank, String title, String url, String sourceName, String time, String isoTime,
                       int outletCount, String summary, boolean rssSummary, List<Link> related) {
    }

    public record Link(String name, String url) {
    }
}
