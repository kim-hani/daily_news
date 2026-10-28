package dailynews.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import org.junit.jupiter.api.Test;

class FakeHttpFetcherTest {

    private final FakeHttpFetcher fetcher = new FakeHttpFetcher()
            .respond("https://a.example.com/ok", 200, "본문")
            .respond("https://a.example.com/error", 500, "")
            .respondWithFixture("https://a.example.com/robots.txt", "/http/hello.txt", "text/plain")
            .fail("https://a.example.com/timeout", new HttpTimeoutException("시간 초과"));

    @Test
    void 등록한_응답을_돌려준다() throws IOException {
        FetchResult ok = fetcher.fetch(URI.create("https://a.example.com/ok"));
        FetchResult error = fetcher.fetch(URI.create("https://a.example.com/error"));

        assertThat(ok.isSuccess()).isTrue();
        assertThat(ok.bodyAsUtf8()).isEqualTo("본문");
        assertThat(error.status()).isEqualTo(500);
        assertThat(error.isSuccess()).isFalse();
    }

    @Test
    void fixture_파일_내용으로_응답한다() throws IOException {
        FetchResult result = fetcher.fetch(URI.create("https://a.example.com/robots.txt"));

        assertThat(result.status()).isEqualTo(200);
        assertThat(result.bodyAsUtf8()).startsWith("hello fixture");
        assertThat(result.contentType()).isEqualTo("text/plain");
    }

    @Test
    void 등록하지_않은_URI는_404다() throws IOException {
        assertThat(fetcher.fetch(URI.create("https://a.example.com/none")).status()).isEqualTo(404);
    }

    @Test
    void 등록한_예외를_던지고_요청_기록을_남긴다() {
        assertThatThrownBy(() -> fetcher.fetch(URI.create("https://a.example.com/timeout")))
                .isInstanceOf(HttpTimeoutException.class);

        assertThat(fetcher.requests()).containsExactly(URI.create("https://a.example.com/timeout"));
    }
}
