package dailynews.keyword;

import dailynews.clean.TitleNormalizer;
import dailynews.keyword.KomoranAnalyzer.Noun;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 그날 수집한 기사 제목에서 오늘의 키워드를 뽑는다(FR-6.1).
 * 명사마다 등장한 제목 수를 세고, 불용어를 뺀 뒤 많은 순으로 고른다.
 * 같은 수면 고유명사(영문 약어 포함)를 먼저, 그다음 가나다순으로 둔다.
 */
public final class KeywordExtractor {

    private final Function<String, List<Noun>> analyzer;
    private final Set<String> stopwords;

    /**
     * @param analyzer 명사 추출기(보통 {@code KomoranAnalyzer.getInstance()::nouns})
     */
    public KeywordExtractor(Function<String, List<Noun>> analyzer, Collection<String> stopwords) {
        this.analyzer = analyzer;
        this.stopwords = stopwords.stream().map(word -> word.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    public List<String> extract(List<String> titles, int limit) {
        Map<String, Integer> counts = new HashMap<>();
        Set<String> proper = new HashSet<>();
        for (String title : titles) {
            Set<String> inTitle = new LinkedHashSet<>();
            for (Noun noun : analyzer.apply(TitleNormalizer.clean(title))) {
                if (stopwords.contains(noun.text().toLowerCase(Locale.ROOT))) {
                    continue;
                }
                inTitle.add(noun.text());
                if (noun.isProper()) {
                    proper.add(noun.text());
                }
            }
            inTitle.forEach(noun -> counts.merge(noun, 1, Integer::sum));
        }
        return counts.keySet().stream()
                .sorted(Comparator.comparing((String noun) -> counts.get(noun)).reversed()
                        .thenComparing(noun -> !proper.contains(noun))
                        .thenComparing(Comparator.naturalOrder()))
                .limit(limit)
                .toList();
    }
}
