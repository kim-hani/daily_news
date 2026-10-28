package dailynews.http;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 응답.
 *
 * @param body        응답 본문. 호출하는 쪽은 수정하지 않는다.
 * @param contentType Content-Type 헤더 값. 없으면 빈 문자열
 * @param finalUri    리다이렉트를 따라간 뒤의 최종 URI
 */
public record FetchResult(int status, byte[] body, String contentType, URI finalUri) {

    /** 2xx 응답인지 */
    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }

    /** 본문을 UTF-8 문자열로 읽는다. 문자 인코딩을 따로 판단해야 하는 RSS·HTML은 {@link #body()}를 쓴다. */
    public String bodyAsUtf8() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
