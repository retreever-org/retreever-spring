package dev.retreever.boot;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

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
        return resolve(context.getEnvironment(), resolveWebServerPort(context));
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

    private Integer resolveWebServerPort(ApplicationContext context) {
        try {
            Method getWebServer = context.getClass().getMethod("getWebServer");
            Object webServer = getWebServer.invoke(context);
            if (webServer == null) {
                return null;
            }

            Method getPort = webServer.getClass().getMethod("getPort");
            Object port = getPort.invoke(webServer);
            return port instanceof Integer value ? value : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

}
