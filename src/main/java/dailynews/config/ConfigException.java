package dailynews.config;

/**
 * 설정 파일을 읽을 수 없거나 값이 잘못되었을 때 던진다. 메시지에 문제가 된 항목의 위치를 담는다.
 */
public class ConfigException extends RuntimeException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
