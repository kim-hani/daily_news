package dailynews.http;

import java.time.Duration;

/**
 * 대기. 테스트에서는 실제로 기다리지 않는 구현을 넣는다.
 */
@FunctionalInterface
public interface Sleeper {

    Sleeper SYSTEM = Thread::sleep;

    void sleep(Duration duration) throws InterruptedException;
}
