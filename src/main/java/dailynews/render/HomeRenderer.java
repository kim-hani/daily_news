package dailynews.render;

import dailynews.model.Newsletter;
import dailynews.render.Chrome.Nav;
import java.io.IOException;
import java.util.Map;

/**
 * 홈 {@code site/index.html}을 만든다(spec FR-2, plan §4.2). 히어로 두 줄과 버튼 글자는 고정 문구라 템플릿에 있고,
 * 여기서는 최신 뉴스레터 날짜와 링크만 넘긴다.
 */
public final class HomeRenderer {

    private final String repositoryUrl;

    public HomeRenderer(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String render(Newsletter latest) {
        Chrome chrome = new Chrome("Daily News · 매일 정오의 한국 뉴스", "매일 낮 12시, 정치·사회·경제·IT·글로벌 주요 뉴스를 "
                + "언론사 원문 링크와 함께 정리합니다. 최신 뉴스레터: " + Formats.dateLabel(latest.date()), "", Nav.HOME,
                Formats.generatedAt(latest.generatedAt()), repositoryUrl);
        HomePage home = new HomePage(chrome, Formats.dateLabel(latest.date()), latest.date().toString(), "today.html",
                "calendar.html");
        return Templates.process("home", Map.of("home", home));
    }

    public void write(Newsletter latest, OutputPaths paths) throws IOException {
        Formats.write(paths.siteIndex(), render(latest));
    }

    /**
     * @param latestLabel "2026-11-02 (월)" → "Latest · 2026-11-02 (월)"(FR-2.3)
     * @param isoDate     {@code <time datetime>}
     */
    public record HomePage(Chrome chrome, String latestLabel, String isoDate, String todayHref, String calendarHref) {
    }
}
