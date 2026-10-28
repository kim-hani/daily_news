package dailynews.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ModelImmutabilityTest {

    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 10, 28, 9, 0, 0, 0, ZoneId.of("Asia/Seoul"));

    @Test
    void Source는_입력_컬렉션을_복사하고_feeds를_Category_순서로_둔다() {
        List<Pattern> patterns = new ArrayList<>(List.of(Pattern.compile("^x")));
        Map<Category, URI> feeds = new LinkedHashMap<>();
        feeds.put(Category.GLOBAL, URI.create("https://a.example.com/global.xml"));
        feeds.put(Category.POLITICS, URI.create("https://a.example.com/politics.xml"));
        Set<Category> filterFeeds = new HashSet<>(Set.of(Category.GLOBAL));

        Source source = new Source("a", "A일보", List.of("A.example.com"), "div", patterns, feeds, filterFeeds);
        patterns.clear();
        feeds.clear();
        filterFeeds.clear();

        assertThat(source.creditPatterns()).hasSize(1);
        assertThat(source.domains()).containsExactly("a.example.com");
        assertThat(source.ownsHost("WWW.A.example.com")).isTrue();
        assertThat(source.ownsHost("xa.example.com")).isFalse();
        assertThat(source.feeds().keySet()).containsExactly(Category.POLITICS, Category.GLOBAL);
        assertThat(source.itKeywordFilterFeeds()).containsExactly(Category.GLOBAL);
        assertThatThrownBy(() -> source.feeds().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> source.itKeywordFilterFeeds().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void Source는_빈_컬렉션도_받는다() {
        Source source = new Source("a", "A일보", List.of("a.example.com"), "div", List.of(), Map.of(), Set.of());

        assertThat(source.feeds()).isEmpty();
        assertThat(source.itKeywordFilterFeeds()).isEmpty();
    }

    @Test
    void Story의_related는_원본_리스트와_분리된다() {
        Article representative = article("대표 기사");
        List<Article> related = new ArrayList<>(List.of(article("관련 기사")));

        Story story = new Story(Category.POLITICS, representative, related, 2, 20.5);
        related.clear();

        assertThat(story.related()).hasSize(1);
        assertThatThrownBy(() -> story.related().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void Newsletter는_sections를_Category_순서로_두고_깊게_복사한다() {
        List<NewsItem> globalItems = new ArrayList<>(List.of(newsItem(Category.GLOBAL)));
        Map<Category, List<NewsItem>> sections = new LinkedHashMap<>();
        sections.put(Category.GLOBAL, globalItems);
        sections.put(Category.POLITICS, List.of(newsItem(Category.POLITICS)));
        List<String> keywords = new ArrayList<>(List.of("예산안"));

        Newsletter newsletter = new Newsletter(LocalDate.of(2026, 10, 28), NOW, List.of("A일보"), sections, keywords, emptyStats());
        globalItems.clear();
        sections.clear();
        keywords.clear();

        assertThat(newsletter.sections().keySet()).containsExactly(Category.POLITICS, Category.GLOBAL);
        assertThat(newsletter.sections().get(Category.GLOBAL)).hasSize(1);
        assertThat(newsletter.keywords()).containsExactly("예산안");
        assertThatThrownBy(() -> newsletter.sections().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> newsletter.sections().get(Category.GLOBAL).clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void RunStats의_failedFeeds는_원본_리스트와_분리된다() {
        List<String> failedFeeds = new ArrayList<>(List.of("yonhap/POLITICS"));

        RunStats stats = new RunStats(10, 8, 5, failedFeeds, 4, 1);
        failedFeeds.clear();

        assertThat(stats.failedFeeds()).containsExactly("yonhap/POLITICS");
    }

    private static Source source() {
        return new Source("a", "A일보", List.of("a.example.com"), "div", List.of(), Map.of(), Set.of());
    }

    private static Article article(String title) {
        return new Article(source(), Category.POLITICS, title, URI.create("https://a.example.com/1"), NOW, "");
    }

    private static NewsItem newsItem(Category category) {
        Story story = new Story(category, article("기사"), List.of(), 1, 10.0);
        return new NewsItem(story, new Summary("요약", SummaryOrigin.BODY));
    }

    private static RunStats emptyStats() {
        return new RunStats(0, 0, 0, List.of(), 0, 0);
    }
}
