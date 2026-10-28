package dailynews.summary;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SentenceSplitterTest {

    private final SentenceSplitter splitter = new SentenceSplitter();

    @Test
    void 다_요_물음표_느낌표_뒤_공백에서_나눈다() {
        assertThat(splitter.split("국회가 예산안을 통과시켰다. 정말인가요? 놀랍습니다! 마지막 문장"))
                .containsExactly("국회가 예산안을 통과시켰다.", "정말인가요?", "놀랍습니다!", "마지막 문장");
    }

    @Test
    void 따옴표_안에서는_나누지_않는다() {
        assertThat(splitter.split("한 원내대표는 “진상규명이 끝난 것이 아니다. 끝까지 밝히겠다”며 강조했다. 이어 질의가 있었다."))
                .containsExactly("한 원내대표는 “진상규명이 끝난 것이 아니다. 끝까지 밝히겠다”며 강조했다.", "이어 질의가 있었다.");
        assertThat(splitter.split("그는 \"좋다. 아주 좋다\"고 말했다. 다음 문장."))
                .containsExactly("그는 \"좋다. 아주 좋다\"고 말했다.", "다음 문장.");
    }

    @Test
    void 따옴표가_닫히며_문장이_끝나면_닫는_따옴표까지_한_문장이다() {
        assertThat(splitter.split("그는 이렇게 말했다. “내일 다시 오겠다.” 회의는 끝났다."))
                .containsExactly("그는 이렇게 말했다.", "“내일 다시 오겠다.”", "회의는 끝났다.");
    }

    @Test
    void 소수점과_날짜는_나누지_않는다() {
        assertThat(splitter.split("평균연령은 28.7세로 집계됐다. 2026.10.28 기준이다."))
                .containsExactly("평균연령은 28.7세로 집계됐다.", "2026.10.28 기준이다.");
    }

    @Test
    void 영문_약어와_이니셜에서는_나누지_않는다() {
        assertThat(splitter.split("U.S. 정부와 Mr. Kim이 만났다. J. Smith 대변인이 밝혔다."))
                .containsExactly("U.S. 정부와 Mr. Kim이 만났다.", "J. Smith 대변인이 밝혔다.");
    }

    @Test
    void 작은따옴표_인용과_영문_아포스트로피를_구분한다() {
        assertThat(splitter.split("이른바 '관계성 범죄. 대책' 주문이다. Korea's plan."))
                .containsExactly("이른바 '관계성 범죄. 대책' 주문이다.", "Korea's plan.");
    }

    @Test
    void 빈_문자열은_빈_목록이다() {
        assertThat(splitter.split("   ")).isEmpty();
    }
}
