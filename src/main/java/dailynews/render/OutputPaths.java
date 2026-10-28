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

    public Path siteIndex() {
        return siteDir().resolve("index.html");
    }

    public Path archiveDir() {
        return siteDir().resolve("archive");
    }

    /** site/archive/YYYY-MM-DD.html */
    public Path archivePage(LocalDate date) {
        return archiveDir().resolve(date + ".html");
    }

    /** site/archive.html(날짜 목록) */
    public Path archiveIndex() {
        return siteDir().resolve("archive.html");
    }

    public Path assetsDir() {
        return siteDir().resolve("assets");
    }

    public Path readme() {
        return root.resolve("README.md");
    }
}
