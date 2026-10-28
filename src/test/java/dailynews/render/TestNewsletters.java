package dailynews.render;

import static dailynews.TestArticles.DONGA;
import static dailynews.TestArticles.KHAN;
import static dailynews.TestArticles.YONHAP;
import static dailynews.TestArticles.article;
import static dailynews.TestArticles.at;

import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.NewsItem;
import dailynews.model.Newsletter;
import dailynews.model.RunStats;
import dailynews.model.Story;
import dailynews.model.Summary;
import dailynews.model.SummaryOrigin;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 렌더러 테스트용 고정 뉴스레터(2026-10-28 수요일). IT 카테고리는 비어 있다.
 */
final class TestNewsletters {

    static final LocalDate DATE = LocalDate.of(2026, 10, 28);
    static final String XSS_TITLE = "[단독] <script>alert(1)</script> *주의* 국회_예산 #1";

    private TestNewsletters() {
    }

    static Newsletter sample() {
        Article yonhap = article(YONHAP, Category.POLITICS, "한미 국방장관, SCM 내년초 개최", "https://www.yna.co.kr/view/AKR1",
                at(8, 12));
        Article donga = article(DONGA, Category.POLITICS, "SCM 내년으로", "https://www.donga.com/news/Politics/article/1",
                at(9, 0));
        Article khan = article(KHAN, Category.POLITICS, "SCM 연기", "https://www.khan.co.kr/article/202610280900001",
                at(9, 30));
        Article xss = article(DONGA, Category.POLITICS, XSS_TITLE, "https://www.donga.com/news/Politics/article/2",
                at(7, 5));
        Article society = article(KHAN, Category.SOCIETY, "서울 첫눈 관측", "https://www.khan.co.kr/article/202610280700001",
                at(7, 0));
        Article economy = article(YONHAP, Category.ECONOMY, "반도체 수출 증가", "https://www.yna.co.kr/view/AKR2", at(6, 30));
        Article global = article(YONHAP, Category.GLOBAL, "미국 대선 토론", "https://www.yna.co.kr/view/AKR3", at(5, 0));

        Map<Category, List<NewsItem>> sections = new EnumMap<>(Category.class);
        sections.put(Category.POLITICS, List.of(
                new NewsItem(new Story(Category.POLITICS, yonhap, List.of(donga, khan), 3, 30.9),
                        new Summary("한미 국방장관이 내년 초 SCM을 열기로 합의했다.", SummaryOrigin.BODY)),
                new NewsItem(new Story(Category.POLITICS, xss, List.of(), 1, 10.5),
                        new Summary("요약에 <b>태그</b>와 [괄호]가 있다.", SummaryOrigin.RSS))));
        sections.put(Category.SOCIETY, List.of(new NewsItem(new Story(Category.SOCIETY, society, List.of(), 1, 10.2),
                new Summary("", SummaryOrigin.NONE))));
        sections.put(Category.ECONOMY, List.of(new NewsItem(new Story(Category.ECONOMY, economy, List.of(), 1, 10.1),
                new Summary("반도체 수출이 늘었다.", SummaryOrigin.BODY))));
        sections.put(Category.GLOBAL, List.of(new NewsItem(new Story(Category.GLOBAL, global, List.of(), 1, 10.0),
                new Summary("토론이 열렸다.", SummaryOrigin.BODY))));
        sections.put(Category.IT, List.of());

        return new Newsletter(DATE, at(9, 0), List.of("연합뉴스", "동아일보", "경향신문"), sections,
                List.of("SCM", "반도체", "국회"), new RunStats(512, 431, 5, List.of("khan/IT"), 4, 1));
    }
}
