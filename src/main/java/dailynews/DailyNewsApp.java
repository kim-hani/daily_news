package dailynews;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Daily News 배치의 진입점. 파이프라인 조립과 CLI 파싱은 T-030에서 채운다.
 */
public final class DailyNewsApp {

    private static final Logger log = LoggerFactory.getLogger(DailyNewsApp.class);

    private DailyNewsApp() {
    }

    public static void main(String[] args) {
        log.info("Daily News 시작");
    }
}
