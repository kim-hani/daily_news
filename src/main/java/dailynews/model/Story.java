package dailynews.model;

import java.util.List;

/**
 * 같은 사안을 다룬 기사 묶음.
 *
 * @param representative 가장 먼저 보도된 대표 기사
 * @param related        대표 기사를 뺀, 다른 언론사의 같은 사안 기사(언론사마다 1건)
 * @param outletCount    보도한 서로 다른 언론사 수
 */
public record Story(Category category, Article representative, List<Article> related,
                    int outletCount, double score) {

    public Story {
        related = List.copyOf(related);
    }
}
