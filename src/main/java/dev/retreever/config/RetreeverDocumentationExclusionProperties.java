/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@ConfigurationProperties(prefix = "retreever.docs")
@Component
public class RetreeverDocumentationExclusionProperties {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private List<String> skip = List.of();

    public List<String> getSkip() {
        return skip;
    }

    public void setSkip(List<String> skip) {
        this.skip = skip != null ? skip : List.of();
    }

    public boolean excludes(String path) {
        String normalizedPath = normalizePath(path);
        if (!StringUtils.hasText(normalizedPath)) {
            return false;
        }

        return skip.stream()
                .filter(StringUtils::hasText)
                .anyMatch(entry -> matches(entry, normalizedPath));
    }

    private boolean matches(String entry, String normalizedPath) {
        String value = entry.trim();
        if (isRegex(value)) {
            return matchesRegex(value, normalizedPath);
        }

        String normalizedEntry = normalizePath(value);
        return isAntPattern(normalizedEntry)
                ? pathMatcher.match(normalizedEntry, normalizedPath)
                : normalizedPath.equals(normalizedEntry);
    }

    private boolean isRegex(String value) {
        return value.startsWith("regex:");
    }

    private boolean matchesRegex(String value, String normalizedPath) {
        String expression = value.substring("regex:".length()).trim();
        if (!StringUtils.hasText(expression)) {
            return false;
        }

        try {
            return Pattern.compile(expression).matcher(normalizedPath).matches();
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("Invalid Retreever docs skip regex: " + expression, ex);
        }
    }

    private boolean isAntPattern(String path) {
        return pathMatcher.isPattern(path);
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }

        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        normalized = normalized.replaceAll("//+", "/");

        if (normalized.length() > 1 && normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
