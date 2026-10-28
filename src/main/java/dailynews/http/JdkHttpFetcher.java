package dailynews.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDK {@link HttpClient}로 요청한다. User-Agent를 붙이고, 리다이렉트를 따라가며, 5xx와 시간 초과는 1회 재시도한다.
 */
public final class JdkHttpFetcher implements HttpFetcher {

    private static final Logger log = LoggerFactory.getLogger(JdkHttpFetcher.class);

    private static final int MAX_ATTEMPTS = 2;
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(1);

    private final HttpClient client;
    private final String userAgent;
    private final Duration timeout;
    private final Duration retryDelay;
    private final Sleeper sleeper;

    public JdkHttpFetcher(String userAgent, Duration timeout) {
        this(userAgent, timeout, DEFAULT_RETRY_DELAY, Sleeper.SYSTEM);
    }

    JdkHttpFetcher(String userAgent, Duration timeout, Duration retryDelay, Sleeper sleeper) {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(timeout)
                .build();
        this.userAgent = userAgent;
        this.timeout = timeout;
        this.retryDelay = retryDelay;
        this.sleeper = sleeper;
    }

    @Override
    public FetchResult fetch(URI uri) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", userAgent)
                .timeout(timeout)
                .GET()
                .build();
        for (int attempt = 1; ; attempt++) {
            boolean lastAttempt = attempt == MAX_ATTEMPTS;
            try {
                FetchResult result = send(request);
                if (result.status() < 500 || lastAttempt) {
                    return result;
                }
                log.warn("{} 응답 {}, 재시도합니다", uri, result.status());
            } catch (HttpTimeoutException e) {
                if (lastAttempt) {
                    throw e;
                }
                log.warn("{} 시간 초과, 재시도합니다", uri);
            }
            pause(retryDelay);
        }
    }

    private FetchResult send(HttpRequest request) throws IOException {
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            return new FetchResult(response.statusCode(), response.body(), contentType, response.uri());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("요청 중 인터럽트: " + request.uri());
        }
    }

    private void pause(Duration duration) throws InterruptedIOException {
        try {
            sleeper.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("재시도 대기 중 인터럽트");
        }
    }
}
