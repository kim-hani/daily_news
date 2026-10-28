package dailynews.render;

import dailynews.model.Article;
import dailynews.model.Category;
import dailynews.model.NewsItem;
import dailynews.model.Newsletter;
import dailynews.model.RunStats;
import dailynews.model.Story;
import dailynews.model.Summary;
import dailynews.model.SummaryOrigin;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 저장소 아카이브용 Markdown 뉴스레터를 만든다(FR-7.0, FR-7.1). 형식은 plan §6.3을 따른다.
 * 모든 기사에 원문 URL 문자열을 쓰고, 제목·요약의 Markdown 특수문자는 이스케이프한다.
 */
public final class MarkdownRenderer {

    private static final Pattern SPECIAL = Pattern.compile("([\\\\`*_\\[\\]#<>|])");

    public String render(Newsletter newsletter) {
        StringBuilder md = new StringBuilder();
        RunStats stats = newsletter.stats();
        md.append("# 📰 Daily News · ").append(Formats.dateLabel(newsletter.date())).append("\n\n");
        md.append("> 수집: ").append(String.join(" · ", newsletter.sourceNames()))
                .append(" | 수집 ").append(stats.collected()).append("건 → 중복 제거 ").append(stats.afterDedup())
                .append("건 → 선정 ").append(stats.selected()).append("건\n");
        if (!newsletter.keywords().isEmpty()) {
            md.append(">\n> 🔑 오늘의 키워드: ")
                    .append(newsletter.keywords().stream().map(MarkdownRenderer::escape).collect(Collectors.joining(" · ")))
                    .append('\n');
        }
        if (!stats.failedFeeds().isEmpty()) {
            md.append(">\n> ⚠️ 수집하지 못한 피드: ").append(String.join(", ", stats.failedFeeds())).append('\n');
        }

        for (Category category : Category.values()) {
            blankLine(md);
            md.append("## ").append(category.emoji()).append(' ').append(category.label()).append("\n\n");
            List<NewsItem> items = newsletter.sections().getOrDefault(category, List.of());
            if (items.isEmpty()) {
                md.append("_오늘은 선정된 기사가 없습니다._\n");
            }
            for (int i = 0; i < items.size(); i++) {
                appendItem(md, i + 1, items.get(i));
            }
        }

        blankLine(md);
        md.append("---\n\n")
                .append("요약은 각 기사 원문의 앞부분(최대 200자)을 그대로 인용한 것이며 저작권은 각 언론사에 있습니다.  \n")
                .append("생성: ").append(Formats.generatedAt(newsletter.generatedAt())).append('\n');
        return md.toString();
    }

    public Path write(Newsletter newsletter, OutputPaths paths) throws IOException {
        Path path = paths.markdown(newsletter.date());
        Formats.write(path, render(newsletter));
        return path;
    }

    private static void appendItem(StringBuilder md, int rank, NewsItem item) {
        Story story = item.story();
        Article article = story.representative();
        blankLine(md);
        md.append("### ").append(rank).append(". ").append(escape(article.title())).append("\n\n");
        md.append("- 언론사: ").append(article.source().name()).append(" · ").append(Formats.time(article.publishedAt()));
        if (story.outletCount() > 1) {
            md.append(" · 🗞 ").append(story.outletCount()).append("개 언론사 보도");
        }
        md.append("\n- 원문: ").append(article.url()).append('\n');

        Summary summary = item.summary();
        if (summary.origin() != SummaryOrigin.NONE) {
            md.append("\n> ").append(escape(summary.text()));
            if (summary.origin() == SummaryOrigin.RSS) {
                md.append(" _(RSS 요약)_");
            }
            md.append('\n');
        }
        if (!story.related().isEmpty()) {
            md.append("\n- 같은 사안: ").append(story.related().stream()
                    .map(related -> "[" + related.source().name() + "](" + linkTarget(related) + ")")
                    .collect(Collectors.joining(" · "))).append('\n');
        }
    }

    /** 앞 블록과의 사이에 빈 줄이 정확히 하나 있게 한다. */
    private static void blankLine(StringBuilder md) {
        while (!md.toString().endsWith("\n\n")) {
            md.append('\n');
        }
    }

    /** 링크 대상의 괄호는 Markdown 문법과 겹치지 않게 인코딩한다. */
    private static String linkTarget(Article article) {
        return article.url().toString().replace("(", "%28").replace(")", "%29");
    }

    static String escape(String text) {
        return SPECIAL.matcher(text).replaceAll("\\\\$1");
    }
}
