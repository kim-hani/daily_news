package dailynews.collect;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.Source;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RSS 2.0 문서를 {@link Article} 목록으로 바꾼다(FR-2.2).
 * 날짜는 {@code pubDate}와 {@code dc:date}를 모두 읽고 KST로 바꾼다. 제목·링크·날짜가 없거나 잘못된 item은 건너뛴다.
 * 링크는 원본 그대로 두고, 정규화는 정제 단계에서 한다.
 */
public final class RssParser {

    private static final Logger log = LoggerFactory.getLogger(RssParser.class);
    private static final Pattern WHITESPACE = Pattern.compile("(?U)\\s+");

    private final ZoneId zone;

    public RssParser() {
        this(ZoneId.of("Asia/Seoul"));
    }

    public RssParser(ZoneId zone) {
        this.zone = zone;
    }

    /**
     * @param xml RSS 응답 본문. 문자 인코딩은 XML 선언과 BOM으로 판단한다.
     * @throws RssParseException 문서 전체를 해석할 수 없을 때
     */
    public List<Article> parse(byte[] xml, Source source, Category category) throws RssParseException {
        SyndFeed feed = read(xml);
        List<Article> articles = new ArrayList<>();
        for (SyndEntry entry : feed.getEntries()) {
            Article article = toArticle(entry, source, category);
            if (article != null) {
                articles.add(article);
            }
        }
        return articles;
    }

    private static SyndFeed read(byte[] xml) throws RssParseException {
        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(xml))) {
            return new SyndFeedInput().build(reader);
        } catch (FeedException | IOException | IllegalArgumentException e) {
            throw new RssParseException("RSS를 해석할 수 없습니다: " + e.getMessage(), e);
        }
    }

    private Article toArticle(SyndEntry entry, Source source, Category category) {
        String title = clean(entry.getTitle());
        URI url = toHttpUri(entry.getLink());
        Date published = entry.getPublishedDate() != null ? entry.getPublishedDate() : entry.getUpdatedDate();
        if (title.isEmpty() || url == null || published == null) {
            log.warn("{}/{} item을 건너뜁니다. 제목·링크·날짜 중 빠지거나 잘못된 값이 있습니다: title={}, link={}",
                    source.id(), category, title, entry.getLink());
            return null;
        }
        ZonedDateTime publishedAt = ZonedDateTime.ofInstant(published.toInstant(), zone);
        return new Article(source, category, title, url, publishedAt, description(entry.getDescription()));
    }

    /** HTML 태그(img 등)를 걷어내고 텍스트만 남긴다. */
    private static String description(SyndContent content) {
        if (content == null || content.getValue() == null) {
            return "";
        }
        return clean(Jsoup.parse(content.getValue()).text());
    }

    private static URI toHttpUri(String link) {
        // 경로에 공백이 그대로 든 링크가 있다(예: 동아일보 /news/K-TECH Global Leaders/…).
        String text = clean(link).replace(" ", "%20");
        if (text.isEmpty()) {
            return null;
        }
        try {
            URI uri = new URI(text);
            boolean http = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            return http && uri.getHost() != null ? uri : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /** 연속 공백(줄바꿈, NBSP 포함)을 하나로 줄이고 앞뒤 공백을 지운다. */
    private static String clean(String text) {
        return text == null ? "" : WHITESPACE.matcher(text).replaceAll(" ").strip();
    }
}
