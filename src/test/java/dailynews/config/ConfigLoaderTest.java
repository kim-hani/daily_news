package dailynews.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dailynews.model.Category;
import dailynews.model.Source;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigLoaderTest {

    private static final String VALID = """
            userAgent: "TestBot/1.0"
            siteUrl: "https://example.github.io/news/"
            repositoryUrl: "https://github.com/example/news"
            requestDelayMillis: 0
            timeoutSeconds: 5
            windowHours: 12
            perCategory: 3
            clusterThreshold: 0.5
            retention:
              siteArchiveDays: 7
              newsDays: 0
            sources:
              - id: sample
                name: 샘플일보
                domains: [example.com]
                bodySelector: "div.body"
                creditPatterns: ['^\\(샘플\\)\\s*']
                feeds:
                  POLITICS: https://news.example.com/politics.xml
                  IT: https://news.example.com/it.xml
                itKeywordFilterFeeds: [IT]
            itKeywords: [AI, 반도체]
            stopwords: [기자]
            """;

    @TempDir
    Path tempDir;

    @Test
    void 기본_설정에는_언론사_3곳과_피드_15개가_있다() {
        AppConfig config = ConfigLoader.loadDefault();

        assertThat(config.sources()).extracting(Source::id).containsExactly("yonhap", "donga", "khan");
        assertThat(config.sources()).extracting(Source::domains)
                .containsExactly(List.of("yna.co.kr"), List.of("donga.com"), List.of("khan.co.kr"));
        assertThat(config.sources()).allSatisfy(source ->
                assertThat(source.feeds().keySet()).containsExactly(Category.values()));
        assertThat(config.sources().stream().mapToInt(source -> source.feeds().size()).sum()).isEqualTo(15);
        assertThat(config.sources()).flatExtracting(source -> source.feeds().values())
                .allSatisfy(uri -> assertThat(uri.getScheme()).isEqualTo("https"));
        assertThat(config.sources()).extracting(Source::fetchBody).containsExactly(true, true, false);
    }

    @Test
    void 기본_설정의_실행_값과_보관_정책() {
        AppConfig config = ConfigLoader.loadDefault();

        assertThat(config.userAgent()).startsWith("DailyNewsBot/1.0");
        assertThat(config.requestDelayMillis()).isEqualTo(1000);
        assertThat(config.hostDelayMillis()).containsEntry("www.khan.co.kr", 10000);
        assertThat(config.timeoutSeconds()).isEqualTo(10);
        assertThat(config.windowHours()).isEqualTo(24);
        assertThat(config.perCategory()).isEqualTo(5);
        assertThat(config.clusterThreshold()).isEqualTo(0.4);
        assertThat(config.retention()).isEqualTo(new AppConfig.Retention(30, 0));
        assertThat(config.itKeywords()).contains("반도체", "SK하이닉스");
        assertThat(config.stopwords()).contains("기자");
        assertThat(config.siteUrl()).isEqualTo("https://kim-hani.github.io/daily_news/");
        assertThat(config.repositoryUrl()).isEqualTo("https://github.com/kim-hani/daily_news");
    }

    @Test
    void 연합뉴스는_산업_피드만_IT_키워드로_거르고_크레디트를_지운다() {
        Source yonhap = ConfigLoader.loadDefault().sources().getFirst();

        assertThat(yonhap.itKeywordFilterFeeds()).containsExactly(Category.IT);
        assertThat(yonhap.creditPatterns()).hasSize(1);
        assertThat(yonhap.creditPatterns().getFirst()
                .matcher("(서울=연합뉴스) 홍길동 기자 = 국회가 28일 본회의를 열었다.").replaceFirst(""))
                .isEqualTo("국회가 28일 본회의를 열었다.");
        assertThat(yonhap.creditPatterns().getFirst()
                .matcher("(서울=연합뉴스) 김효정 전명훈 기자 = 한미 국방장관이 합의했다.").replaceFirst(""))
                .isEqualTo("한미 국방장관이 합의했다.");
    }

    @Test
    void 외부_파일로_설정을_바꿀_수_있다() throws IOException {
        AppConfig config = ConfigLoader.load(write(VALID));

        assertThat(config.userAgent()).isEqualTo("TestBot/1.0");
        assertThat(config.perCategory()).isEqualTo(3);
        assertThat(config.retention()).isEqualTo(new AppConfig.Retention(7, 0));
        assertThat(config.sources()).singleElement().satisfies(source -> {
            assertThat(source.name()).isEqualTo("샘플일보");
            assertThat(source.feeds().keySet()).containsExactly(Category.POLITICS, Category.IT);
            assertThat(source.itKeywordFilterFeeds()).containsExactly(Category.IT);
            assertThat(source.creditPatterns().getFirst().matcher("(샘플) 본문").replaceFirst("")).isEqualTo("본문");
        });
    }

    @Test
    void 선택_항목이_없으면_빈_목록으로_읽는다() throws IOException {
        String yaml = VALID
                .replace("    creditPatterns: ['^\\(샘플\\)\\s*']\n", "")
                .replace("    itKeywordFilterFeeds: [IT]\n", "")
                .replace("itKeywords: [AI, 반도체]\n", "")
                .replace("stopwords: [기자]\n", "");

        AppConfig config = ConfigLoader.load(write(yaml));

        assertThat(config.sources().getFirst().creditPatterns()).isEmpty();
        assertThat(config.sources().getFirst().itKeywordFilterFeeds()).isEmpty();
        assertThat(config.sources().getFirst().fetchBody()).isTrue();
        assertThat(config.itKeywords()).isEmpty();
        assertThat(config.stopwords()).isEmpty();
    }

    @Test
    void 언론사별로_원문_가져오기를_끌_수_있다() throws IOException {
        String yaml = VALID.replace("    itKeywordFilterFeeds: [IT]\n",
                "    itKeywordFilterFeeds: [IT]\n    fetchBody: false\n");

        AppConfig config = ConfigLoader.load(write(yaml));

        assertThat(config.sources().getFirst().fetchBody()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidConfigs")
    void 잘못된_설정은_위치가_담긴_예외를_던진다(String description, String yaml, String expectedMessage)
            throws IOException {
        Path path = write(yaml);

        assertThatThrownBy(() -> ConfigLoader.load(path))
                .isInstanceOf(ConfigException.class)
                .hasMessageStartingWith(path.toString())
                .hasMessageContaining(expectedMessage);
    }

    static Stream<Arguments> invalidConfigs() {
        return Stream.of(
                Arguments.of("http(s)가 아닌 URL",
                        VALID.replace("https://news.example.com/it.xml", "ftp://news.example.com/it.xml"),
                        "sources[sample].feeds.IT: http(s) URL만"),
                Arguments.of("스킴 없는 URL",
                        VALID.replace("https://news.example.com/it.xml", "news.example.com/it.xml"),
                        "sources[sample].feeds.IT: http(s) URL만"),
                Arguments.of("형식이 깨진 URL",
                        VALID.replace("https://news.example.com/it.xml", "https://news.example.com/a b"),
                        "sources[sample].feeds.IT: URL 형식"),
                Arguments.of("bodySelector 누락",
                        VALID.replace("    bodySelector: \"div.body\"\n", ""),
                        "sources[sample].bodySelector: 필수 항목"),
                Arguments.of("id 누락",
                        VALID.replace("  - id: sample\n    name", "  - name"),
                        "sources[0].id: 필수 항목"),
                Arguments.of("userAgent 누락",
                        VALID.replace("userAgent: \"TestBot/1.0\"\n", ""),
                        "userAgent: 필수 항목"),
                Arguments.of("retention 누락",
                        VALID.replace("retention:\n  siteArchiveDays: 7\n  newsDays: 0\n", ""),
                        "retention: 필수 항목"),
                Arguments.of("피드 URL 값 없음",
                        VALID.replace("IT: https://news.example.com/it.xml", "IT:"),
                        "sources[sample].feeds.IT: 필수 항목"),
                Arguments.of("perCategory 0",
                        VALID.replace("perCategory: 3", "perCategory: 0"),
                        "perCategory: 1 이상"),
                Arguments.of("siteArchiveDays 0",
                        VALID.replace("siteArchiveDays: 7", "siteArchiveDays: 0"),
                        "retention.siteArchiveDays: 1 이상"),
                Arguments.of("newsDays 음수",
                        VALID.replace("newsDays: 0", "newsDays: -1"),
                        "retention.newsDays: 0 이상"),
                Arguments.of("clusterThreshold 1 초과",
                        VALID.replace("clusterThreshold: 0.5", "clusterThreshold: 1.5"),
                        "clusterThreshold: 0보다 크고 1 이하"),
                Arguments.of("정규식 오류",
                        VALID.replace("['^\\(샘플\\)\\s*']", "['(닫히지 않음']"),
                        "sources[sample].creditPatterns[0]: 정규식"),
                Arguments.of("feeds에 없는 IT 필터 카테고리",
                        VALID.replace("itKeywordFilterFeeds: [IT]", "itKeywordFilterFeeds: [GLOBAL]"),
                        "sources[sample].itKeywordFilterFeeds: feeds에 없는 카테고리입니다: GLOBAL"),
                Arguments.of("알 수 없는 카테고리 키",
                        VALID.replace("IT: https", "TECH: https"),
                        "YAML을 해석할 수 없습니다"),
                Arguments.of("알 수 없는 설정 항목(오타)",
                        VALID.replace("perCategory: 3", "perCategory: 3\nperCatgory: 3"),
                        "YAML을 해석할 수 없습니다"),
                Arguments.of("언론사 없음",
                        VALID.substring(0, VALID.indexOf("sources:")) + "sources: []\n",
                        "sources: 언론사가 하나 이상"),
                Arguments.of("중복된 언론사 id",
                        VALID.replace("itKeywords:", """
                                  - id: sample
                                    name: 다른일보
                                    domains: [example.com]
                                    bodySelector: "div"
                                    feeds:
                                      POLITICS: https://other.example.com/politics.xml
                                itKeywords:"""),
                        "sources[1].id: 중복된 언론사 id입니다: sample"),
                Arguments.of("domains 누락",
                        VALID.replace("    domains: [example.com]\n", ""),
                        "sources[sample].domains: 도메인이 하나 이상"),
                Arguments.of("도메인 형식 오류",
                        VALID.replace("domains: [example.com]", "domains: [\"https://example.com\"]"),
                        "sources[sample].domains[0]: 도메인 형식이 아닙니다"),
                Arguments.of("피드 호스트가 domains 밖",
                        VALID.replace("https://news.example.com/it.xml", "https://news.other.com/it.xml"),
                        "sources[sample].feeds.IT: 피드 호스트가 domains에 속하지 않습니다: news.other.com"),
                Arguments.of("siteUrl이 http(s)가 아님",
                        VALID.replace("https://example.github.io/news/", "example.github.io/news"),
                        "siteUrl: http(s) URL만"),
                Arguments.of("호스트별 간격 음수",
                        VALID.replace("requestDelayMillis: 0", "requestDelayMillis: 0\nhostDelayMillis: {news.example.com: -1}"),
                        "hostDelayMillis.news.example.com: 0 이상"),
                Arguments.of("빈 파일", "", "설정이 비어 있습니다"));
    }

    @Test
    void 없는_파일을_지정하면_예외를_던진다() {
        Path missing = tempDir.resolve("missing.yml");

        assertThatThrownBy(() -> ConfigLoader.load(missing))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("파일이 없습니다");
    }

    private Path write(String yaml) throws IOException {
        return Files.writeString(Files.createTempFile(tempDir, "sources", ".yml"), yaml);
    }
}
