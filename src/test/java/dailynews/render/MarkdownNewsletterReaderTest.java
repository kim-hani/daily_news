package dailynews.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dailynews.model.Category;
import dailynews.model.NewsItem;
import dailynews.model.Newsletter;
import dailynews.model.SummaryOrigin;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownNewsletterReaderTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();
    private final MarkdownNewsletterReader reader = new MarkdownNewsletterReader();

    @Test
    void 읽은_뉴스레터를_다시_Markdown으로_만들면_원래와_같다() {
        String markdown = renderer.render(TestNewsletters.sample());

        Newsletter parsed = reader.read(markdown, TestNewsletters.DATE);

        assertThat(renderer.render(parsed)).isEqualTo(markdown);
    }

    @Test
    void 실제_뉴스레터_Markdown도_그대로_되돌린다() throws IOException {
        String markdown;
        try (InputStream in = getClass().getResourceAsStream("/fixtures/markdown/2026-11-02.md")) {
            markdown = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        Newsletter parsed = reader.read(markdown, LocalDate.of(2026, 11, 2));

        assertThat(renderer.render(parsed)).isEqualTo(markdown);
        assertThat(parsed.stats().selected()).isEqualTo(25);
        assertThat(parsed.sections().values()).allSatisfy(items -> assertThat(items).hasSize(5));
        assertThat(parsed.sections().get(Category.POLITICS).get(2).story().representative().title())
                .isEqualTo("이 대통령 지지율 36.9%, 1.6%P&#8593;···국힘 40.2%·민주 38.3%[리얼미터]");
    }

    @Test
    void 뉴스피드에_필요한_값을_모두_읽는다() {
        Newsletter parsed = reader.read(renderer.render(TestNewsletters.sample()), TestNewsletters.DATE);

        assertThat(parsed.date()).isEqualTo(TestNewsletters.DATE);
        assertThat(parsed.generatedAt()).isEqualTo(ZonedDateTime.parse("2026-10-28T09:00+09:00[Asia/Seoul]"));
        assertThat(parsed.sourceNames()).containsExactly("연합뉴스", "동아일보", "경향신문");
        assertThat(parsed.keywords()).containsExactly("SCM", "반도체", "국회");
        assertThat(parsed.stats().collected()).isEqualTo(512);
        assertThat(parsed.stats().afterDedup()).isEqualTo(431);
        assertThat(parsed.stats().selected()).isEqualTo(5);
        assertThat(parsed.stats().failedFeeds()).containsExactly("khan/IT");
        assertThat(parsed.sections().get(Category.IT)).isEmpty();

        List<NewsItem> politics = parsed.sections().get(Category.POLITICS);
        NewsItem first = politics.get(0);
        assertThat(first.story().representative().source().name()).isEqualTo("연합뉴스");
        assertThat(first.story().representative().url()).hasToString("https://www.yna.co.kr/view/AKR1");
        assertThat(first.story().representative().publishedAt())
                .isEqualTo(ZonedDateTime.parse("2026-10-28T08:12+09:00[Asia/Seoul]"));
        assertThat(first.story().outletCount()).isEqualTo(3);
        assertThat(first.story().related()).extracting(a -> a.source().name(), a -> a.url().toString())
                .containsExactly(org.assertj.core.groups.Tuple.tuple("동아일보", "https://www.donga.com/news/Politics/article/1"),
                        org.assertj.core.groups.Tuple.tuple("경향신문", "https://www.khan.co.kr/article/202610280900001"));
        assertThat(first.summary().origin()).isEqualTo(SummaryOrigin.BODY);

        NewsItem xss = politics.get(1);
        assertThat(xss.story().representative().title()).isEqualTo(TestNewsletters.XSS_TITLE);
        assertThat(xss.story().outletCount()).isEqualTo(1);
        assertThat(xss.summary().text()).isEqualTo("요약에 <b>태그</b>와 [괄호]가 있다.");
        assertThat(xss.summary().origin()).isEqualTo(SummaryOrigin.RSS);

        assertThat(parsed.sections().get(Category.SOCIETY).get(0).summary().origin()).isEqualTo(SummaryOrigin.NONE);
    }

    @Test
    void CRLF_줄바꿈도_읽는다() {
        String markdown = renderer.render(TestNewsletters.sample());

        Newsletter parsed = reader.read(markdown.replace("\n", "\r\n"), TestNewsletters.DATE);

        assertThat(renderer.render(parsed)).isEqualTo(markdown);
    }

    @Test
    void 생성_시각보다_늦은_기사_시각은_전날로_본다() {
        String markdown = renderer.render(TestNewsletters.sample()).replace("경향신문 · 07:00", "경향신문 · 23:10");

        Newsletter parsed = reader.read(markdown, TestNewsletters.DATE);

        assertThat(parsed.sections().get(Category.SOCIETY).get(0).story().representative().publishedAt())
                .isEqualTo(ZonedDateTime.parse("2026-10-27T23:10+09:00[Asia/Seoul]"));
    }

    @Test
    void 형식이_다르면_예외를_던진다() {
        assertThatThrownBy(() -> reader.read("old", TestNewsletters.DATE)).isInstanceOf(IllegalArgumentException.class);
        String noUrl = renderer.render(TestNewsletters.sample()).replace("- 원문: https://www.yna.co.kr/view/AKR2\n", "");
        assertThatThrownBy(() -> reader.read(noUrl, TestNewsletters.DATE)).isInstanceOf(IllegalArgumentException.class);
    }
}
