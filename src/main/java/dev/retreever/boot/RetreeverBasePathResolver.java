package dev.retreever.boot;

import dev.retreever.auth.RetreeverAuthSupport;
import dev.retreever.config.RetreeverContextPathProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RetreeverBasePathResolver {

    public static final String FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";

    private final RetreeverContextPathProperties properties;
    private final Environment environment;

    public RetreeverBasePathResolver(RetreeverContextPathProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    public String resolveContextPath(HttpServletRequest request) {
        if (StringUtils.hasText(properties.getContextPath())) {
            return normalizeContextPath(properties.getContextPath());
        }

        return normalizeContextPath(joinPathSegments(
                request.getHeader(FORWARDED_PREFIX_HEADER),
                request.getContextPath(),
                environment.getProperty("spring.mvc.servlet.path")
        ));
    }

    public String resolve(HttpServletRequest request) {
        return resolveRetreeverUiPath(request);
    }

    public String resolveContextPath(Environment environment) {
        if (StringUtils.hasText(properties.getContextPath())) {
            return normalizeContextPath(properties.getContextPath());
        }

        return normalizeContextPath(joinPathSegments(
                environment.getProperty("server.servlet.context-path"),
                environment.getProperty("spring.mvc.servlet.path")
        ));
    }

    public String resolve(Environment environment) {
        return resolveRetreeverUiPath(environment);
    }

    public String resolveRetreeverUiPath(HttpServletRequest request) {
        return joinPathSegments(resolveContextPath(request), RetreeverAuthSupport.RETREEVER_BASE_PATH);
    }

    public String resolveRetreeverUiPath(Environment environment) {
        return joinPathSegments(resolveContextPath(environment), RetreeverAuthSupport.RETREEVER_BASE_PATH);
    }

    String resolveFilterBasePath() {
        return joinPathSegments(
                environment.getProperty("spring.mvc.servlet.path"),
                RetreeverAuthSupport.RETREEVER_BASE_PATH
        );
    }

    public String resolveCookiePath(HttpServletRequest request) {
        return resolveRetreeverUiPath(request);
    }

    static String joinPathSegments(String... segments) {
        StringBuilder path = new StringBuilder();

        for (String segment : segments) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }

            String normalized = trimSlashes(segment.trim());
            if (normalized.isEmpty()) {
                continue;
            }

            path.append('/').append(normalized);
        }

        return path.isEmpty() ? "/" : path.toString();
    }

    private static String trimSlashes(String value) {
        int start = 0;
        int end = value.length();

        while (start < end && value.charAt(start) == '/') {
            start++;
        }

        while (end > start && value.charAt(end - 1) == '/') {
            end--;
        }

        return value.substring(start, end);
    }

    private static String normalizeContextPath(String path) {
        return "/".equals(path) ? "" : path;
    }
}
