package dailynews;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * src/test/resources의 fixture 파일을 읽는다.
 */
public final class Fixtures {

    private Fixtures() {
    }

    /** 예: {@code bytes("/fixtures/yonhap/rss.xml")} */
    public static byte[] bytes(String resourcePath) {
        try (InputStream in = Fixtures.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("fixture가 없습니다: " + resourcePath);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String text(String resourcePath) {
        return new String(bytes(resourcePath), StandardCharsets.UTF_8);
    }
}
