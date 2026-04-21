package dev.retreever.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "retreever.dev")
public class RetreeverCorsProperties implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RetreeverCorsProperties.class);

    private final boolean localDevelopmentRuntime;
    private List<String> allowCrossOrigin = new ArrayList<>();

    public RetreeverCorsProperties() {
        this(new RetreeverRuntimeMode().isLocalDevelopmentRuntime());
    }

    RetreeverCorsProperties(boolean localDevelopmentRuntime) {
        this.localDevelopmentRuntime = localDevelopmentRuntime;
    }

    public List<String> getAllowCrossOrigin() {
        return allowCrossOrigin;
    }

    public void setAllowCrossOrigin(List<String> allowCrossOrigin) {
        this.allowCrossOrigin = allowCrossOrigin != null ? new ArrayList<>(allowCrossOrigin) : new ArrayList<>();
    }

    public boolean isEnabled() {
        return localDevelopmentRuntime && !allowCrossOrigin.isEmpty();
    }

    public boolean isAllowed(String origin) {
        return allowCrossOrigin.contains(origin);
    }

    @Override
    public void afterPropertiesSet() {
        List<String> normalized = allowCrossOrigin.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();

        if (normalized.stream().anyMatch("*"::equals)) {
            throw new IllegalStateException(
                    "'retreever.dev.allow-cross-origin' must list explicit origins when credentials are enabled."
            );
        }

        this.allowCrossOrigin = new ArrayList<>(normalized);

        if (!localDevelopmentRuntime && !this.allowCrossOrigin.isEmpty()) {
            log.info(
                    "Ignoring 'retreever.dev.allow-cross-origin' because Retreever is running from a packaged dependency."
            );
        }
    }
}
