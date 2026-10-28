package dailynews.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void 카테고리는_정치_사회_경제_IT_글로벌_순서다() {
        assertThat(Category.values()).containsExactly(
                Category.POLITICS, Category.SOCIETY, Category.ECONOMY, Category.IT, Category.GLOBAL);
    }

    @Test
    void 라벨과_slug는_순서대로_정해져_있다() {
        assertThat(Arrays.stream(Category.values()).map(Category::label))
                .containsExactly("정치", "사회", "경제", "IT", "글로벌");
        assertThat(Arrays.stream(Category.values()).map(Category::slug))
                .containsExactly("politics", "society", "economy", "it", "global");
    }

    @Test
    void 모든_카테고리에_이모지가_있다() {
        assertThat(Category.values()).allSatisfy(category -> assertThat(category.emoji()).isNotBlank());
    }
}
