package dailynews.clean;

import dailynews.model.Source;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 원문 URL을 표준형으로 바꾸고 안전한 링크만 남긴다(FR-3.1, FR-7.0).
 * <ul>
 *   <li>{@code &amp;}를 디코딩하고 추적 파라미터({@code utm_*}, fbclid, gclid)와 fragment를 지운다.</li>
 *   <li>스킴·호스트는 소문자로, 기본 포트와 경로 끝의 {@code /}는 지운다.</li>
 *   <li>http(s)이면서 언론사 도메인(하위 도메인 포함)인 링크만 허용한다.</li>
 * </ul>
 */
public final class UrlNormalizer {

    private static final Set<String> TRACKING_PARAMS = Set.of("fbclid", "gclid");

    public Optional<URI> normalize(URI url, Source source) {
        return normalize(url.toString(), source);
    }

    public Optional<URI> normalize(String url, Source source) {
        if (url == null) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = new URI(url.strip().replace("&amp;", "&"));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            return Optional.empty();
        }
        if (!source.ownsHost(uri.getHost())) {
            return Optional.empty();
        }

        StringBuilder normalized = new StringBuilder(scheme).append("://").append(uri.getHost().toLowerCase(Locale.ROOT));
        if (!isDefaultPort(scheme, uri.getPort())) {
            normalized.append(':').append(uri.getPort());
        }
        normalized.append(path(uri.getRawPath()));
        String query = query(uri.getRawQuery());
        if (!query.isEmpty()) {
            normalized.append('?').append(query);
        }
        return Optional.of(URI.create(normalized.toString()));
    }

    private static boolean isDefaultPort(String scheme, int port) {
        return port == -1 || (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443);
    }

    private static String path(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return "/";
        }
        String path = rawPath;
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static String query(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        return Arrays.stream(rawQuery.split("&"))
                .filter(param -> !param.isEmpty() && !isTracking(param))
                .collect(Collectors.joining("&"));
    }

    private static boolean isTracking(String param) {
        int equals = param.indexOf('=');
        String name = (equals < 0 ? param : param.substring(0, equals)).toLowerCase(Locale.ROOT);
        return name.startsWith("utm_") || TRACKING_PARAMS.contains(name);
    }
}
