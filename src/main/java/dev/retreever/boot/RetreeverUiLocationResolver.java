package dev.retreever.boot;

import dev.retreever.auth.RetreeverAuthSupport;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Resolves the most useful location to show in startup logs for the Retreever UI.
 * Falls back to a relative path when the bound server address is not available.
 */
final class RetreeverUiLocationResolver {

    String resolve(ApplicationContext context) {
        Integer port = null;

        if (context instanceof WebServerApplicationContext webContext && webContext.getWebServer() != null) {
            port = webContext.getWebServer().getPort();
        }

        return resolve(context.getEnvironment(), port);
    }

    String resolve(Environment environment, Integer port) {
        String path = resolveRelativePath(environment);

        if (port == null || port <= 0) {
            return path;
        }

        return resolveBaseUrl(environment, port) + path;
    }

    String resolveRelativePath(Environment environment) {
        return joinPathSegments(
                environment.getProperty("server.servlet.context-path"),
                environment.getProperty("spring.mvc.servlet.path"),
                RetreeverAuthSupport.RETREEVER_BASE_PATH
        );
    }

    private String resolveBaseUrl(Environment environment, int port) {
        String scheme = environment.getProperty("server.ssl.enabled", Boolean.class, false) ? "https" : "http";
        String host = normalizeHost(environment.getProperty("server.address"));

        if (isDefaultPort(scheme, port)) {
            return scheme + "://" + host;
        }

        return scheme + "://" + host + ":" + port;
    }

    private boolean isDefaultPort(String scheme, int port) {
        return ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
    }

    private String normalizeHost(String configuredHost) {
        if (!StringUtils.hasText(configuredHost)) {
            return "localhost";
        }

        String host = configuredHost.trim();
        if ("0.0.0.0".equals(host) || "::".equals(host) || "0:0:0:0:0:0:0:0".equals(host)) {
            return "localhost";
        }

        if (host.contains(":") && !(host.startsWith("[") && host.endsWith("]"))) {
            return "[" + host + "]";
        }

        return host;
    }

    private String joinPathSegments(String... segments) {
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

    private String trimSlashes(String value) {
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
}
