package dailynews.select;

import dailynews.keyword.KomoranAnalyzer;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 정규화된 두 제목이 같은 사안인지 판단하는 유사도(0~1, FR-4.1).
 * {@code max(문자 2-gram Jaccard, 명사 집합 Jaccard)}다. 2-gram은 공백을 뺀 문자열로 만든다.
 * 같은 제목을 여러 번 비교하므로 {@link #features}로 특징을 한 번 만들어 두고 비교한다.
 */
public final class TitleSimilarity {

    private final KomoranAnalyzer analyzer;

    public TitleSimilarity(KomoranAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public Features features(String normalizedTitle) {
        String compact = normalizedTitle.replace(" ", "");
        Set<String> bigrams = new HashSet<>();
        for (int i = 0; i + 2 <= compact.length(); i++) {
            bigrams.add(compact.substring(i, i + 2));
        }
        Set<String> nouns = analyzer.nouns(normalizedTitle).stream()
                .map(KomoranAnalyzer.Noun::text)
                .collect(Collectors.toSet());
        return new Features(Set.copyOf(bigrams), Set.copyOf(nouns));
    }

    public double similarity(String normalizedA, String normalizedB) {
        return similarity(features(normalizedA), features(normalizedB));
    }

    public double similarity(Features a, Features b) {
        return Math.max(jaccard(a.bigrams(), b.bigrams()), jaccard(a.nouns(), b.nouns()));
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        long intersection = a.stream().filter(b::contains).count();
        return (double) intersection / (a.size() + b.size() - intersection);
    }

    /** 제목 하나의 비교용 특징 */
    public record Features(Set<String> bigrams, Set<String> nouns) {
    }
}
