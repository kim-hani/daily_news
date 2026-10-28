package dailynews.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dailynews.model.Category;
import dailynews.model.Source;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * sources.yml을 읽고 검증해서 {@link AppConfig}로 만든다.
 */
public final class ConfigLoader {

    static final String DEFAULT_RESOURCE = "sources.yml";

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final Pattern DOMAIN = Pattern.compile("(?i)[a-z0-9-]+(\\.[a-z0-9-]+)+");

    private ConfigLoader() {
    }

    /** classpath의 기본 sources.yml을 읽는다. */
    public static AppConfig loadDefault() {
        String origin = "classpath:" + DEFAULT_RESOURCE;
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new ConfigException(origin + ": 파일이 없습니다");
            }
            return read(in, origin);
        } catch (IOException e) {
            throw new ConfigException(origin + ": 파일을 읽을 수 없습니다", e);
        }
    }

    /** 지정한 경로의 설정 파일을 읽는다(--config). */
    public static AppConfig load(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return read(in, path.toString());
        } catch (NoSuchFileException e) {
            throw new ConfigException(path + ": 파일이 없습니다", e);
        } catch (IOException e) {
            throw new ConfigException(path + ": 파일을 읽을 수 없습니다", e);
        }
    }

    private static AppConfig read(InputStream in, String origin) throws IOException {
        String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        RawConfig raw;
        try {
            raw = yaml.isBlank() ? null : YAML.readValue(yaml, RawConfig.class);
        } catch (JsonProcessingException e) {
            throw new ConfigException(origin + ": YAML을 해석할 수 없습니다 - " + e.getOriginalMessage(), e);
        }
        if (raw == null) {
            throw new ConfigException(origin + ": 설정이 비어 있습니다");
        }
        try {
            return toAppConfig(raw);
        } catch (ConfigException e) {
            throw new ConfigException(origin + ": " + e.getMessage(), e.getCause());
        }
    }

    private static AppConfig toAppConfig(RawConfig raw) {
        String userAgent = requireText(raw.userAgent(), "userAgent");
        int requestDelayMillis = requireAtLeast(raw.requestDelayMillis(), 0, "requestDelayMillis");
        int timeoutSeconds = requireAtLeast(raw.timeoutSeconds(), 1, "timeoutSeconds");
        int windowHours = requireAtLeast(raw.windowHours(), 1, "windowHours");
        int perCategory = requireAtLeast(raw.perCategory(), 1, "perCategory");
        double clusterThreshold = requireThreshold(raw.clusterThreshold(), "clusterThreshold");
        AppConfig.Retention retention = toRetention(raw.retention());
        List<Source> sources = toSources(raw.sources());
        List<String> itKeywords = textList(raw.itKeywords(), "itKeywords");
        List<String> stopwords = textList(raw.stopwords(), "stopwords");
        return new AppConfig(userAgent, requestDelayMillis, timeoutSeconds, windowHours, perCategory,
                clusterThreshold, retention, sources, itKeywords, stopwords);
    }

    private static AppConfig.Retention toRetention(RawRetention raw) {
        if (raw == null) {
            throw missing("retention");
        }
        return new AppConfig.Retention(
                requireAtLeast(raw.siteArchiveDays(), 1, "retention.siteArchiveDays"),
                requireAtLeast(raw.newsDays(), 0, "retention.newsDays"));
    }

    private static List<Source> toSources(List<SourceConfig> raws) {
        if (raws == null || raws.isEmpty()) {
            throw new ConfigException("sources: 언론사가 하나 이상 있어야 합니다");
        }
        Set<String> ids = new HashSet<>();
        List<Source> sources = new ArrayList<>();
        for (int i = 0; i < raws.size(); i++) {
            Source source = toSource(raws.get(i), i);
            if (!ids.add(source.id())) {
                throw new ConfigException("sources[" + i + "].id: 중복된 언론사 id입니다: " + source.id());
            }
            sources.add(source);
        }
        return sources;
    }

    private static Source toSource(SourceConfig raw, int index) {
        if (raw == null) {
            throw missing("sources[" + index + "]");
        }
        String id = requireText(raw.id(), "sources[" + index + "].id");
        String path = "sources[" + id + "]";
        String name = requireText(raw.name(), path + ".name");
        List<String> domains = toDomains(raw.domains(), path + ".domains");
        String bodySelector = requireText(raw.bodySelector(), path + ".bodySelector");
        List<Pattern> creditPatterns = compilePatterns(raw.creditPatterns(), path + ".creditPatterns");
        Map<Category, URI> feeds = toFeeds(raw.feeds(), path + ".feeds");
        Set<Category> itKeywordFilterFeeds = toFilterFeeds(raw.itKeywordFilterFeeds(), feeds.keySet(),
                path + ".itKeywordFilterFeeds");
        Source source = new Source(id, name, domains, bodySelector, creditPatterns, feeds, itKeywordFilterFeeds);
        source.feeds().forEach((category, feed) -> {
            if (!source.ownsHost(feed.getHost())) {
                throw new ConfigException(path + ".feeds." + category + ": 피드 호스트가 domains에 속하지 않습니다: "
                        + feed.getHost());
            }
        });
        return source;
    }

    private static List<String> toDomains(List<String> raw, String path) {
        List<String> domains = textList(raw, path);
        if (domains.isEmpty()) {
            throw new ConfigException(path + ": 도메인이 하나 이상 있어야 합니다");
        }
        for (int i = 0; i < domains.size(); i++) {
            if (!DOMAIN.matcher(domains.get(i)).matches()) {
                throw new ConfigException(path + "[" + i + "]: 도메인 형식이 아닙니다: " + domains.get(i));
            }
        }
        return domains;
    }

    private static Map<Category, URI> toFeeds(Map<Category, String> raw, String path) {
        if (raw == null || raw.isEmpty()) {
            throw new ConfigException(path + ": 피드가 하나 이상 있어야 합니다");
        }
        Map<Category, URI> feeds = new EnumMap<>(Category.class);
        raw.forEach((category, url) -> feeds.put(category, toHttpUri(url, path + "." + category)));
        return feeds;
    }

    private static URI toHttpUri(String value, String path) {
        String text = requireText(value, path);
        URI uri;
        try {
            uri = new URI(text);
        } catch (URISyntaxException e) {
            throw new ConfigException(path + ": URL 형식이 올바르지 않습니다: " + text, e);
        }
        String scheme = uri.getScheme();
        boolean http = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        if (!http || uri.getHost() == null) {
            throw new ConfigException(path + ": http(s) URL만 쓸 수 있습니다: " + text);
        }
        return uri;
    }

    private static Set<Category> toFilterFeeds(List<Category> raw, Set<Category> feedCategories, String path) {
        Set<Category> filterFeeds = EnumSet.noneOf(Category.class);
        if (raw == null) {
            return filterFeeds;
        }
        for (Category category : raw) {
            if (category == null || !feedCategories.contains(category)) {
                throw new ConfigException(path + ": feeds에 없는 카테고리입니다: " + category);
            }
            filterFeeds.add(category);
        }
        return filterFeeds;
    }

    private static List<Pattern> compilePatterns(List<String> raw, String path) {
        List<String> regexes = textList(raw, path);
        List<Pattern> patterns = new ArrayList<>();
        for (int i = 0; i < regexes.size(); i++) {
            try {
                patterns.add(Pattern.compile(regexes.get(i)));
            } catch (PatternSyntaxException e) {
                throw new ConfigException(path + "[" + i + "]: 정규식이 올바르지 않습니다 - " + e.getDescription(), e);
            }
        }
        return patterns;
    }

    private static List<String> textList(List<String> raw, String path) {
        if (raw == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            values.add(requireText(raw.get(i), path + "[" + i + "]"));
        }
        return values;
    }

    private static String requireText(String value, String path) {
        if (value == null || value.isBlank()) {
            throw missing(path);
        }
        return value.strip();
    }

    private static int requireAtLeast(Integer value, int min, String path) {
        if (value == null) {
            throw missing(path);
        }
        if (value < min) {
            throw new ConfigException(path + ": " + min + " 이상이어야 합니다(현재 " + value + ")");
        }
        return value;
    }

    private static double requireThreshold(Double value, String path) {
        if (value == null) {
            throw missing(path);
        }
        if (value <= 0 || value > 1) {
            throw new ConfigException(path + ": 0보다 크고 1 이하여야 합니다(현재 " + value + ")");
        }
        return value;
    }

    private static ConfigException missing(String path) {
        return new ConfigException(path + ": 필수 항목이 없습니다");
    }

    /** sources.yml 최상위 구조 */
    private record RawConfig(String userAgent, Integer requestDelayMillis, Integer timeoutSeconds,
                             Integer windowHours, Integer perCategory, Double clusterThreshold,
                             RawRetention retention, List<SourceConfig> sources,
                             List<String> itKeywords, List<String> stopwords) {
    }

    private record RawRetention(Integer siteArchiveDays, Integer newsDays) {
    }
}
