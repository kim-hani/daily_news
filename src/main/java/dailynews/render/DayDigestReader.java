package dailynews.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 보관된 날짜별 뉴스피드에서 캘린더 칸에 보여 줄 선정 수와 키워드를 읽는다(plan §4.4).
 * <ol>
 *   <li>새 페이지: {@code meta[name=dn:selected]}, {@code meta[name=dn:keyword]}</li>
 *   <li>옛 페이지(전환 기간, plan §3.3): 키워드는 {@code ul.chips li}, 선정 수는 {@code article.card} 개수</li>
 *   <li>읽지 못하면 선정 0건·키워드 없음으로 두고 경고만 남긴다. 캘린더 링크는 그대로 둔다.</li>
 * </ol>
 */
public final class DayDigestReader {

    private static final Logger log = LoggerFactory.getLogger(DayDigestReader.class);

    public DayDigest read(Path page, LocalDate date) {
        try {
            Document document = Jsoup.parse(Files.readString(page, StandardCharsets.UTF_8));
            Element selected = document.selectFirst("meta[name=dn:selected]");
            if (selected != null) {
                List<String> keywords = document.select("meta[name=dn:keyword]").eachAttr("content");
                return new DayDigest(date, Integer.parseInt(selected.attr("content").trim()), keywords);
            }
            return new DayDigest(date, document.select("article.card").size(), document.select("ul.chips li").eachText());
        } catch (IOException | RuntimeException e) {
            log.warn("날짜별 페이지를 읽지 못해 캘린더 칸 정보 없이 링크만 둡니다: {} ({})", page.getFileName(), e.toString());
            return new DayDigest(date, 0, List.of());
        }
    }
}
