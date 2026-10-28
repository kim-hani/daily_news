package dailynews.config;

import dailynews.model.Source;
import java.util.List;

/**
 * 검증을 마친 실행 설정. {@link ConfigLoader}가 만든다.
 *
 * @param requestDelayMillis 같은 호스트에 연속 요청할 때 간격
 * @param windowHours        최근 몇 시간 안의 기사만 쓸지
 * @param perCategory        카테고리별 선정 기사 수
 * @param clusterThreshold   같은 사안으로 묶는 제목 유사도 기준(0 초과 1 이하)
 * @param stopwords          오늘의 키워드에서 뺄 단어
 * @param siteUrl            GitHub Pages 주소(README에 표시)
 * @param repositoryUrl      저장소 주소(아카이브 페이지의 Markdown 링크)
 */
public record AppConfig(String userAgent, int requestDelayMillis, int timeoutSeconds, int windowHours,
                        int perCategory, double clusterThreshold, Retention retention,
                        List<Source> sources, List<String> itKeywords, List<String> stopwords,
                        String siteUrl, String repositoryUrl) {

    public AppConfig {
        sources = List.copyOf(sources);
        itKeywords = List.copyOf(itKeywords);
        stopwords = List.copyOf(stopwords);
    }

    /**
     * 보관 정책(FR-7.5).
     *
     * @param siteArchiveDays 웹 날짜별 페이지 보관 일수(실행일 포함, 1 이상)
     * @param newsDays        Markdown 보관 일수(0이면 무기한)
     */
    public record Retention(int siteArchiveDays, int newsDays) {
    }
}
