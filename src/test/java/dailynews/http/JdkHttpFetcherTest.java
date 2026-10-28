package dailynews.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdkHttpFetcherTest {

    private static final String USER_AGENT = "DailyNewsBot/1.0 (+test)";
    private static final Duration RETRY_DELAY = Duration.ofSeconds(1);

    private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();
    private final List<String> userAgents = new CopyOnWriteArrayList<>();
    private final List<Duration> sleeps = new CopyOnWriteArrayList<>();

    private HttpServer server;
    private ExecutorService executor;
    private JdkHttpFetcher fetcher;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/ok", exchange -> {
            userAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
            respond(exchange, 200, "ok");
        });
        server.createContext("/flaky", exchange -> respond(exchange, hit("/flaky") == 1 ? 500 : 200, "flaky"));
        server.createContext("/down", exchange -> respond(exchange, hit("/down") > 0 ? 503 : 200, "down"));
        server.createContext("/missing", exchange -> respond(exchange, hit("/missing") > 0 ? 404 : 200, "missing"));
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/ok");
            respond(exchange, 302, "");
        });
        server.createContext("/slow", exchange -> {
            if (hit("/slow") == 1) {
                sleepQuietly(Duration.ofSeconds(2));
            }
            respond(exchange, 200, "slow");
        });
        server.createContext("/always-slow", exchange -> {
            hit("/always-slow");
            sleepQuietly(Duration.ofSeconds(2));
            respond(exchange, 200, "always-slow");
        });
        server.start();

        fetcher = new JdkHttpFetcher(USER_AGENT, Duration.ofMillis(500), RETRY_DELAY, sleeps::add);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void User_Agent를_붙이고_본문과_Content_Type을_돌려준다() throws IOException {
        FetchResult result = fetcher.fetch(uri("/ok"));

        assertThat(result.status()).isEqualTo(200);
        assertThat(result.bodyAsUtf8()).isEqualTo("ok");
        assertThat(result.contentType()).isEqualTo("text/plain; charset=UTF-8");
        assertThat(userAgents).containsExactly(USER_AGENT);
    }

    @Test
    void 리다이렉트를_따라가고_최종_URI를_남긴다() throws IOException {
        FetchResult result = fetcher.fetch(uri("/redirect"));

        assertThat(result.status()).isEqualTo(200);
        assertThat(result.finalUri()).isEqualTo(uri("/ok"));
    }

    @Test
    void 서버_오류_5xx는_1회_재시도한다() throws IOException {
        FetchResult result = fetcher.fetch(uri("/flaky"));

        assertThat(result.status()).isEqualTo(200);
        assertThat(hits.get("/flaky")).hasValue(2);
        assertThat(sleeps).containsExactly(RETRY_DELAY);
    }

    @Test
    void 재시도해도_5xx면_그_응답을_돌려준다() throws IOException {
        FetchResult result = fetcher.fetch(uri("/down"));

        assertThat(result.status()).isEqualTo(503);
        assertThat(hits.get("/down")).hasValue(2);
    }

    @Test
    void 클라이언트_오류_4xx는_재시도하지_않는다() throws IOException {
        FetchResult result = fetcher.fetch(uri("/missing"));

        assertThat(result.status()).isEqualTo(404);
        assertThat(hits.get("/missing")).hasValue(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void 시간_초과는_1회_재시도한다() throws IOException {
        FetchResult result = fetcher.fetch(uri("/slow"));

        assertThat(result.bodyAsUtf8()).isEqualTo("slow");
        assertThat(hits.get("/slow")).hasValue(2);
        assertThat(sleeps).containsExactly(RETRY_DELAY);
    }

    @Test
    void 재시도해도_시간_초과면_예외를_던진다() {
        assertThatThrownBy(() -> fetcher.fetch(uri("/always-slow")))
                .isInstanceOf(HttpTimeoutException.class);
        assertThat(hits.get("/always-slow")).hasValue(2);
    }

    private URI uri(String path) {
        return URI.create("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":"
                + server.getAddress().getPort() + path);
    }

    private int hit(String path) {
        return hits.computeIfAbsent(path, key -> new AtomicInteger()).incrementAndGet();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
