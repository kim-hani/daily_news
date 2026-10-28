package dailynews.model;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 하루치 뉴스레터. 렌더러의 입력이다.
 *
 * @param sourceNames 수집 대상 언론사 이름(설정 순서)
 * @param sections    카테고리별 기사. Category 순서를 따른다.
 */
public record Newsletter(LocalDate date, ZonedDateTime generatedAt, List<String> sourceNames,
                         Map<Category, List<NewsItem>> sections,
                         List<String> keywords, RunStats stats) {

    public Newsletter {
        sourceNames = List.copyOf(sourceNames);
        EnumMap<Category, List<NewsItem>> sectionCopy = new EnumMap<>(Category.class);
        sections.forEach((category, items) -> sectionCopy.put(category, List.copyOf(items)));
        sections = Collections.unmodifiableMap(sectionCopy);
        keywords = List.copyOf(keywords);
    }
}
