package dailynews;

import dailynews.config.ConfigLoader;
import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.Source;
import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * 테스트용 기사와 언론사(기본 설정의 연합·동아·경향)를 만든다.
 */
public final class TestArticles {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    public static final List<Source> SOURCES = ConfigLoader.loadDefault().sources();
    public static final Source YONHAP = SOURCES.get(0);
    public static final Source DONGA = SOURCES.get(1);
    public static final Source KHAN = SOURCES.get(2);

    private TestArticles() {
    }

    /** 2026-10-28 hour:minute KST */
    public static ZonedDateTime at(int hour, int minute) {
        return ZonedDateTime.of(2026, 10, 28, hour, minute, 0, 0, KST);
    }

    public static Article article(Source source, Category category, String title, String url, ZonedDateTime at) {
        return article(source, category, title, url, at, "");
    }

    public static Article article(Source source, Category category, String title, String url, ZonedDateTime at,
                                  String description) {
        return new Article(source, category, title, URI.create(url), at, description);
    }
}
