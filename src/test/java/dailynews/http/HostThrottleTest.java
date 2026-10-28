package dailynews.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HostThrottleTest {

    private static final Duration DELAY = Duration.ofMillis(1000);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-28T00:00:00Z"));
    private final List<Duration> sleeps = new ArrayList<>();
    private final FakeHttpFetcher delegate = new FakeHttpFetcher()
            .fail("https://down.example.com/feed.xml", new ConnectException("연결 실패"));
    private final HostThrottle throttle = new HostThrottle(delegate, DELAY, clock, duration -> {
        sleeps.add(duration);
        clock.advance(duration);
    });

    @Test
    void 같은_호스트에_연속_요청하면_간격만큼_기다린다() throws IOException {
        throttle.fetch(URI.create("https://a.example.com/1"));
        throttle.fetch(URI.create("https://a.example.com/2"));
        throttle.fetch(URI.create("https://a.example.com/3"));

        assertThat(sleeps).containsExactly(DELAY, DELAY);
        assertThat(delegate.requests()).hasSize(3);
    }

    @Test
    void 다른_호스트는_기다리지_않는다() throws IOException {
        throttle.fetch(URI.create("https://a.example.com/1"));
        throttle.fetch(URI.create("https://b.example.com/1"));
        throttle.fetch(URI.create("https://c.example.com/1"));

        assertThat(sleeps).isEmpty();
    }

    @Test
    void 이미_시간이_지났으면_남은_만큼만_기다리거나_바로_보낸다() throws IOException {
        throttle.fetch(URI.create("https://a.example.com/1"));
        clock.advance(Duration.ofMillis(300));
        throttle.fetch(URI.create("https://a.example.com/2"));
        clock.advance(Duration.ofMillis(1500));
        throttle.fetch(URI.create("https://a.example.com/3"));

        assertThat(sleeps).containsExactly(Duration.ofMillis(700));
    }

    @Test
    void 호스트_대소문자는_구분하지_않는다() throws IOException {
        throttle.fetch(URI.create("https://A.example.com/1"));
        throttle.fetch(URI.create("https://a.example.com/2"));

        assertThat(sleeps).containsExactly(DELAY);
    }

    @Test
    void 실패한_요청_뒤에도_간격을_둔다() throws IOException {
        assertThatThrownBy(() -> throttle.fetch(URI.create("https://down.example.com/feed.xml")))
                .isInstanceOf(ConnectException.class);
        throttle.fetch(URI.create("https://down.example.com/other.xml"));

        assertThat(sleeps).containsExactly(DELAY);
    }

    /** 테스트에서 시각을 직접 옮기는 Clock */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
