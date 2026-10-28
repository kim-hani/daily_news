package dailynews.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 같은 호스트에 연속으로 요청할 때 간격을 두는 {@link HttpFetcher} 데코레이터(NFR-3).
 * 간격은 직전 요청이 끝난 시각부터 잰다. 호스트별로 다른 간격을 줄 수 있다(예: 빈도 제한이 엄한 사이트).
 * 요청은 순차로 보낸다는 전제라 스레드 안전하지 않다.
 */
public final class HostThrottle implements HttpFetcher {

    private final HttpFetcher delegate;
    private final Duration defaultDelay;
    private final Map<String, Duration> hostDelays;
    private final Clock clock;
    private final Sleeper sleeper;
    private final Map<String, Instant> lastRequestEnd = new HashMap<>();

    public HostThrottle(HttpFetcher delegate, Duration defaultDelay) {
        this(delegate, defaultDelay, Map.of());
    }

    /** @param hostDelays 소문자 호스트 → 간격. 없는 호스트는 {@code defaultDelay} */
    public HostThrottle(HttpFetcher delegate, Duration defaultDelay, Map<String, Duration> hostDelays) {
        this(delegate, defaultDelay, hostDelays, Clock.systemUTC(), Sleeper.SYSTEM);
    }

    HostThrottle(HttpFetcher delegate, Duration defaultDelay, Map<String, Duration> hostDelays, Clock clock,
                 Sleeper sleeper) {
        this.delegate = delegate;
        this.defaultDelay = defaultDelay;
        this.hostDelays = Map.copyOf(hostDelays);
        this.clock = clock;
        this.sleeper = sleeper;
    }

    @Override
    public FetchResult fetch(URI uri) throws IOException {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        waitFor(host);
        try {
            return delegate.fetch(uri);
        } finally {
            lastRequestEnd.put(host, clock.instant());
        }
    }

    private void waitFor(String host) throws InterruptedIOException {
        Instant last = lastRequestEnd.get(host);
        if (last == null) {
            return;
        }
        Duration delay = hostDelays.getOrDefault(host, defaultDelay);
        Duration remaining = delay.minus(Duration.between(last, clock.instant()));
        if (remaining.isNegative() || remaining.isZero()) {
            return;
        }
        try {
            sleeper.sleep(remaining);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("요청 간격 대기 중 인터럽트: " + host);
        }
    }
}
