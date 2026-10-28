package dailynews;

import dailynews.config.AppConfig;
import dailynews.config.ConfigException;
import dailynews.config.ConfigLoader;
import dailynews.http.HostThrottle;
import dailynews.http.HttpFetcher;
import dailynews.http.JdkHttpFetcher;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Daily News 배치의 진입점. 옵션과 설정을 읽고 파이프라인을 실행한 뒤 종료 코드로 끝난다.
 * 종료 코드: 0 성공(일부 피드 실패 포함), 1 수집 0건·설정·옵션 오류, 2 출력 실패.
 */
public final class DailyNewsApp {

    private static final Logger log = LoggerFactory.getLogger(DailyNewsApp.class);

    private DailyNewsApp() {
    }

    public static void main(String[] args) {
        System.exit(run(args, Clock.system(DailyNewsPipeline.KST), DailyNewsApp::networkFetcher));
    }

    /**
     * @param fetcherFactory 설정으로 HTTP 요청기를 만든다(테스트에서는 fixture를 돌려주는 가짜를 넣는다)
     */
    static int run(String[] args, Clock clock, Function<AppConfig, HttpFetcher> fetcherFactory) {
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException e) {
            log.error("{}\n{}", e.getMessage(), CliOptions.USAGE);
            return DailyNewsPipeline.EXIT_FAILURE;
        }

        AppConfig config;
        try {
            config = options.config() == null ? ConfigLoader.loadDefault() : ConfigLoader.load(options.config());
        } catch (ConfigException e) {
            log.error("설정 오류: {}", e.getMessage());
            return DailyNewsPipeline.EXIT_FAILURE;
        }

        LocalDate date = options.date() != null ? options.date() : LocalDate.now(clock.withZone(DailyNewsPipeline.KST));
        log.info("Daily News 시작: 날짜 {}, 출력 {}{}", date, options.root().toAbsolutePath().normalize(),
                options.dryRun() ? " (dry-run)" : "");
        return new DailyNewsPipeline(config, fetcherFactory.apply(config), clock)
                .run(date, options.root(), options.dryRun())
                .exitCode();
    }

    /** User-Agent·타임아웃·재시도를 갖춘 요청기를 호스트별 간격 데코레이터로 감싼다(호스트별 간격은 설정의 hostDelayMillis). */
    static HttpFetcher networkFetcher(AppConfig config) {
        HttpFetcher http = new JdkHttpFetcher(config.userAgent(), Duration.ofSeconds(config.timeoutSeconds()));
        Map<String, Duration> hostDelays = config.hostDelayMillis().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Duration.ofMillis(entry.getValue())));
        return new HostThrottle(http, Duration.ofMillis(config.requestDelayMillis()), hostDelays);
    }
}
