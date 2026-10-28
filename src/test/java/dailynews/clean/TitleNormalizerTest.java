package dailynews.clean;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TitleNormalizerTest {

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = '|', value = {
            "[단독] 국회, “예산안” 통과                 | 국회, '예산안' 통과",
            "[속보]국회 예산안 통과                     | 국회   예산안 통과",
            "한미 국방장관, SCM은 내년초 개최키로(종합2보) | 한미 국방장관, SCM은 내년초 개최키로(종합)",
            "중수청, 고발사건 경찰에 넘겨(2보)           | 중수청, 고발사건 경찰에 넘겨",
            "‘반미 전과’ 진성준, 비자 거부               | \"반미 전과\" 진성준 비자 거부",
            "오세훈에 삼행시 건넨 이준석[정치를 부탁해]    | 오세훈에 삼행시 건넨 이준석",
            "【포토】 서울 첫눈                         | <속보> 서울 첫눈"})
    void 말머리_따옴표_특수문자_차이는_같은_정규형이_된다(String a, String b) {
        assertThat(TitleNormalizer.normalize(a)).isEqualTo(TitleNormalizer.normalize(b));
    }

    @Test
    void 정규형은_소문자이고_공백이_하나씩이다() {
        assertThat(TitleNormalizer.normalize("[속보]  한미 국방장관, SCM은 내년초 개최키로…\"전작권 전환 적기에\"(종합2보)"))
                .isEqualTo("한미 국방장관 scm은 내년초 개최키로 전작권 전환 적기에");
    }

    @Test
    void 대괄호가_아닌_괄호_내용은_남긴다() {
        assertThat(TitleNormalizer.normalize("국가공무원 7급(2차) 합격…평균연령 28.7세"))
                .isEqualTo("국가공무원 7급 2차 합격 평균연령 28 7세");
    }

    @Test
    void clean은_대소문자와_문장부호를_남긴다() {
        assertThat(TitleNormalizer.clean("[속보] 한미 “SCM 내년초 개최”…전작권 전환(종합)"))
                .isEqualTo("한미 SCM 내년초 개최 …전작권 전환");
    }

    @Test
    void 다른_제목은_다른_정규형이다() {
        assertThat(TitleNormalizer.normalize("국회 예산안 통과"))
                .isNotEqualTo(TitleNormalizer.normalize("국회 예산안 부결"));
    }
}
