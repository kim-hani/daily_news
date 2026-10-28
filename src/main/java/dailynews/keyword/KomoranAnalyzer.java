package dailynews.keyword;

import java.util.List;
import java.util.Set;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KOMORAN 형태소 분석기 래퍼. 모델을 읽는 데 시간이 걸려서 처음 쓸 때 한 번만 만들고
 * 클러스터링과 키워드 추출이 함께 쓴다.
 */
public final class KomoranAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(KomoranAnalyzer.class);

    /** 고유명사, 일반명사, 외국어(영문 약어 예: SCM, AI) */
    private static final Set<String> NOUN_TAGS = Set.of("NNP", "NNG", "SL");
    private static final int MIN_LENGTH = 2;

    private final Komoran komoran;

    private KomoranAnalyzer() {
        long start = System.nanoTime();
        this.komoran = new Komoran(DEFAULT_MODEL.FULL);
        log.info("KOMORAN 모델 로딩 {}ms", (System.nanoTime() - start) / 1_000_000);
    }

    public static KomoranAnalyzer getInstance() {
        return Holder.INSTANCE;
    }

    /** 2글자 이상의 명사(NNP·NNG)와 영문(SL)을 나온 순서대로 돌려준다. */
    public synchronized List<Noun> nouns(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return komoran.analyze(text).getTokenList().stream()
                .filter(token -> NOUN_TAGS.contains(token.getPos()))
                .filter(token -> token.getMorph().codePointCount(0, token.getMorph().length()) >= MIN_LENGTH)
                .map(KomoranAnalyzer::toNoun)
                .toList();
    }

    private static Noun toNoun(Token token) {
        return new Noun(token.getMorph(), token.getPos());
    }

    /**
     * @param tag NNP(고유명사), NNG(일반명사), SL(외국어)
     */
    public record Noun(String text, String tag) {

        /** 고유명사나 영문 약어처럼 이름에 가까운 명사인지(키워드 동률 정렬에 쓴다) */
        public boolean isProper() {
            return tag.equals("NNP") || tag.equals("SL");
        }
    }

    private static final class Holder {
        private static final KomoranAnalyzer INSTANCE = new KomoranAnalyzer();
    }
}
