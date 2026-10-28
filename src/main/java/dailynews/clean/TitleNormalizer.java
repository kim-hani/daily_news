package dailynews.clean;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 제목을 비교·클러스터링·키워드 추출용 형태로 바꾼다(FR-3.2, FR-4.1). 원래 제목(표시용)은 건드리지 않는다.
 */
public final class TitleNormalizer {

    /** [단독], [속보], [정치를 부탁해], 【포토】, &lt;속보&gt; 같은 말머리·꼬리표 */
    private static final Pattern TAG = Pattern.compile("\\[[^\\]]{0,20}]|【[^】]{0,20}】|<[^>]{0,10}>");
    /** 연합뉴스식 갱신 표시: (종합), (종합2보), (2보), (속보), (상보) */
    private static final Pattern UPDATE_MARK = Pattern.compile("\\((?:종합\\d*보?|\\d+보|속보|상보)\\)");
    private static final Pattern QUOTES = Pattern.compile("[\"'`“”‘’「」『』]");
    private static final Pattern NON_TEXT = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private TitleNormalizer() {
    }

    /** 말머리·갱신 표시·따옴표를 지우고 공백을 정리한다. 대소문자와 문장부호는 남긴다(키워드 추출용). */
    public static String clean(String title) {
        String text = TAG.matcher(title).replaceAll(" ");
        text = UPDATE_MARK.matcher(text).replaceAll(" ");
        text = QUOTES.matcher(text).replaceAll(" ");
        return SPACES.matcher(text).replaceAll(" ").strip();
    }

    /** {@link #clean} 뒤 글자·숫자가 아닌 문자를 공백으로 바꾸고 소문자로 만든다(비교용). */
    public static String normalize(String title) {
        String text = NON_TEXT.matcher(clean(title)).replaceAll(" ");
        return SPACES.matcher(text).replaceAll(" ").strip().toLowerCase(Locale.ROOT);
    }
}
