package dailynews.collect;

/**
 * RSS 문서 전체를 해석할 수 없을 때 던진다(깨진 XML, RSS가 아닌 응답 등).
 */
public class RssParseException extends Exception {

    public RssParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
