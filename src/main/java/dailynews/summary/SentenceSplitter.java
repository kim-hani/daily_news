package dailynews.summary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 한국어 기사 문단을 문장으로 나눈다.
 * {@code .}·{@code ?}·{@code !} 뒤에 공백이나 끝이 오면 문장 끝으로 본다(“…다.”, “…요.” 포함).
 * 따옴표가 열려 있는 동안은 나누지 않고, 따옴표가 닫히며 문장이 끝나면 닫는 따옴표까지 한 문장이다.
 * 영문 약어(U.S., Mr. 등)와 한 글자 이니셜 뒤의 점은 문장 끝으로 보지 않는다. 소수점(3.5%)은 뒤에 공백이 없어 나뉘지 않는다.
 */
public final class SentenceSplitter {

    private static final String OPEN_QUOTES = "“‘「『";
    private static final String CLOSE_QUOTES = "”’」』";
    private static final Set<String> ABBREVIATIONS = Set.of(
            "mr", "mrs", "ms", "dr", "prof", "st", "no", "vs", "etc", "jr", "sr", "inc", "co", "corp", "ltd", "gov",
            "u.s", "u.k", "u.n", "e.g", "i.e");

    public List<String> split(String text) {
        List<String> sentences = new ArrayList<>();
        int depth = 0;
        boolean inDouble = false;
        boolean inSingle = false;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean closedQuote = false;
            if (OPEN_QUOTES.indexOf(c) >= 0) {
                depth++;
            } else if (CLOSE_QUOTES.indexOf(c) >= 0) {
                depth = Math.max(0, depth - 1);
                closedQuote = true;
            } else if (c == '"') {
                inDouble = !inDouble;
                closedQuote = !inDouble;
            } else if (c == '\'' && !isApostrophe(text, i)) {
                inSingle = !inSingle;
                closedQuote = !inSingle;
            }
            boolean quoted = depth > 0 || inDouble || inSingle;
            if (quoted || !isBoundary(text, i)) {
                continue;
            }
            boolean end = isTerminal(c) && !isAbbreviation(text, i)
                    || closedQuote && i > 0 && isTerminal(text.charAt(i - 1));
            if (end) {
                add(sentences, text.substring(start, i + 1));
                start = i + 1;
            }
        }
        add(sentences, text.substring(start));
        return sentences;
    }

    private static void add(List<String> sentences, String sentence) {
        String trimmed = sentence.strip();
        if (!trimmed.isEmpty()) {
            sentences.add(trimmed);
        }
    }

    private static boolean isTerminal(char c) {
        return c == '.' || c == '?' || c == '!';
    }

    /** 다음 글자가 공백이거나 문단 끝인지 */
    private static boolean isBoundary(String text, int i) {
        return i + 1 == text.length() || Character.isWhitespace(text.charAt(i + 1));
    }

    /** Korea's 처럼 영문자 사이의 작은따옴표 */
    private static boolean isApostrophe(String text, int i) {
        return i > 0 && i + 1 < text.length() && isAsciiLetter(text.charAt(i - 1)) && isAsciiLetter(text.charAt(i + 1));
    }

    /** 점 앞의 단어가 영문 약어이거나 한 글자 이니셜(A. B.)인지 */
    private static boolean isAbbreviation(String text, int i) {
        if (text.charAt(i) != '.') {
            return false;
        }
        int start = i;
        while (start > 0 && (isAsciiLetter(text.charAt(start - 1)) || text.charAt(start - 1) == '.')) {
            start--;
        }
        String word = text.substring(start, i).toLowerCase(Locale.ROOT);
        return word.length() == 1 || ABBREVIATIONS.contains(word);
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}
