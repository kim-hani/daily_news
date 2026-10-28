package dailynews.model;

import java.util.List;

/**
 * 실행 지표(NFR-8). 로그와 Markdown 헤더에 남긴다.
 *
 * @param collected    수집한 기사 수
 * @param afterDedup   정제·중복 제거 후 기사 수
 * @param selected     선정한 기사 수
 * @param failedFeeds  실패한 피드(예: "yonhap/POLITICS")
 * @param bodyOk       본문 기반 요약 수
 * @param bodyFallback RSS 대체 또는 요약 없음 수
 */
public record RunStats(int collected, int afterDedup, int selected,
                       List<String> failedFeeds, int bodyOk, int bodyFallback) {

    public RunStats {
        failedFeeds = List.copyOf(failedFeeds);
    }
}
