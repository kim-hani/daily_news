package dailynews.summary;

import dailynews.model.Source;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * 기사 HTML에서 본문 문단 텍스트만 꺼낸다(FR-5.1).
 * 언론사별 {@code bodySelector}로 본문 요소를 고르고 사진·설명·광고·관련기사·저작권 같은 잡음 요소를 지운다.
 * 문단은 블록 요소와 {@code <br>}을 기준으로 나눈다(동아일보처럼 {@code <p>} 없이 {@code <br>}로 나눈 본문 포함).
 */
public final class BodyExtractor {

    private static final String NOISE = String.join(", ",
            "script", "style", "noscript", "iframe", "figure", "figcaption", "picture", "aside", "table",
            "em.img_desc", ".caption", ".photo-group", ".art_photo", ".editor-subtitle", ".strapline", ".tit-sub",
            ".related-zone", ".txt-copyright", ".link-box", "[class*=banner]", "[class~=(^|\\s)ads?([-_\\s]|$)]");
    /** 본문 컨테이너 바로 아래의 굵은 글씨는 중간 제목이다(예: 동아일보 "● …"). */
    private static final Set<String> HEADING_TAGS = Set.of("strong", "b", "h2", "h3", "h4");
    private static final Pattern CHARSET = Pattern.compile("(?i)charset=\"?([\\w-]+)");
    private static final Pattern SPACES = Pattern.compile("(?U)\\s+");

    /**
     * @param contentType 응답 Content-Type. charset이 없으면 HTML의 meta나 BOM으로 판단한다.
     * @return 본문 문단(공백 정리됨). 본문 요소가 없으면 빈 목록
     */
    public List<String> extract(byte[] html, String contentType, URI baseUri, Source source) {
        Matcher charset = CHARSET.matcher(contentType == null ? "" : contentType);
        try {
            Document document = Jsoup.parse(new ByteArrayInputStream(html), charset.find() ? charset.group(1) : null,
                    baseUri.toString());
            return extract(document, source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<String> extract(Document document, Source source) {
        List<String> paragraphs = new ArrayList<>();
        for (Element body : document.select(source.bodySelector())) {
            // selector가 문단 자체를 고르는 경우(연합뉴스 "… > p") 그 문단이 잡음(저작권 문구 등)이면 건너뛴다.
            if (body.is(NOISE)) {
                continue;
            }
            Element copy = body.clone();
            copy.select(NOISE).remove();
            if (!copy.nameIs("p")) {
                copy.children().stream().filter(child -> HEADING_TAGS.contains(child.normalName())).toList()
                        .forEach(Element::remove);
            }
            paragraphs.addAll(paragraphs(copy));
        }
        return paragraphs;
    }

    private static List<String> paragraphs(Element root) {
        StringBuilder text = new StringBuilder();
        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof TextNode textNode) {
                    text.append(textNode.text());
                } else if (node instanceof Element element && (element.nameIs("br") || element.isBlock())) {
                    text.append('\n');
                }
            }

            @Override
            public void tail(Node node, int depth) {
                if (node instanceof Element element && element.isBlock()) {
                    text.append('\n');
                }
            }
        }, root);
        List<String> paragraphs = new ArrayList<>();
        for (String line : text.toString().split("\n")) {
            String paragraph = SPACES.matcher(line).replaceAll(" ").strip();
            if (!paragraph.isEmpty()) {
                paragraphs.add(paragraph);
            }
        }
        return paragraphs;
    }
}
