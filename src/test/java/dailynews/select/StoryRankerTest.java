package dailynews.select;

import static dailynews.TestArticles.DONGA;
import static dailynews.TestArticles.KHAN;
import static dailynews.TestArticles.SOURCES;
import static dailynews.TestArticles.YONHAP;
import static dailynews.TestArticles.article;
import static dailynews.TestArticles.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.Story;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoryRankerTest {

    private static final ZonedDateTime NOW = at(22, 0);

    private final StoryRanker ranker = new StoryRanker(SOURCES);

    @Test
    void 점수는_언론사_수_곱하기_10에_최신성을_더한다() {
        ArticleCluster cluster = cluster(
                article(YONHAP, Category.POLITICS, "가", "https://www.yna.co.kr/view/1", at(10, 0)),
                article(DONGA, Category.POLITICS, "가", "https://www.donga.com/news/1", at(16, 0)));

        Story story = ranker.toStory(cluster, NOW);

        assertThat(story.outletCount()).isEqualTo(2);
        assertThat(story.score()).isCloseTo(20 + (1 - 6.0 / 24), within(1e-9));
    }

    @Test
    void 최신성은_0에서_1_사이다() {
        assertThat(StoryRanker.recency(cluster(article(YONHAP, Category.POLITICS, "가", "https://www.yna.co.kr/view/1",
                NOW.minusHours(30))), NOW)).isZero();
        assertThat(StoryRanker.recency(cluster(article(YONHAP, Category.POLITICS, "가", "https://www.yna.co.kr/view/1",
                NOW)), NOW)).isEqualTo(1.0);
    }

    @Test
    void 대표_기사는_가장_먼저_보도된_기사이고_같은_사안은_다른_언론사마다_가장_이른_1건이다() {
        Article khanFirst = article(KHAN, Category.POLITICS, "가", "https://www.khan.co.kr/article/1", at(8, 0));
        Article yonhapLater = article(YONHAP, Category.POLITICS, "가", "https://www.yna.co.kr/view/2", at(12, 0));
        Article yonhapEarlier = article(YONHAP, Category.POLITICS, "가", "https://www.yna.co.kr/view/1", at(9, 0));
        Article donga = article(DONGA, Category.POLITICS, "가", "https://www.donga.com/news/1", at(10, 0));
        Article khanLater = article(KHAN, Category.POLITICS, "가", "https://www.khan.co.kr/article/2", at(11, 0));

        Story story = ranker.toStory(cluster(yonhapLater, donga, khanFirst, yonhapEarlier, khanLater), NOW);

        assertThat(story.representative()).isEqualTo(khanFirst);
        assertThat(story.related()).containsExactly(yonhapEarlier, donga);
    }

    @Test
    void 보도_시각이_같으면_설정의_언론사_순서로_대표를_정한다() {
        Article khan = article(KHAN, Category.POLITICS, "가", "https://www.khan.co.kr/article/1", at(8, 0));
        Article donga = article(DONGA, Category.POLITICS, "가", "https://www.donga.com/news/1", at(8, 0));

        assertThat(ranker.toStory(cluster(khan, donga), NOW).representative()).isEqualTo(donga);
    }

    @Test
    void 점수가_높은_순으로_정렬하고_같으면_기사가_많은_쪽이_먼저다() {
        ArticleCluster single = cluster(article(YONHAP, Category.POLITICS, "단독", "https://www.yna.co.kr/view/1",
                at(21, 0)));
        ArticleCluster twoOutlets = cluster(
                article(YONHAP, Category.POLITICS, "두 곳", "https://www.yna.co.kr/view/2", at(9, 0)),
                article(DONGA, Category.POLITICS, "두 곳", "https://www.donga.com/news/2", at(9, 0)));
        ArticleCluster singleTwoArticles = cluster(
                article(KHAN, Category.POLITICS, "단독 두 건", "https://www.khan.co.kr/article/3", at(21, 0)),
                article(KHAN, Category.POLITICS, "단독 두 건", "https://www.khan.co.kr/article/4", at(21, 0)));

        assertThat(ranker.rank(List.of(single, twoOutlets, singleTwoArticles), NOW))
                .extracting(ranked -> ranked.story().representative().title())
                .containsExactly("두 곳", "단독 두 건", "단독");
    }

    private static ArticleCluster cluster(Article... articles) {
        return new ArticleCluster(Category.POLITICS, List.of(articles));
    }
}
