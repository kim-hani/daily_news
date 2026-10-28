package dailynews.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트용 {@link HttpFetcher}. 등록한 URI에는 정해 둔 응답이나 예외를, 등록하지 않은 URI에는 404를 돌려준다.
 */
public final class FakeHttpFetcher implements HttpFetcher {

    private final Map<URI, Response> responses = new HashMap<>();
    private final List<URI> requests = new ArrayList<>();

    /** 상태 코드와 문자열 본문(UTF-8)으로 응답한다. */
    public FakeHttpFetcher respond(String uri, int status, String body) {
        return respond(uri, status, body.getBytes(StandardCharsets.UTF_8), "text/plain; charset=UTF-8");
    }

    public FakeHttpFetcher respond(String uri, int status, byte[] body, String contentType) {
        responses.put(URI.create(uri), new Response(status, body, contentType, null));
        return this;
    }

    /** classpath의 fixture 파일 내용으로 200 응답한다. 예: {@code "/fixtures/robots/yonhap.txt"} */
    public FakeHttpFetcher respondWithFixture(String uri, String resourcePath, String contentType) {
        return respond(uri, 200, readResource(resourcePath), contentType);
    }

    /** 응답 대신 예외(연결 실패, 시간 초과 등)를 던진다. */
    public FakeHttpFetcher fail(String uri, IOException exception) {
        responses.put(URI.create(uri), new Response(0, null, null, exception));
        return this;
    }

    @Override
    public FetchResult fetch(URI uri) throws IOException {
        requests.add(uri);
        Response response = responses.get(uri);
        if (response == null) {
            return new FetchResult(404, new byte[0], "", uri);
        }
        if (response.exception() != null) {
            throw response.exception();
        }
        return new FetchResult(response.status(), response.body(), response.contentType(), uri);
    }

    /** 지금까지 요청된 URI(순서대로) */
    public List<URI> requests() {
        return List.copyOf(requests);
    }

    private static byte[] readResource(String resourcePath) {
        try (InputStream in = FakeHttpFetcher.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("fixture가 없습니다: " + resourcePath);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record Response(int status, byte[] body, String contentType, IOException exception) {
    }
}
