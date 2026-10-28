package dailynews.select;

import static org.assertj.core.api.Assertions.assertThat;

import dailynews.clean.TitleNormalizer;
import dailynews.keyword.KomoranAnalyzer;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 제목 쌍은 fixture RSS(2026-10-28 정치)의 실제 제목이다.
 */
class TitleSimilarityTest {

    private static final double THRESHOLD = 0.4;

    private final TitleSimilarity similarity = new TitleSimilarity(KomoranAnalyzer.getInstance());

    @ParameterizedTest(name = "{0} ~ {1}")
    @CsvSource(delimiter = '|', value = {
            "李대통령 \"서울시 등 다문화가족 우대제…폐지 필요한지 검토\"(종합) | 이 대통령 “다문화 가족 우대까지 할 필요성 큰지 검토”",
            "李 “다문화 우대제도 필요성 큰가…폐지 필요한지 검토하겠다”        | 이 대통령 “다문화 가족 우대까지 할 필요성 큰지 검토”",
            "‘반미 전과’ 與진성준 국방위원장, 美비자 발급 거부당해              | 미국, 이번엔 진성준 국방위원장 비자 발급 ‘거부’···진 위원장 “강력 유감”",
            "진성준 \"美, 태평양사령부 방문 위한 비자발급 거부…강력 유감\"       | 미국, 이번엔 진성준 국방위원장 비자 발급 ‘거부’···진 위원장 “강력 유감”",
            "한미 국방장관, SCM은 내년초 개최키로…\"전작권 전환 적기에\"(종합) | 강신철, 미 헤그세스와 첫 회담 “SCM 내년초 개최…전작권 전환 적기에 이뤄지도록 협력”"})
    void 같은_사안의_다른_언론사_제목은_임계값_이상이다(String a, String b) {
        assertThat(similarity(a, b)).isGreaterThanOrEqualTo(THRESHOLD);
    }

    @ParameterizedTest(name = "{0} ~ {1}")
    @CsvSource(delimiter = '|', value = {
            "법무부, 제81주년 교정의날 기념식…45명 포상                     | 금융권 노렸나…北 IP 통한 해킹 시도, 하루에만 440만회",
            "李대통령 \"서울시 등 다문화가족 우대제…폐지 필요한지 검토\"(종합) | 李대통령, 與중진들에 \"집권여당 역할 중요…통합에 힘 보태달라\"",
            "진성준 \"美, 태평양사령부 방문 위한 비자발급 거부…강력 유감\"       | 한·미 “안보협의회 내년 초로 연기”",
            "국가공무원 7급 2차시험 820명 합격…평균연령 28.7세              | [단독]3호선 전철역 덥다 했더니…34곳중 20곳 에어컨 없었다",
            "이 대통령 “다문화 가족 우대까지 할 필요성 큰지 검토”             | 비판 언론에  “까대기식 보도”…논란 키우는 ‘대통령 SNS’"})
    void 다른_사안의_제목은_임계값_미만이다(String a, String b) {
        assertThat(similarity(a, b)).isLessThan(THRESHOLD);
    }

    @Test
    void 같은_제목은_1이다() {
        assertThat(similarity.similarity("국회 예산안 통과", "국회 예산안 통과")).isEqualTo(1.0);
    }

    @Test
    void Jaccard는_교집합을_합집합으로_나누고_빈_집합이면_0이다() {
        assertThat(TitleSimilarity.jaccard(Set.of("a", "b", "c"), Set.of("b", "c", "d"))).isEqualTo(0.5);
        assertThat(TitleSimilarity.jaccard(Set.of(), Set.of("a"))).isZero();
    }

    @Test
    void 문자_2gram은_공백을_빼고_만든다() {
        assertThat(similarity.features("국회 예산").bigrams()).containsExactlyInAnyOrder("국회", "회예", "예산");
    }

    private double similarity(String a, String b) {
        return similarity.similarity(TitleNormalizer.normalize(a), TitleNormalizer.normalize(b));
    }
}
