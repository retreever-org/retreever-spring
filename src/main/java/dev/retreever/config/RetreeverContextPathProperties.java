package dev.retreever.config;

import dev.retreever.auth.RetreeverAuthSupport;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "retreever")
public class RetreeverContextPathProperties {

    private String contextPath;

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = normalize(contextPath);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.endsWith(RetreeverAuthSupport.RETREEVER_BASE_PATH)) {
            normalized = normalized.substring(0, normalized.length() - RetreeverAuthSupport.RETREEVER_BASE_PATH.length());
        }

        return "/".equals(normalized) ? null : normalized;
    }
}
