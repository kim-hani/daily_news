package dailynews.render;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * 산출물 경로를 한곳에서 정한다(plan §4.3). 경로는 날짜로만 정해지므로 같은 날 다시 실행하면 같은 파일을 덮어쓴다(FR-7.4).
 *
 * @param root 출력 루트(저장소 루트, 테스트에서는 임시 폴더)
 */
public record OutputPaths(Path root) {

    public Path newsDir() {
        return root.resolve("news");
    }

    /** news/YYYY/MM/YYYY-MM-DD.md */
    public Path markdown(LocalDate date) {
        return root.resolve(markdownRelative(date));
    }

    /** 루트 기준 Markdown 경로(README 링크용, 항상 / 구분) */
    public String markdownRelative(LocalDate date) {
        return "news/%d/%02d/%s.md".formatted(date.getYear(), date.getMonthValue(), date);
    }

    public Path siteDir() {
        return root.resolve("site");
    }

    /** site/index.html(홈) */
    public Path siteIndex() {
        return siteDir().resolve("index.html");
    }

    /** site/today.html(오늘의 뉴스피드, 최신 뉴스레터) */
    public Path siteToday() {
        return siteDir().resolve("today.html");
    }

    /** site/calendar.html(캘린더 뉴스피드) */
    public Path calendarPage() {
        return siteDir().resolve("calendar.html");
    }

    public Path archiveDir() {
        return siteDir().resolve("archive");
    }

    /** site/archive/YYYY-MM-DD.html */
    public Path archivePage(LocalDate date) {
        return archiveDir().resolve(date + ".html");
    }

    /** site/archive.html(옛 주소. calendar.html로 리다이렉트) */
    public Path archiveIndex() {
        return siteDir().resolve("archive.html");
    }

    public Path assetsDir() {
        return siteDir().resolve("assets");
    }

    /** site/assets/fonts(Pretendard 서브셋) */
    public Path fontsDir() {
        return assetsDir().resolve("fonts");
    }

    public Path dataDir() {
        return siteDir().resolve("data");
    }

    /** site/data/archive.json(보관 날짜 목록, 날짜별 페이지의 이전/다음 이동용) */
    public Path archiveJson() {
        return dataDir().resolve("archive.json");
    }

    public Path readme() {
        return root.resolve("README.md");
    }
}
