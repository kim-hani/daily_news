package dailynews.http;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * robots.txt가 허용하는 URL인지 판단한다(NFR-3). 호스트별 robots.txt는 한 번만 받아 캐시한다.
 * <ul>
 *   <li>2xx: {@code User-agent: *} 그룹 규칙을 따른다.</li>
 *   <li>4xx: robots.txt가 없는 것으로 보고 전부 허용한다.</li>
 *   <li>5xx, 연결 실패: 판단할 수 없으니 보수적으로 전부 차단한다.</li>
 * </ul>
 */
public final class RobotsPolicy {

    private static final Logger log = LoggerFactory.getLogger(RobotsPolicy.class);

    private final HttpFetcher fetcher;
    private final Map<String, RobotsRules> cache = new HashMap<>();

    public RobotsPolicy(HttpFetcher fetcher) {
        this.fetcher = fetcher;
    }

    public boolean isAllowed(URI uri) {
        String pathAndQuery = pathAndQuery(uri);
        if (pathAndQuery.equals("/robots.txt")) {
            return true;
        }
        return cache.computeIfAbsent(origin(uri), this::load).isAllowed(pathAndQuery);
    }

    private RobotsRules load(String origin) {
        URI robotsUri = URI.create(origin + "/robots.txt");
        try {
            FetchResult result = fetcher.fetch(robotsUri);
            if (result.isSuccess()) {
                return RobotsRules.parse(result.bodyAsUtf8());
            }
            if (result.status() >= 400 && result.status() < 500) {
                log.info("{} 응답 {}: robots.txt가 없어 전부 허용합니다", robotsUri, result.status());
                return RobotsRules.allowAll();
            }
            log.warn("{} 응답 {}: 판단할 수 없어 이 호스트 요청을 모두 막습니다", robotsUri, result.status());
        } catch (IOException e) {
            log.warn("{} 요청 실패({}): 판단할 수 없어 이 호스트 요청을 모두 막습니다", robotsUri, e.toString());
        }
        return RobotsRules.disallowAll();
    }

    private static String origin(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return scheme + "://" + host + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
    }

    private static String pathAndQuery(URI uri) {
        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
    }
}
