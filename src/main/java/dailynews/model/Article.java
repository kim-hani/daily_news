package dailynews.model;

import java.net.URI;
import java.time.ZonedDateTime;

/**
 * RSS에서 수집한 기사 1건.
 *
 * @param url            정규화된 원문 URL(FR-7.0)
 * @param rssDescription HTML을 걷어낸 RSS 설명문. 없으면 빈 문자열
 */
public record Article(Source source, Category category, String title, URI url,
                      ZonedDateTime publishedAt, String rssDescription) {
}
