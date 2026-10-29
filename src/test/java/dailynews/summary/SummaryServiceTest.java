package dailynews.summary;

import static dailynews.TestArticles.DONGA;
import static dailynews.TestArticles.KHAN;
import static dailynews.TestArticles.YONHAP;
import static dailynews.TestArticles.article;
import static dailynews.TestArticles.at;
import static org.assertj.core.api.Assertions.assertThat;

import dailynews.http.FakeHttpFetcher;
import dailynews.http.RobotsPolicy;
import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.NewsItem;
import dailynews.model.Story;
import dailynews.model.SummaryOrigin;
import java.net.URI;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SummaryServiceTest {

    private static final String RSS = "RSS 설명문의 첫 문장입니다. 둘째 문장입니다.";

    private final FakeHttpFetcher fetcher = new FakeHttpFetcher()
            .respondWithFixture("https://www.yna.co.kr/robots.txt", "/fixtures/robots/yonhap.txt", "text/plain")
            .respondWithFixture("https://www.donga.com/robots.txt", "/fixtures/robots/donga.txt", "text/plain")
            .respondWithFixture("https://www.khan.co.kr/robots.txt", "/fixtures/robots/khan.txt", "text/plain")
            .respondWithFixture("https://www.yna.co.kr/view/AKR20261028090952504", "/fixtures/yonhap/article-1.html",
                    "text/html;charset=UTF-8")
            .respondWithFixture("https://www.khan.co.kr/article/202610282137015", "/fixtures/khan/article-1.html",
                    "text/html; charset=UTF-8");
    private final SummaryService service = new SummaryService(fetcher, new RobotsPolicy(fetcher), new BodyExtractor(),
            new LeadSummarizer(new SentenceSplitter()));

    private final Story yonhapOk = story(article(YONHAP, Category.POLITICS, "한미 국방장관 회담",
            "https://www.yna.co.kr/view/AKR20261028090952504", at(21, 55), RSS));
    private final Story dongaMissing = story(article(DONGA, Category.POLITICS, "사라진 기사",
            "https://www.donga.com/news/Politics/article/all/20261028/1/1", at(21, 0), RSS));
    private final Story yonhapBlocked = story(article(YONHAP, Category.IT, "영문 기사",
            "https://www.yna.co.kr/view/AEN20261028000100315", at(20, 0), RSS));
    private final Story khanSkipped = story(article(KHAN, Category.IT, "다문화 우대 검토",
            "https://www.khan.co.kr/article/202610282137015", at(21, 37), RSS));

    @Test
    void 원문을_받으면_본문으로_요약하고_404나_robots_차단이면_RSS로_대체한다() {
        SummaryService.Result result = service.summarize(stories());

        assertThat(result.sections().get(Category.POLITICS)).extracting(item -> item.summary().origin())
                .containsExactly(SummaryOrigin.BODY, SummaryOrigin.RSS);
        assertThat(result.sections().get(Category.IT)).extracting(item -> item.summary().origin())
                .containsExactly(SummaryOrigin.RSS, SummaryOrigin.RSS);
        assertThat(result.bodyOk()).isEqualTo(1);
        assertThat(result.bodyFallback()).isEqualTo(3);
        assertThat(result.sections().get(Category.POLITICS).get(1).summary().text()).isEqualTo(RSS);
    }

    @Test
    void 원문_가져오기를_끈_언론사는_원문을_요청하지_않고_RSS로_요약한다() {
        assertThat(KHAN.fetchBody()).isFalse();

        SummaryService.Result result = service.summarize(stories());

        assertThat(result.sections().get(Category.IT).get(1).summary().text()).isEqualTo(RSS);
        assertThat(fetcher.requests()).noneMatch(uri -> uri.getHost().equals("www.khan.co.kr"));
    }

    @Test
    void 사안과_순서를_그대로_두고_요약만_붙인다() {
        SummaryService.Result result = service.summarize(stories());

        assertThat(result.sections().keySet()).containsExactly(Category.POLITICS, Category.IT);
        assertThat(result.sections().get(Category.POLITICS)).extracting(NewsItem::story)
                .containsExactly(yonhapOk, dongaMissing);
    }

    @Test
    void 원문_요청은_선정_기사_수를_넘지_않고_robots에_막힌_기사는_요청하지_않는다() {
        service.summarize(stories());

        List<URI> articleRequests = fetcher.requests().stream()
                .filter(uri -> !uri.getPath().equals("/robots.txt"))
                .toList();
        assertThat(articleRequests).hasSizeLessThanOrEqualTo(4)
                .doesNotContain(URI.create("https://www.yna.co.kr/view/AEN20261028000100315"));
    }

    private Map<Category, List<Story>> stories() {
        Map<Category, List<Story>> stories = new EnumMap<>(Category.class);
        stories.put(Category.POLITICS, List.of(yonhapOk, dongaMissing));
        stories.put(Category.IT, List.of(yonhapBlocked, khanSkipped));
        return stories;
    }

    private static Story story(Article representative) {
        return new Story(representative.category(), representative, List.of(), 1, 10.5);
    }
}
