package dailynews.clean;

import dailynews.model.Article;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code itKeywordFilterFeeds}로 지정된 피드(예: 연합뉴스 산업)의 기사는 제목이나 설명에 IT 키워드가 있을 때만 남긴다(FR-3.3).
 * 키워드 앞뒤에 영문자·숫자가 붙어 있으면 맞지 않는 것으로 본다(예: {@code AI}는 {@code MAIN}에 맞지 않음). 대소문자는 구분하지 않는다.
 */
public final class CategoryFilter {

    private static final Logger log = LoggerFactory.getLogger(CategoryFilter.class);

    private final Pattern keywords;

    public CategoryFilter(List<String> itKeywords) {
        this.keywords = itKeywords.isEmpty() ? null : Pattern.compile(itKeywords.stream()
                .map(keyword -> "(?<![A-Za-z0-9])" + Pattern.quote(keyword) + "(?![A-Za-z0-9])")
                .collect(Collectors.joining("|")), Pattern.CASE_INSENSITIVE);
    }

    public List<Article> filter(List<Article> articles) {
        List<Article> kept = articles.stream().filter(this::accepts).toList();
        if (kept.size() < articles.size()) {
            log.info("IT 키워드 필터: {}건 제외", articles.size() - kept.size());
        }
        return kept;
    }

    private boolean accepts(Article article) {
        if (!article.source().itKeywordFilterFeeds().contains(article.category())) {
            return true;
        }
        return matches(article.title()) || matches(article.rssDescription());
    }

    boolean matches(String text) {
        return keywords != null && text != null && keywords.matcher(text).find();
    }
}
