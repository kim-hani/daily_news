package dailynews.select;

import static dailynews.TestArticles.DONGA;
import static dailynews.TestArticles.KHAN;
import static dailynews.TestArticles.YONHAP;
import static dailynews.TestArticles.article;
import static dailynews.TestArticles.at;
import static org.assertj.core.api.Assertions.assertThat;

import dailynews.keyword.KomoranAnalyzer;
import dailynews.model.Article;
import dailynews.model.Category;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoryClustererTest {

    private final StoryClusterer clusterer =
            new StoryClusterer(new TitleSimilarity(KomoranAnalyzer.getInstance()), 0.4);

    private final Article yonhapVisa = article(YONHAP, Category.POLITICS,
            "진성준 \"美, 태평양사령부 방문 위한 비자발급 거부…강력 유감\"", "https://www.yna.co.kr/view/1", at(18, 27));
    private final Article dongaVisa = article(DONGA, Category.POLITICS,
            "‘반미 전과’ 與진성준 국방위원장, 美비자 발급 거부당해", "https://www.donga.com/news/1", at(19, 22));
    private final Article khanVisa = article(KHAN, Category.POLITICS,
            "미국, 이번엔 진성준 국방위원장 비자 발급 ‘거부’···진 위원장 “강력 유감”", "https://www.khan.co.kr/article/1", at(18, 22));
    private final Article single1 = article(DONGA, Category.POLITICS,
            "법무부, 제81주년 교정의날 기념식…45명 포상", "https://www.donga.com/news/2", at(20, 16));
    private final Article single2 = article(YONHAP, Category.POLITICS,
            "국가공무원 7급 2차시험 820명 합격…평균연령 28.7세", "https://www.yna.co.kr/view/2", at(18, 0));

    @Test
    void 세_언론사가_보도한_사안_하나와_단독_기사_둘은_클러스터_셋이_된다() {
        List<ArticleCluster> clusters = clusterer.cluster(List.of(single1, yonhapVisa, single2, dongaVisa, khanVisa))
                .get(Category.POLITICS);

        assertThat(clusters).hasSize(3);
        assertThat(clusters).extracting(ArticleCluster::outletCount).containsExactlyInAnyOrder(3, 1, 1);
        assertThat(clusters).filteredOn(cluster -> cluster.outletCount() == 3).singleElement()
                .satisfies(cluster -> assertThat(cluster.articles())
                        .containsExactlyInAnyOrder(yonhapVisa, dongaVisa, khanVisa));
    }

    @Test
    void 같은_언론사의_기사가_여럿이어도_언론사_수는_한_번만_센다() {
        Article yonhapVisaUpdate = article(YONHAP, Category.POLITICS,
                "진성준 \"美 비자발급 거부 강력 유감\"…국방위원장 방미 무산", "https://www.yna.co.kr/view/3", at(20, 0));

        List<ArticleCluster> clusters = clusterer.cluster(List.of(yonhapVisa, yonhapVisaUpdate)).get(Category.POLITICS);

        assertThat(clusters).singleElement().satisfies(cluster -> {
            assertThat(cluster.articles()).hasSize(2);
            assertThat(cluster.outletCount()).isEqualTo(1);
        });
    }

    @Test
    void 카테고리가_다르면_같은_제목도_따로_묶는다() {
        Article global = article(KHAN, Category.GLOBAL, khanVisa.title(), "https://www.khan.co.kr/article/9", at(18, 22));

        Map<Category, List<ArticleCluster>> clusters = clusterer.cluster(List.of(khanVisa, global));

        assertThat(clusters).containsOnlyKeys(Category.POLITICS, Category.GLOBAL);
        assertThat(clusters.get(Category.POLITICS)).hasSize(1);
        assertThat(clusters.get(Category.GLOBAL)).hasSize(1);
    }

    @Test
    void 기사가_없으면_빈_결과다() {
        assertThat(clusterer.cluster(List.of())).isEmpty();
    }
}
