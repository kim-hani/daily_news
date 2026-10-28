package dailynews;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class DailyNewsAppTest {

    @Test
    void main은_예외_없이_끝난다() {
        assertThatCode(() -> DailyNewsApp.main(new String[0])).doesNotThrowAnyException();
    }
}
