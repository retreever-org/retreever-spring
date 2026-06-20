package dev.retreever.boot;

import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Resolves the most useful location to show in startup logs for the Retreever UI.
 * Falls back to a relative path when the bound server address is not available.
 */
final class RetreeverUiLocationResolver {

    private final RetreeverBasePathResolver basePathResolver;

    RetreeverUiLocationResolver(RetreeverBasePathResolver basePathResolver) {
        this.basePathResolver = basePathResolver;
    }

    String resolve(ApplicationContext context) {
        Integer port = null;

        if (context instanceof WebServerApplicationContext webContext && webContext.getWebServer() != null) {
            port = webContext.getWebServer().getPort();
        }

        return resolve(context.getEnvironment(), port);
    }

    String resolve(Environment environment, Integer port) {
        String path = basePathResolver.resolve(environment);

        if (port == null || port <= 0) {
            return path;
        }

        return resolveBaseUrl(environment, port) + path;
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

}
