package dailynews.render;

import java.util.Locale;
import java.util.Map;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * classpath {@code templates/}의 Thymeleaf 템플릿을 Spring 없이 처리한다.
 * 템플릿에서는 {@code th:text}만 써서 외부 텍스트(RSS 제목·요약)를 항상 이스케이프한다(plan §8 XSS).
 */
final class Templates {

    private static final TemplateEngine ENGINE = createEngine();

    private Templates() {
    }

    static String process(String template, Map<String, Object> variables) {
        Context context = new Context(Locale.KOREAN);
        context.setVariables(variables);
        return ENGINE.process(template, context);
    }

    private static TemplateEngine createEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
