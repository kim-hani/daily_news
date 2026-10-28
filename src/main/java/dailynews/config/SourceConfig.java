package dailynews.config;

import dailynews.model.Category;
import java.util.List;
import java.util.Map;

/**
 * sources.yml의 언론사 항목을 그대로 읽은 값. {@link ConfigLoader}가 검증한 뒤 {@link dailynews.model.Source}로 바꾼다.
 */
record SourceConfig(String id, String name, String bodySelector, List<String> creditPatterns,
                    Map<Category, String> feeds, List<Category> itKeywordFilterFeeds) {
}
