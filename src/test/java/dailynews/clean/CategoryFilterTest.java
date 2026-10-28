package dailynews.clean;

import static dailynews.TestArticles.DONGA;
import static dailynews.TestArticles.YONHAP;
import static dailynews.TestArticles.article;
import static dailynews.TestArticles.at;
import static org.assertj.core.api.Assertions.assertThat;

import dailynews.config.ConfigLoader;
import dailynews.model.Article;
import dailynews.model.Category;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CategoryFilterTest {

    private final CategoryFilter filter = new CategoryFilter(ConfigLoader.loadDefault().itKeywords());

    @Test
    void 연합_산업_피드는_IT_키워드가_있는_기사만_남긴다() {
        Article chip = article(YONHAP, Category.IT, "SK하이닉스, HBM 증설 투자", "https://www.yna.co.kr/view/1", at(8, 0));
        Article ai = article(YONHAP, Category.IT, "제조업 혁신 방안 발표", "https://www.yna.co.kr/view/2", at(8, 0),
                "정부가 AI 기반 공정 자동화를 지원한다");
        Article steel = article(YONHAP, Category.IT, "철강업계 수출 부진", "https://www.yna.co.kr/view/3", at(8, 0),
                "철강 수출이 3개월 연속 줄었다");

        assertThat(filter.filter(List.of(chip, ai, steel))).extracting(Article::title)
                .containsExactly("SK하이닉스, HBM 증설 투자", "제조업 혁신 방안 발표");
    }

    @Test
    void 지정되지_않은_피드는_영향을_받지_않는다() {
        Article dongaScience = article(DONGA, Category.IT, "철강업계 수출 부진", "https://www.donga.com/news/1", at(8, 0));
        Article yonhapEconomy = article(YONHAP, Category.ECONOMY, "철강업계 수출 부진", "https://www.yna.co.kr/view/4",
                at(8, 0));

        assertThat(filter.filter(List.of(dongaScience, yonhapEconomy))).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AI 반도체", "생성형 AI가 바꾼 일상", "ai 규제 논의", "5G망 투자", "(AI)", "SK하이닉스 실적"})
    void 영문_키워드가_단어로_쓰이면_맞는다(String text) {
        assertThat(filter.matches(text)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MAIN 스트리트", "PAID 광고", "SAIL 요트", "15G 하중", "철강 수출"})
    void 영문_키워드가_다른_단어의_일부면_맞지_않는다(String text) {
        assertThat(filter.matches(text)).isFalse();
    }

    @Test
    void 키워드가_없으면_지정된_피드의_기사는_모두_제외된다() {
        CategoryFilter empty = new CategoryFilter(List.of());
        Article chip = article(YONHAP, Category.IT, "AI 반도체", "https://www.yna.co.kr/view/1", at(8, 0));

        assertThat(empty.filter(List.of(chip))).isEmpty();
    }
}
