package dev.retreever.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "retreever.studio")
public class RetreeverStudioProperties {

    public static final String STORAGE_IN_MEMORY = "in-memory";
    public static final String STORAGE_INDEXED_DB = "indexed-db";

    private static final Logger log = LoggerFactory.getLogger(RetreeverStudioProperties.class);

    private String storage = STORAGE_IN_MEMORY;

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = normalizeStorage(storage);
    }

    private String normalizeStorage(String value) {
        if (!StringUtils.hasText(value)) {
            return STORAGE_IN_MEMORY;
        }

        String normalized = value.trim().toLowerCase();
        if (STORAGE_IN_MEMORY.equals(normalized) || STORAGE_INDEXED_DB.equals(normalized)) {
            return normalized;
        }

        log.warn(
                "Invalid Retreever studio storage mode '{}'. Falling back to '{}'.",
                value,
                STORAGE_IN_MEMORY
        );
        return STORAGE_IN_MEMORY;
    }
}
