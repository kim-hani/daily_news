package dailynews.http;

import java.io.IOException;
import java.net.URI;

/**
 * 외부 HTTP GET 요청. 테스트에서는 fixture를 돌려주는 가짜 구현으로 바꿔 끼운다(NFR-7).
 */
public interface HttpFetcher {

    /**
     * URI를 요청한다. 4xx·5xx 응답도 예외 없이 {@link FetchResult}로 돌려준다.
     *
     * @throws IOException 연결 실패, 시간 초과 등 응답을 받지 못했을 때
     */
    FetchResult fetch(URI uri) throws IOException;
}
