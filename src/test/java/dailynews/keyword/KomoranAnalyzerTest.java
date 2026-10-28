package dailynews.keyword;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.keyword.KomoranAnalyzer.Noun;
import org.junit.jupiter.api.Test;

class KomoranAnalyzerTest {

    private final KomoranAnalyzer analyzer = KomoranAnalyzer.getInstance();

    @Test
    void 제목에서_명사를_나온_순서대로_뽑는다() {
        assertThat(analyzer.nouns("삼성전자 반도체 투자 확대")).extracting(Noun::text)
                .containsExactly("삼성전자", "반도체", "투자", "확대");
    }

    @Test
    void 조사와_어미는_빼고_명사만_남긴다() {
        assertThat(analyzer.nouns("국회가 내년도 예산안을 통과시켰다")).extracting(Noun::text)
                .contains("국회", "예산안")
                .doesNotContain("가", "을", "통과시켰다");
    }

    @Test
    void 영문_약어도_명사로_본다() {
        assertThat(analyzer.nouns("한미 SCM 내년초 개최")).extracting(Noun::text, Noun::tag)
                .contains(org.assertj.core.groups.Tuple.tuple("SCM", "SL"));
    }

    @Test
    void 한_글자_명사는_뺀다() {
        assertThat(analyzer.nouns("이 대통령 미 국방 장관과 첫 회담")).extracting(Noun::text)
                .allSatisfy(text -> assertThat(text.length()).isGreaterThanOrEqualTo(2));
    }

    @Test
    void 고유명사와_영문은_proper로_본다() {
        assertThat(new Noun("진성준", "NNP").isProper()).isTrue();
        assertThat(new Noun("AI", "SL").isProper()).isTrue();
        assertThat(new Noun("반도체", "NNG").isProper()).isFalse();
    }

    @Test
    void 빈_문자열은_빈_목록이고_인스턴스는_하나다() {
        assertThat(analyzer.nouns("  ")).isEmpty();
        assertThat(analyzer.nouns(null)).isEmpty();
        assertThat(KomoranAnalyzer.getInstance()).isSameAs(analyzer);
    }
}
