package dailynews.render;

import static dailynews.TestArticles.DONGA;
import static dailynews.TestArticles.KHAN;
import static dailynews.TestArticles.KST;
import static dailynews.TestArticles.YONHAP;
import static dailynews.TestArticles.article;
import static dailynews.TestArticles.at;
import static org.assertj.core.api.Assertions.assertThat;

import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.NewsItem;
import dailynews.model.Newsletter;
import dailynews.model.RunStats;
import dailynews.model.Story;
import dailynews.model.Summary;
import dailynews.model.SummaryOrigin;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PublishedHistoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    @TempDir
    Path root;

    private final MarkdownRenderer renderer = new MarkdownRenderer();
    private final PublishedHistory history = new PublishedHistory(7, new MarkdownNewsletterReader());

    @Test
    void 출력_날짜_이전_7일_파일의_원문과_같은_사안_URL만_모은다_SC1() throws IOException {
        for (LocalDate day = LocalDate.of(2026, 10, 27); !day.isAfter(LocalDate.of(2026, 11, 5)); day = day.plusDays(1)) {
            write(day);
        }

        Set<URI> urls = history.urlsBefore(paths(), TODAY);

        Set<URI> expected = new HashSet<>();
        for (LocalDate day = LocalDate.of(2026, 10, 28); day.isBefore(TODAY); day = day.plusDays(1)) {
            expected.addAll(urlsOf(day));
        }
        assertThat(urls).hasSize(7 * 4).isEqualTo(expected);
        assertThat(urls).doesNotContainAnyElementsOf(urlsOf(LocalDate.of(2026, 10, 27)))
                .doesNotContainAnyElementsOf(urlsOf(TODAY))
                .doesNotContainAnyElementsOf(urlsOf(LocalDate.of(2026, 11, 5)));
    }

    @Test
    void 같은_사안_링크의_괄호_URL도_원래대로_모은다() throws IOException {
        write(TODAY.minusDays(1));

        assertThat(history.urlsBefore(paths(), TODAY))
                .contains(URI.create("https://www.donga.com/news/Politics/article/(20261103)/1"));
    }

    @Test
    void historyDays가_0이면_파일이_있어도_빈_집합이다() throws IOException {
        write(TODAY.minusDays(1));

        assertThat(new PublishedHistory(0, new MarkdownNewsletterReader()).urlsBefore(paths(), TODAY)).isEmpty();
    }

    @Test
    void 깨진_파일은_건너뛰고_나머지_URL은_모은다_SC2() throws IOException {
        write(TODAY.minusDays(1));
        LocalDate brokenFormat = TODAY.minusDays(2);
        LocalDate brokenEncoding = TODAY.minusDays(3);
        Files.createDirectories(paths().markdown(brokenFormat).getParent());
        Files.writeString(paths().markdown(brokenFormat), renderer.render(newsletter(brokenFormat)).replace("## 💻 IT", "## IT"));
        Files.write(paths().markdown(brokenEncoding), new byte[] {(byte) 0xFF, (byte) 0xFE, 0x00});

        assertThat(history.urlsBefore(paths(), TODAY)).isEqualTo(urlsOf(TODAY.minusDays(1)));
    }

    @Test
    void news_폴더가_없으면_빈_집합이다() {
        assertThat(history.urlsBefore(paths(), TODAY)).isEmpty();
    }

    @Test
    void CRLF_줄바꿈_파일도_읽는다() throws IOException {
        LocalDate day = TODAY.minusDays(1);
        Path file = paths().markdown(day);
        Files.createDirectories(file.getParent());
        Files.writeString(file, renderer.render(newsletter(day)).replace("\n", "\r\n"), StandardCharsets.UTF_8);

        assertThat(history.urlsBefore(paths(), TODAY)).isEqualTo(urlsOf(day));
    }

    private OutputPaths paths() {
        return new OutputPaths(root);
    }

    private void write(LocalDate day) throws IOException {
        renderer.write(newsletter(day), paths());
    }

    /** 날짜마다 URL이 다른 뉴스레터. 정치 1건(같은 사안 2건, 하나는 괄호 URL)과 경제 1건, 나머지는 빈 카테고리 */
    private static Newsletter newsletter(LocalDate day) {
        String id = day.toString().replace("-", "");
        Article yonhap = article(YONHAP, Category.POLITICS, "정치 " + id, "https://www.yna.co.kr/view/AKR" + id, at(8, 0));
        Article donga = article(DONGA, Category.POLITICS, "정치 " + id,
                "https://www.donga.com/news/Politics/article/(" + id + ")/1", at(9, 0));
        Article khan = article(KHAN, Category.POLITICS, "정치 " + id, "https://www.khan.co.kr/article/" + id + "0900001",
                at(9, 30));
        Article economy = article(YONHAP, Category.ECONOMY, "경제 " + id, "https://www.yna.co.kr/view/ECO" + id, at(7, 0));
        Map<Category, List<NewsItem>> sections = Map.of(
                Category.POLITICS, List.of(new NewsItem(new Story(Category.POLITICS, yonhap, List.of(donga, khan), 3, 30),
                        new Summary("요약", SummaryOrigin.BODY))),
                Category.ECONOMY, List.of(new NewsItem(new Story(Category.ECONOMY, economy, List.of(), 1, 10),
                        new Summary("", SummaryOrigin.NONE))));
        return new Newsletter(day, day.atTime(LocalTime.of(12, 0)).atZone(KST), List.of("연합뉴스", "동아일보", "경향신문"),
                sections, List.of("정치"), new RunStats(100, 90, 2, List.of(), 1, 1));
    }

    private static Set<URI> urlsOf(LocalDate day) {
        String id = day.toString().replace("-", "");
        return Set.of(URI.create("https://www.yna.co.kr/view/AKR" + id),
                URI.create("https://www.donga.com/news/Politics/article/(" + id + ")/1"),
                URI.create("https://www.khan.co.kr/article/" + id + "0900001"),
                URI.create("https://www.yna.co.kr/view/ECO" + id));
    }
}
