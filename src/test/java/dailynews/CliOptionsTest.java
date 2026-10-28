package dailynews;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CliOptionsTest {

    @Test
    void 옵션이_없으면_기본값이다() {
        CliOptions options = CliOptions.parse(new String[0]);

        assertThat(options.date()).isNull();
        assertThat(options.root()).isEqualTo(Path.of("."));
        assertThat(options.config()).isNull();
        assertThat(options.dryRun()).isFalse();
    }

    @Test
    void 모든_옵션을_읽는다() {
        CliOptions options = CliOptions.parse(
                new String[] {"--date", "2026-10-28", "--root", "out", "--config", "my.yml", "--dry-run"});

        assertThat(options).isEqualTo(new CliOptions(LocalDate.of(2026, 10, 28), Path.of("out"), Path.of("my.yml"), true));
    }

    @Test
    void 알_수_없는_옵션_값_없는_옵션_잘못된_날짜는_예외다() {
        assertThatThrownBy(() -> CliOptions.parse(new String[] {"--verbose"})).hasMessageContaining("알 수 없는 옵션");
        assertThatThrownBy(() -> CliOptions.parse(new String[] {"--root"})).hasMessageContaining("값이 없습니다");
        assertThatThrownBy(() -> CliOptions.parse(new String[] {"--date", "--dry-run"})).hasMessageContaining("값이 없습니다");
        assertThatThrownBy(() -> CliOptions.parse(new String[] {"--date", "2026/10/28"})).hasMessageContaining("YYYY-MM-DD");
    }
}
