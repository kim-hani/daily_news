package dailynews.render;

import dailynews.model.Newsletter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 네트워크 없이 샘플 뉴스레터로 미리보기 사이트를 만든다(plan §9.2). {@code ./gradlew previewSite}로 실행하고
 * 실행 결과에 나오는 {@code jwebserver -d <절대 경로> -p 8000}으로 띄워 확인한다(jwebserver는 상대 경로를 받지 않는다).
 * <ul>
 *   <li>2026-10-15 ~ 2026-11-02(두 달). 10-18·10-25는 뉴스레터가 없는 날이다.</li>
 *   <li>IT 카테고리가 비어 있고, 정치 기사 하나는 {@code <script>}가 든 제목이다.</li>
 *   <li>홀수 번째 날에는 수집 실패 피드가 있다.</li>
 *   <li>2026-10-14는 전환 기간의 옛 디자인 페이지다(plan §3.3).</li>
 * </ul>
 */
public final class PreviewSite {

    static final String REPOSITORY = "https://github.com/kim-hani/daily_news";
    static final LocalDate FIRST = LocalDate.of(2026, 10, 15);
    static final LocalDate LAST = LocalDate.of(2026, 11, 2);
    static final LocalDate OLD_DESIGN = LocalDate.of(2026, 10, 14);
    static final Set<LocalDate> SKIPPED = Set.of(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 25));

    private PreviewSite() {
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : "build/preview");
        Path site = build(root);
        // Windows 콘솔(CP949)에서 한글이 깨지므로 안내 문구는 영어로 쓴다. jwebserver는 절대 경로만 받는다.
        System.out.println("Preview site: " + site.toAbsolutePath());
        System.out.println("Serve: jwebserver -d \"" + site.toAbsolutePath() + "\" -p 8000");
    }

    /** @return 만든 site 폴더 */
    static Path build(Path root) throws IOException {
        OutputPaths paths = new OutputPaths(root);
        deleteContents(paths.siteDir());

        Files.createDirectories(paths.archiveDir());
        try (InputStream old = PreviewSite.class.getResourceAsStream("/fixtures/archive/old-design.html")) {
            Files.copy(old, paths.archivePage(OLD_DESIGN), StandardCopyOption.REPLACE_EXISTING);
        }

        FeedRenderer feed = new FeedRenderer(REPOSITORY);
        List<Newsletter> newsletters = new ArrayList<>();
        int variant = 0;
        for (LocalDate date = FIRST; !date.isAfter(LAST); date = date.plusDays(1)) {
            if (!SKIPPED.contains(date)) {
                newsletters.add(TestNewsletters.on(date, variant, variant % 2 == 1));
                variant++;
            }
        }
        for (Newsletter newsletter : newsletters) {
            feed.write(newsletter, paths);       // 마지막(최신)이 today.html이 된다
        }
        Newsletter latest = newsletters.get(newsletters.size() - 1);
        new CalendarRenderer(REPOSITORY, 30).write(paths, latest.generatedAt());
        new HomeRenderer(REPOSITORY).write(latest, paths);
        return paths.siteDir();
    }

    /** 폴더 자체는 남긴다. Windows에서는 jwebserver 등이 열어 둔 폴더를 지울 수 없어서다. */
    private static void deleteContents(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).filter(file -> !file.equals(dir)).toList()) {
                Files.delete(file);
            }
        }
    }
}
