package dailynews.render;

import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.NewsItem;
import dailynews.model.Newsletter;
import dailynews.model.RunStats;
import dailynews.model.Source;
import dailynews.model.Story;
import dailynews.model.Summary;
import dailynews.model.SummaryOrigin;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link MarkdownRenderer}가 만든 Markdown 뉴스레터를 다시 {@link Newsletter}로 읽는다.
 * 옛 디자인으로 남은 날짜별 페이지를 새 디자인으로 다시 만들 때 쓴다({@link LegacyArchiveUpgrader}).
 * <p>
 * Markdown에 없는 값은 화면에 쓰이지 않는 기본값으로 채운다: 사안 점수 0, 본문 요약 통계 0, 언론사는 이름만.
 * 기사 시각은 시:분만 있으므로 뉴스레터 날짜로 두고, 생성 시각보다 늦으면 전날 기사로 본다.
 */
public final class MarkdownNewsletterReader {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter GENERATED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Pattern STATS = Pattern.compile(
            "^> 수집: (.*) \\| 수집 (\\d+)건 → 중복 제거 (\\d+)건 → 선정 (\\d+)건$");
    private static final String KEYWORDS = "> 🔑 오늘의 키워드: ";
    private static final String FAILED = "> ⚠️ 수집하지 못한 피드: ";
    private static final Pattern ITEM = Pattern.compile("^### \\d+\\. (.*)$");
    private static final Pattern OUTLET = Pattern.compile("^- 언론사: (.+) · (\\d{2}):(\\d{2})(?: · 🗞 (\\d+)개 언론사 보도)?$");
    private static final String URL = "- 원문: ";
    private static final String RSS_MARK = " _(RSS 요약)_";
    private static final String RELATED = "- 같은 사안: ";
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)\\]\\(([^)\\s]+)\\)");
    private static final String GENERATED_PREFIX = "생성: ";
    private static final Pattern ESCAPED = Pattern.compile("\\\\([\\\\`*_\\[\\]#<>|])");

    /**
     * @param date 파일 이름의 날짜
     * @throws IllegalArgumentException 형식이 맞지 않을 때(필수 줄이 없거나 기사 줄이 깨짐)
     */
    public Newsletter read(String markdown, LocalDate date) {
        List<String> sourceNames = null;
        int collected = 0;
        int afterDedup = 0;
        int selected = 0;
        List<String> keywords = List.of();
        List<String> failedFeeds = List.of();
        ZonedDateTime generatedAt = null;
        Map<Category, List<ItemLines>> sections = new EnumMap<>(Category.class);
        Category category = null;
        ItemLines item = null;

        // Windows에서 체크아웃한 파일은 CRLF일 수 있다.
        for (String line : markdown.split("\r?\n", -1)) {
            Matcher matcher;
            if ((matcher = STATS.matcher(line)).matches()) {
                sourceNames = List.of(matcher.group(1).split(" · "));
                collected = Integer.parseInt(matcher.group(2));
                afterDedup = Integer.parseInt(matcher.group(3));
                selected = Integer.parseInt(matcher.group(4));
            } else if (line.startsWith(KEYWORDS)) {
                keywords = Arrays.stream(line.substring(KEYWORDS.length()).split(" · "))
                        .map(MarkdownNewsletterReader::unescape).toList();
            } else if (line.startsWith(FAILED)) {
                failedFeeds = List.of(line.substring(FAILED.length()).split(", "));
            } else if (line.startsWith("## ")) {
                category = category(line);
                sections.put(category, new ArrayList<>());
                item = null;
            } else if ((matcher = ITEM.matcher(line)).matches()) {
                if (category == null) {
                    throw new IllegalArgumentException("카테고리 앞에 기사가 있습니다: " + line);
                }
                item = new ItemLines(unescape(matcher.group(1)));
                sections.get(category).add(item);
            } else if (line.equals("---")) {
                category = null;
                item = null;
            } else if (line.startsWith(GENERATED_PREFIX) && line.endsWith(" KST")) {
                String value = line.substring(GENERATED_PREFIX.length(), line.length() - " KST".length());
                generatedAt = LocalDateTime.parse(value, GENERATED).atZone(KST);
            } else if (item != null) {
                item.add(line);
            }
        }
        if (sourceNames == null || generatedAt == null || sections.size() != Category.values().length) {
            throw new IllegalArgumentException("통계·생성 시각·카테고리 5개 중 빠진 것이 있습니다");
        }

        Map<String, Source> sources = new HashMap<>();
        Map<Category, List<NewsItem>> items = new EnumMap<>(Category.class);
        for (Map.Entry<Category, List<ItemLines>> section : sections.entrySet()) {
            List<NewsItem> parsed = new ArrayList<>();
            for (ItemLines lines : section.getValue()) {
                parsed.add(lines.toNewsItem(section.getKey(), date, generatedAt, sources));
            }
            items.put(section.getKey(), parsed);
        }
        return new Newsletter(date, generatedAt, sourceNames, items, keywords,
                new RunStats(collected, afterDedup, selected, failedFeeds, 0, 0));
    }

    private static Category category(String line) {
        for (Category category : Category.values()) {
            if (line.equals("## " + category.emoji() + " " + category.label())) {
                return category;
            }
        }
        throw new IllegalArgumentException("알 수 없는 카테고리: " + line);
    }

    static String unescape(String text) {
        return ESCAPED.matcher(text).replaceAll("$1");
    }

    /** 언론사 이름만 아는 Source. 뉴스피드는 언론사 이름만 쓴다. */
    private static Source source(String name, Map<String, Source> sources) {
        return sources.computeIfAbsent(name, key -> new Source(key, key, List.of(), "", List.of(), Map.of(), Set.of()));
    }

    /** 기사 하나(### 줄 아래의 줄들) */
    private static final class ItemLines {

        private final String title;
        private final List<String> lines = new ArrayList<>();

        ItemLines(String title) {
            this.title = title;
        }

        void add(String line) {
            lines.add(line);
        }

        NewsItem toNewsItem(Category category, LocalDate date, ZonedDateTime generatedAt, Map<String, Source> sources) {
            Matcher outlet = null;
            URI url = null;
            List<String> quote = new ArrayList<>();
            List<Article> related = new ArrayList<>();
            for (String line : lines) {
                Matcher matcher = OUTLET.matcher(line);
                if (matcher.matches()) {
                    outlet = matcher;
                } else if (line.startsWith(URL)) {
                    url = URI.create(line.substring(URL.length()).trim());
                } else if (line.startsWith("> ")) {
                    quote.add(line.substring(2));
                } else if (line.startsWith(RELATED)) {
                    Matcher link = LINK.matcher(line.substring(RELATED.length()));
                    while (link.find()) {
                        URI target = URI.create(link.group(2).replace("%28", "(").replace("%29", ")"));
                        related.add(new Article(source(link.group(1), sources), category, "", target, generatedAt, ""));
                    }
                }
            }
            if (outlet == null || url == null) {
                throw new IllegalArgumentException("언론사 또는 원문 줄이 없는 기사: " + title);
            }

            ZonedDateTime publishedAt = date.atTime(LocalTime.of(Integer.parseInt(outlet.group(2)),
                    Integer.parseInt(outlet.group(3)))).atZone(KST);
            if (publishedAt.isAfter(generatedAt)) {
                publishedAt = publishedAt.minusDays(1);
            }
            int outletCount = outlet.group(4) == null ? 1 : Integer.parseInt(outlet.group(4));
            Article representative = new Article(source(outlet.group(1), sources), category, title, url, publishedAt, "");

            Summary summary;
            if (quote.isEmpty()) {
                summary = new Summary("", SummaryOrigin.NONE);
            } else {
                String text = String.join(" ", quote);
                boolean rss = text.endsWith(RSS_MARK);
                if (rss) {
                    text = text.substring(0, text.length() - RSS_MARK.length());
                }
                summary = new Summary(unescape(text), rss ? SummaryOrigin.RSS : SummaryOrigin.BODY);
            }
            return new NewsItem(new Story(category, representative, related, outletCount, 0), summary);
        }
    }
}
