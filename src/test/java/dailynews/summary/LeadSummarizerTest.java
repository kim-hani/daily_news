package dailynews.summary;

import static dailynews.TestArticles.DONGA;
import static dailynews.TestArticles.KHAN;
import static dailynews.TestArticles.YONHAP;
import static org.assertj.core.api.Assertions.assertThat;

import dailynews.Fixtures;
import dailynews.model.Source;
import dailynews.model.Summary;
import dailynews.model.SummaryOrigin;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LeadSummarizerTest {

    private final LeadSummarizer summarizer = new LeadSummarizer(new SentenceSplitter());
    private final BodyExtractor extractor = new BodyExtractor();

    static Stream<Arguments> fixtures() {
        return Stream.of(Arguments.of(YONHAP, 1), Arguments.of(YONHAP, 2), Arguments.of(DONGA, 1),
                Arguments.of(DONGA, 2), Arguments.of(KHAN, 1), Arguments.of(KHAN, 2));
    }

    @ParameterizedTest(name = "{0} article-{1}")
    @MethodSource("fixtures")
    void fixture_기사_요약은_본문_기반이고_200자_이하이며_크레디트가_없다(Source source, int n) {
        Summary summary = summarizer.summarize(body(source, n), "RSS 설명", source);

        assertThat(summary.origin()).isEqualTo(SummaryOrigin.BODY);
        assertThat(summary.text().codePointCount(0, summary.text().length())).isBetween(1, 200);
        assertThat(summary.text()).doesNotContain("=연합뉴스)", "기자 =", "@", "저작권자", "무단 전재");
    }

    @ParameterizedTest(name = "{0} article-{1}")
    @MethodSource("fixtures")
    void 요약의_각_문장은_정제한_본문의_부분_문자열이다_SC5(Source source, int n) {
        List<String> cleaned = summarizer.cleanParagraphs(body(source, n), source);

        assertThat(summarizer.leadSentences(cleaned, false)).isNotEmpty().allSatisfy(sentence ->
                assertThat(cleaned).anyMatch(paragraph -> paragraph.contains(sentence.replaceAll("…$", ""))));
    }

    @Test
    void 연합뉴스_요약은_크레디트_뒤_첫_문장으로_시작한다() {
        Summary summary = summarizer.summarize(body(YONHAP, 1), "", YONHAP);

        assertThat(summary.text()).startsWith("한미 국방장관이 올해 12월 워싱턴에서 양자 회담을 하고");
    }

    @Test
    void 목표_120자에_이를_때까지_최대_3문장을_모은다() {
        List<String> paragraphs = List.of("첫째 문장은 조금 더 깁니다.", "둘째 문장도 조금 더 깁니다. 셋째 문장도 조금 더 깁니다.",
                "넷째 문장은 쓰이지 않습니다.");

        assertThat(summarizer.summarize(paragraphs, "", DONGA).text())
                .isEqualTo("첫째 문장은 조금 더 깁니다. 둘째 문장도 조금 더 깁니다. 셋째 문장도 조금 더 깁니다.");
    }

    @Test
    void 최대_200자를_넘으면_마지막_문장을_뺀다() {
        String first = "가".repeat(100) + "다.";
        String second = "나".repeat(110) + "다.";

        assertThat(summarizer.summarize(List.of(first + " " + second), "", DONGA).text()).isEqualTo(first);
    }

    @Test
    void 첫_문장이_200자를_넘으면_잘라서_말줄임표를_붙인다() {
        String longSentence = "가".repeat(250) + "다.";

        String text = summarizer.summarize(List.of(longSentence), "", DONGA).text();

        assertThat(text).hasSize(200).endsWith("…").startsWith("가".repeat(199));
    }

    @Test
    void 크레디트_이메일_언론사_머리말을_지우고_관련기사와_저작권_문단은_버린다() {
        List<String> paragraphs = List.of(
                "(서울=뉴스1) 홍길동 기자 = 국회가 28일 본회의를 열어 내년도 예산안을 처리했다.",
                "[동아일보] 여야는 막판 협상 끝에 합의했다. hong@donga.com",
                "▶ 관련기사 예산안 처리 일지",
                "ⓒ 동아일보 & donga.com, 무단 전재 및 재배포 금지");

        assertThat(summarizer.cleanParagraphs(paragraphs, DONGA))
                .containsExactly("국회가 28일 본회의를 열어 내년도 예산안을 처리했다.", "여야는 막판 협상 끝에 합의했다.");
    }

    @Test
    void 본문이_40자_미만이면_RSS_설명문으로_요약한다() {
        Summary summary = summarizer.summarize(List.of("짧은 본문."),
                "(서울=연합뉴스) 김효정 기자 = 한미 국방장관이 회담했다. 양국은 내년 초 SCM을 열기로 ...", YONHAP);

        assertThat(summary.origin()).isEqualTo(SummaryOrigin.RSS);
        assertThat(summary.text()).isEqualTo("한미 국방장관이 회담했다.");
    }

    @Test
    void 글자가_10자_미만인_RSS_설명문은_요약으로_쓰지_않는다() {
        // 연합뉴스 속보: 본문 문단이 비어 있고 RSS 설명문은 "(" 한 글자다(2026-10-28 실제 데이터).
        Summary summary = summarizer.summarize(List.of(), "(", YONHAP);

        assertThat(summary.origin()).isEqualTo(SummaryOrigin.NONE);
    }

    @Test
    void 본문과_RSS가_모두_없으면_요약_없음이다() {
        Summary summary = summarizer.summarize(List.of(), "", KHAN);

        assertThat(summary.origin()).isEqualTo(SummaryOrigin.NONE);
        assertThat(summary.text()).isEmpty();
    }

    private List<String> body(Source source, int n) {
        return extractor.extract(Fixtures.bytes("/fixtures/" + source.id() + "/article-" + n + ".html"),
                "text/html; charset=UTF-8", URI.create("https://example.com/"), source);
    }
}
