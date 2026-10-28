package dailynews.summary;

import dailynews.model.Source;
import dailynews.model.Summary;
import dailynews.model.SummaryOrigin;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 본문 앞 문장으로 요약을 만든다(리드 추출, FR-5.2~5.5).
 * <ol>
 *   <li>문단에서 크레디트, 이메일, 맨 앞 {@code [언론사]} 머리말을 지우고, 관련기사·저작권 문단은 버린다.</li>
 *   <li>앞 문장부터 모아 120자 이상이 되거나 3문장이 되면 멈춘다. 200자를 넘으면 마지막 문장을 뺀다.
 *       첫 문장 하나가 200자를 넘으면 199자에서 자르고 {@code …}을 붙인다.</li>
 *   <li>정제한 본문이 40자 미만이면 RSS 설명문에 같은 규칙을 적용한다({@link SummaryOrigin#RSS}).
 *       그것도 없으면 요약 없음({@link SummaryOrigin#NONE})이다.</li>
 * </ol>
 * 요약의 각 문장은 정제한 문단의 부분 문자열이다. 원문에 없는 문장은 만들지 않는다(SC5).
 */
public final class LeadSummarizer {

    static final int MAX_LENGTH = 200;
    static final int TARGET_LENGTH = 120;
    static final int MAX_SENTENCES = 3;
    static final int MIN_BODY_LENGTH = 40;

    /** (서울=뉴스1) 홍길동 기자 = / (워싱턴=연합뉴스) 김동현 특파원 = / (세종=연합뉴스) */
    private static final Pattern GENERIC_CREDIT =
            Pattern.compile("^\\([^()=]{1,20}=[^()=]{1,20}\\)\\s*(?:[^=]{0,40}?(?:기자|특파원|통신원)\\s*=\\s*)?");
    private static final Pattern MEDIA_HEADER = Pattern.compile("^\\[[^\\]]{1,20}]\\s*");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern BOILERPLATE = Pattern.compile("ⓒ|©|무단\\s*전재|재배포\\s*금지|저작권자|^[▶☞■△]");
    private static final Pattern TRUNCATED = Pattern.compile("(\\.\\.\\.|…|···)$");
    private static final Pattern SPACES = Pattern.compile("(?U)\\s+");

    private final SentenceSplitter splitter;

    public LeadSummarizer(SentenceSplitter splitter) {
        this.splitter = splitter;
    }

    public Summary summarize(List<String> bodyParagraphs, String rssDescription, Source source) {
        List<String> body = cleanParagraphs(bodyParagraphs, source);
        if (length(String.join(" ", body)) >= MIN_BODY_LENGTH) {
            List<String> lead = leadSentences(body, false);
            if (!lead.isEmpty()) {
                return new Summary(join(lead), SummaryOrigin.BODY);
            }
        }
        List<String> rss = cleanParagraphs(rssDescription == null ? List.of() : List.of(rssDescription), source);
        List<String> lead = leadSentences(rss, true);
        if (!lead.isEmpty()) {
            return new Summary(join(lead), SummaryOrigin.RSS);
        }
        return new Summary("", SummaryOrigin.NONE);
    }

    /** 문장이 아닌 부분을 지운 문단 */
    List<String> cleanParagraphs(List<String> paragraphs, Source source) {
        List<String> cleaned = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String text = SPACES.matcher(paragraph).replaceAll(" ").strip();
            if (BOILERPLATE.matcher(text).find()) {
                continue;
            }
            for (Pattern credit : source.creditPatterns()) {
                text = credit.matcher(text).replaceFirst("");
            }
            text = GENERIC_CREDIT.matcher(text).replaceFirst("");
            text = MEDIA_HEADER.matcher(text).replaceFirst("");
            text = EMAIL.matcher(text).replaceAll("");
            text = SPACES.matcher(text).replaceAll(" ").strip();
            if (!text.isEmpty()) {
                cleaned.add(text);
            }
        }
        return cleaned;
    }

    /**
     * 요약에 쓸 문장들. 마지막 문장이 200자에서 잘렸으면 끝에 {@code …}이 붙어 있다.
     *
     * @param dropTruncatedTail RSS 설명문처럼 끝이 잘린 글이면, 잘린 마지막 조각은 다른 문장이 있을 때 쓰지 않는다
     */
    List<String> leadSentences(List<String> paragraphs, boolean dropTruncatedTail) {
        List<String> sentences = new ArrayList<>();
        paragraphs.forEach(paragraph -> sentences.addAll(splitter.split(paragraph)));
        if (dropTruncatedTail && sentences.size() > 1 && TRUNCATED.matcher(sentences.getLast()).find()) {
            sentences.removeLast();
        }
        if (sentences.isEmpty()) {
            return List.of();
        }

        List<String> picked = new ArrayList<>();
        for (String sentence : sentences) {
            picked.add(sentence);
            if (length(join(picked)) >= TARGET_LENGTH || picked.size() == MAX_SENTENCES) {
                break;
            }
        }
        while (picked.size() > 1 && length(join(picked)) > MAX_LENGTH) {
            picked.removeLast();
        }
        String first = picked.getFirst();
        if (length(first) > MAX_LENGTH) {
            picked.set(0, first.substring(0, first.offsetByCodePoints(0, MAX_LENGTH - 1)).strip() + "…");
        }
        return picked;
    }

    private static String join(List<String> sentences) {
        return String.join(" ", sentences);
    }

    private static int length(String text) {
        return text.codePointCount(0, text.length());
    }
}
