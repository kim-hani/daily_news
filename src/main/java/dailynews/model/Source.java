package dailynews.model;

import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 언론사 설정.
 *
 * @param domains              기사 링크로 허용하는 도메인(하위 도메인 포함). 예: {@code donga.com}
 * @param creditPatterns       본문 앞 크레디트(예: "(서울=연합뉴스) ○○○ 기자 =")를 지우는 정규식
 * @param feeds                카테고리별 RSS 주소. Category 순서를 따른다.
 * @param itKeywordFilterFeeds IT 키워드가 맞는 기사만 쓰는 피드의 카테고리
 */
public record Source(String id, String name, List<String> domains, String bodySelector, List<Pattern> creditPatterns,
                     Map<Category, URI> feeds, Set<Category> itKeywordFilterFeeds) {

    public Source {
        domains = domains.stream().map(domain -> domain.toLowerCase(Locale.ROOT)).toList();
        creditPatterns = List.copyOf(creditPatterns);

        EnumMap<Category, URI> feedCopy = new EnumMap<>(Category.class);
        feedCopy.putAll(feeds);
        feeds = Collections.unmodifiableMap(feedCopy);

        EnumSet<Category> filterCopy = EnumSet.noneOf(Category.class);
        filterCopy.addAll(itKeywordFilterFeeds);
        itKeywordFilterFeeds = Collections.unmodifiableSet(filterCopy);
    }

    /** host가 이 언론사 도메인이거나 그 하위 도메인인지 */
    public boolean ownsHost(String host) {
        if (host == null) {
            return false;
        }
        String lower = host.toLowerCase(Locale.ROOT);
        return domains.stream().anyMatch(domain -> lower.equals(domain) || lower.endsWith("." + domain));
    }
}
