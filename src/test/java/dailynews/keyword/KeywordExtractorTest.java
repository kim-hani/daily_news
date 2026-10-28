package dailynews.keyword;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.keyword.KomoranAnalyzer.Noun;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class KeywordExtractorTest {

    /** 제목을 공백으로 나누고, 이름에 * 가 붙은 단어는 고유명사로 보는 가짜 분석기 */
    private static final Function<String, List<Noun>> FAKE = title -> Arrays.stream(title.split(" "))
            .map(word -> word.endsWith("*") ? new Noun(word.substring(0, word.length() - 1), "NNP") : new Noun(word, "NNG"))
            .toList();

    @Test
    void 많이_나온_순으로_고르고_같으면_고유명사_다음_가나다순이다() {
        List<String> titles = List.of(
                "반도체 수출 서울*",
                "반도체 투자 서울*",
                "반도체 예산 국회",
                "투자 국회 다리",
                "가방 나무");

        assertThat(new KeywordExtractor(FAKE, List.of()).extract(titles, 10))
                .containsExactly("반도체", "서울", "국회", "투자", "가방", "나무", "다리", "수출", "예산");
    }

    @Test
    void 불용어는_대소문자를_가리지_않고_뺀다() {
        List<String> titles = List.of("기자 반도체", "기자 반도체", "기자 AI*", "ai* 오늘");

        assertThat(new KeywordExtractor(FAKE, List.of("기자", "AI", "오늘")).extract(titles, 10))
                .containsExactly("반도체");
    }

    @Test
    void 한_제목에서_여러_번_나와도_한_번만_센다() {
        List<String> titles = List.of("위원장 위원장 위원장 비자", "비자 거부", "비자 발급");

        assertThat(new KeywordExtractor(FAKE, List.of()).extract(titles, 2)).containsExactly("비자", "거부");
    }

    @Test
    void 최대_limit개까지만_고른다() {
        List<String> titles = IntStream.rangeClosed(1, 15).mapToObj(i -> "단어" + (char) ('가' + i)).toList();

        assertThat(new KeywordExtractor(FAKE, List.of()).extract(titles, 10)).hasSize(10);
    }

    @Test
    void 실제_KOMORAN으로_제목의_대표_명사를_고른다() {
        List<String> titles = List.of(
                "[속보] 삼성전자 반도체 투자 확대",
                "삼성전자, 반도체 공장 착공…기자 간담회",
                "SK하이닉스 반도체 수출 증가",
                "국회 예산안 통과");

        List<String> keywords = new KeywordExtractor(KomoranAnalyzer.getInstance()::nouns, List.of("기자"))
                .extract(titles, 10);

        assertThat(keywords.getFirst()).isEqualTo("반도체");
        assertThat(keywords).contains("삼성전자", "예산안").doesNotContain("기자", "속보").hasSizeLessThanOrEqualTo(10);
    }
}
