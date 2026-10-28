package dailynews.clean;

import static dailynews.TestArticles.DONGA;
import static dailynews.TestArticles.KHAN;
import static dailynews.TestArticles.YONHAP;
import static dailynews.TestArticles.article;
import static dailynews.TestArticles.at;
import static org.assertj.core.api.Assertions.assertThat;

import dailynews.model.Article;
import dailynews.model.Category;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArticleCleanerTest {

    /** 실행 시각: 2026-10-28 09:00 KST */
    private final Clock clock = Clock.fixed(at(9, 0).toInstant(), at(9, 0).getZone());
    private final ArticleCleaner cleaner = new ArticleCleaner(new UrlNormalizer(), clock, Duration.ofHours(24));

    @Test
    void 정확히_24시간_전은_포함하고_1초라도_넘으면_제외한다() {
        Article boundary = article(YONHAP, Category.POLITICS, "경계", "https://www.yna.co.kr/view/A1",
                at(9, 0).minusHours(24));
        Article tooOld = article(YONHAP, Category.POLITICS, "초과", "https://www.yna.co.kr/view/A2",
                at(9, 0).minusHours(24).minusSeconds(1));
        Article now = article(YONHAP, Category.POLITICS, "지금", "https://www.yna.co.kr/view/A3", at(9, 0));
        Article future = article(YONHAP, Category.POLITICS, "미래", "https://www.yna.co.kr/view/A4",
                at(9, 0).plusSeconds(1));

        assertThat(cleaner.clean(List.of(boundary, tooOld, now, future)))
                .extracting(Article::title).containsExactly("경계", "지금");
    }

    @Test
    void URL을_정규화하고_허용되지_않는_링크는_버린다() {
        Article khan = article(KHAN, Category.POLITICS, "경향 기사",
                "https://www.khan.co.kr/article/202610280800001/?utm_source=khan_rss&utm_medium=rss", at(8, 0));
        Article foreign = article(YONHAP, Category.POLITICS, "다른 도메인", "https://evil.example.com/a", at(8, 0));

        assertThat(cleaner.clean(List.of(khan, foreign)))
                .extracting(Article::url)
                .containsExactly(URI.create("https://www.khan.co.kr/article/202610280800001"));
    }

    @Test
    void 같은_카테고리에서_URL이_같으면_가장_이른_기사만_남긴다() {
        Article later = article(KHAN, Category.POLITICS, "나중 제목",
                "https://www.khan.co.kr/article/1/?utm_source=khan_rss", at(8, 0));
        Article earlier = article(KHAN, Category.POLITICS, "처음 제목", "https://www.khan.co.kr/article/1", at(7, 0));

        assertThat(cleaner.clean(List.of(later, earlier))).extracting(Article::title).containsExactly("처음 제목");
    }

    @Test
    void 같은_언론사에서_정규화_제목이_같으면_가장_이른_기사만_남긴다() {
        Article update = article(YONHAP, Category.POLITICS, "한미 국방장관, SCM 내년초 개최(종합2보)",
                "https://www.yna.co.kr/view/AKR2", at(8, 30));
        Article first = article(YONHAP, Category.POLITICS, "[속보] 한미 국방장관, SCM 내년초 개최",
                "https://www.yna.co.kr/view/AKR1", at(6, 0));
        Article otherOutlet = article(DONGA, Category.POLITICS, "한미 국방장관, SCM 내년초 개최",
                "https://www.donga.com/news/1", at(7, 0));

        assertThat(cleaner.clean(List.of(update, first, otherOutlet)))
                .extracting(Article::url)
                .containsExactly(URI.create("https://www.yna.co.kr/view/AKR1"), URI.create("https://www.donga.com/news/1"));
    }

    @Test
    void 카테고리가_다르면_같은_기사도_합치지_않는다() {
        Article economy = article(DONGA, Category.ECONOMY, "반도체 수출 증가", "https://www.donga.com/news/9", at(8, 0));
        Article it = article(DONGA, Category.IT, "반도체 수출 증가", "https://www.donga.com/news/9", at(8, 0));

        assertThat(cleaner.clean(List.of(economy, it))).hasSize(2);
    }

    @Test
    void 결과는_입력_순서를_유지한다() {
        Article a = article(YONHAP, Category.POLITICS, "가", "https://www.yna.co.kr/view/1", at(8, 0));
        Article b = article(YONHAP, Category.POLITICS, "나", "https://www.yna.co.kr/view/2", at(6, 0));
        Article c = article(YONHAP, Category.POLITICS, "다", "https://www.yna.co.kr/view/3", at(7, 0));

        assertThat(cleaner.clean(List.of(a, b, c))).extracting(Article::title).containsExactly("가", "나", "다");
    }
}
