package dailynews.select;

import static dailynews.TestArticles.DONGA;
import static dailynews.TestArticles.SOURCES;
import static dailynews.TestArticles.YONHAP;
import static dailynews.TestArticles.article;
import static dailynews.TestArticles.at;
import static org.assertj.core.api.Assertions.assertThat;

import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.Story;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class StorySelectorTest {

    private static final ZonedDateTime NOW = at(22, 0);

    private final StorySelector selector = new StorySelector(new StoryRanker(SOURCES), 5);

    @Test
    void 카테고리마다_점수_상위_5개만_고른다() {
        List<ArticleCluster> clusters = new ArrayList<>();
        IntStream.rangeClosed(1, 7).forEach(i -> clusters.add(single(Category.POLITICS, "기사" + i, i, at(10 + i, 0))));

        Map<Category, List<Story>> selected = selector.select(Map.of(Category.POLITICS, clusters), NOW);

        assertThat(selected.get(Category.POLITICS)).extracting(story -> story.representative().title())
                .containsExactly("기사7", "기사6", "기사5", "기사4", "기사3");
    }

    @Test
    void 후보가_5개보다_적으면_있는_만큼만_고르고_빈_카테고리도_결과에_있다() {
        List<ArticleCluster> clusters = List.of(single(Category.IT, "가", 1, at(9, 0)),
                single(Category.IT, "나", 2, at(9, 0)), single(Category.IT, "다", 3, at(9, 0)));

        Map<Category, List<Story>> selected = selector.select(Map.of(Category.IT, clusters), NOW);

        assertThat(selected.keySet()).containsExactly(Category.values());
        assertThat(selected.get(Category.IT)).hasSize(3);
        assertThat(selected.get(Category.POLITICS)).isEmpty();
    }

    @Test
    void 앞_카테고리에서_고른_기사는_뒤_카테고리에서_빠진다() {
        Article dongaEconomy = article(DONGA, Category.ECONOMY, "반도체 수출 증가", "https://www.donga.com/news/9", at(9, 0));
        Article dongaIt = article(DONGA, Category.IT, "반도체 수출 증가", "https://www.donga.com/news/9", at(9, 0));
        Article yonhapIt = article(YONHAP, Category.IT, "반도체 수출 증가", "https://www.yna.co.kr/view/9", at(9, 30));

        Map<Category, List<ArticleCluster>> clusters = new EnumMap<>(Category.class);
        clusters.put(Category.ECONOMY, List.of(new ArticleCluster(Category.ECONOMY, List.of(dongaEconomy))));
        clusters.put(Category.IT, List.of(new ArticleCluster(Category.IT, List.of(dongaIt, yonhapIt))));

        Map<Category, List<Story>> selected = selector.select(clusters, NOW);

        assertThat(selected.get(Category.ECONOMY)).singleElement()
                .satisfies(story -> assertThat(story.representative()).isEqualTo(dongaEconomy));
        assertThat(selected.get(Category.IT)).singleElement().satisfies(story -> {
            assertThat(story.representative()).isEqualTo(yonhapIt);
            assertThat(story.outletCount()).isEqualTo(1);
            assertThat(story.related()).isEmpty();
        });
    }

    @Test
    void 글로벌을_IT보다_먼저_고른다() {
        Article global = article(YONHAP, Category.GLOBAL, "미 빅테크 실적", "https://www.yna.co.kr/view/7", at(9, 0));
        Article it = article(YONHAP, Category.IT, "미 빅테크 실적", "https://www.yna.co.kr/view/7", at(9, 0));

        Map<Category, List<ArticleCluster>> clusters = new EnumMap<>(Category.class);
        clusters.put(Category.IT, List.of(new ArticleCluster(Category.IT, List.of(it))));
        clusters.put(Category.GLOBAL, List.of(new ArticleCluster(Category.GLOBAL, List.of(global))));

        Map<Category, List<Story>> selected = selector.select(clusters, NOW);

        assertThat(selected.get(Category.GLOBAL)).hasSize(1);
        assertThat(selected.get(Category.IT)).isEmpty();
    }

    private static ArticleCluster single(Category category, String title, int id, ZonedDateTime at) {
        return new ArticleCluster(category,
                List.of(article(YONHAP, category, title, "https://www.yna.co.kr/view/" + category + id, at)));
    }
}
