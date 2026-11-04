package dailynews.render;

import dailynews.model.Article;
import dailynews.model.NewsItem;
import dailynews.model.Newsletter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 지난 뉴스레터에 실린 기사 URL을 모은다(history-dedup spec FR-1). 정제 단계가 이 URL의 기사를 후보에서 뺀다.
 * <ul>
 *   <li>출력 날짜 D 기준 {@code D − historyDays} ~ {@code D − 1}의 {@code news/YYYY/MM/YYYY-MM-DD.md}만 읽는다.
 *       D 당일 파일은 읽지 않으므로 같은 날 다시 실행해도 결과가 같다(FR-7.4).</li>
 *   <li>모으는 URL은 원문 URL과 같은 사안 URL이다. 둘 다 정제 단계에서 정규화한 URL이 그대로 저장되어 있다.</li>
 *   <li>파일이 없으면 건너뛰고, 읽거나 해석하지 못한 파일은 경고를 남기고 건너뛴다. 예외를 던지지 않는다.</li>
 *   <li>{@code historyDays}가 0이면 아무 파일도 읽지 않는다.</li>
 * </ul>
 */
public final class PublishedHistory {

    private static final Logger log = LoggerFactory.getLogger(PublishedHistory.class);

    private final int historyDays;
    private final MarkdownNewsletterReader reader;

    public PublishedHistory(int historyDays, MarkdownNewsletterReader reader) {
        this.historyDays = historyDays;
        this.reader = reader;
    }

    /** {@code date} 이전 {@code historyDays}일 뉴스레터에 실린 기사 URL */
    public Set<URI> urlsBefore(OutputPaths paths, LocalDate date) {
        if (historyDays == 0) {
            log.info("지난 뉴스레터: historyDays가 0이라 비교하지 않습니다");
            return Set.of();
        }
        LocalDate from = date.minusDays(historyDays);
        Set<URI> urls = new HashSet<>();
        int read = 0;
        int skipped = 0;
        for (LocalDate day = from; day.isBefore(date); day = day.plusDays(1)) {
            Path file = paths.markdown(day);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                Newsletter newsletter = reader.read(Files.readString(file, StandardCharsets.UTF_8), day);
                newsletter.sections().values().stream().flatMap(List::stream).forEach(item -> addUrls(item, urls));
                read++;
            } catch (IOException | IllegalArgumentException | DateTimeException e) {
                log.warn("지난 뉴스레터를 읽지 못해 건너뜁니다: {} ({})", paths.root().relativize(file), e.toString());
                skipped++;
            }
        }
        log.info("지난 뉴스레터: {} ~ {} 중 {}개 파일에서 URL {}개 (건너뜀 {})", from, date.minusDays(1), read,
                urls.size(), skipped);
        return Set.copyOf(urls);
    }

    private static void addUrls(NewsItem item, Set<URI> urls) {
        urls.add(item.story().representative().url());
        item.story().related().stream().map(Article::url).forEach(urls::add);
    }
}
