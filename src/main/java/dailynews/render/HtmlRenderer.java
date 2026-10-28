package dailynews.render;

import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.NewsItem;
import dailynews.model.Newsletter;
import dailynews.model.RunStats;
import dailynews.model.SummaryOrigin;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 웹페이지를 만든다(FR-7.2): {@code site/index.html}(오늘)과 {@code site/archive/YYYY-MM-DD.html}(날짜별)을 쓰고
 * 공통 CSS·탭 스크립트를 {@code site/assets/}에 복사한다.
 */
public final class HtmlRenderer {

    private static final List<String> ASSETS = List.of("style.css", "tabs.js");

    private final String repositoryUrl;

    public HtmlRenderer(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    /**
     * @param base site 루트까지의 상대 경로. index.html은 "", archive/ 아래 페이지는 "../"
     */
    public String render(Newsletter newsletter, String base) {
        return Templates.process("newsletter", Map.of("page", page(newsletter, base)));
    }

    public void write(Newsletter newsletter, OutputPaths paths) throws IOException {
        Formats.write(paths.siteIndex(), render(newsletter, ""));
        Formats.write(paths.archivePage(newsletter.date()), render(newsletter, "../"));
        Files.createDirectories(paths.assetsDir());
        for (String asset : ASSETS) {
            copyResource("static/" + asset, paths.assetsDir().resolve(asset));
        }
    }

    private Page page(Newsletter newsletter, String base) {
        RunStats stats = newsletter.stats();
        String statsLine = "수집 %s · 수집 %d건 → 중복 제거 %d건 → 선정 %d건".formatted(
                String.join(" · ", newsletter.sourceNames()), stats.collected(), stats.afterDedup(), stats.selected());
        List<Section> sections = Arrays.stream(Category.values())
                .map(category -> new Section(category.slug(), category.label(), category.emoji(),
                        newsletter.sections().getOrDefault(category, List.of()).stream().map(HtmlRenderer::card).toList()))
                .toList();
        String dateLabel = Formats.dateLabel(newsletter.date());
        return new Page("Daily News · " + dateLabel, base, dateLabel, statsLine, stats.failedFeeds(),
                newsletter.keywords(), sections, Formats.generatedAt(newsletter.generatedAt()), repositoryUrl);
    }

    private static Card card(NewsItem item) {
        Article article = item.story().representative();
        List<Link> related = item.story().related().stream()
                .map(other -> new Link(other.source().name(), other.url().toString()))
                .toList();
        return new Card(article.title(), article.url().toString(), article.source().name(),
                Formats.time(article.publishedAt()), article.publishedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                item.story().outletCount(), item.summary().text(), item.summary().origin() == SummaryOrigin.RSS, related);
    }

    private static void copyResource(String resource, Path target) throws IOException {
        try (InputStream in = HtmlRenderer.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("리소스가 없습니다: " + resource);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** 템플릿에 넘기는 페이지 값(표시 문자열은 미리 만들어 둔다) */
    public record Page(String title, String base, String dateLabel, String statsLine, List<String> failedFeeds,
                       List<String> keywords, List<Section> sections, String generatedAt, String repositoryUrl) {
    }

    public record Section(String slug, String label, String emoji, List<Card> cards) {
    }

    public record Card(String title, String url, String sourceName, String time, String isoTime, int outletCount,
                       String summary, boolean rssSummary, List<Link> related) {
    }

    public record Link(String name, String url) {
    }
}
