package dev.retreever.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetreeverDocumentationExclusionPropertiesTest {

    @Test
    void excludesExactPathsAndAntPatternsFromSingleSkipList() {
        RetreeverDocumentationExclusionProperties properties = new RetreeverDocumentationExclusionProperties();
        properties.setSkip(List.of("/internal/reindex", "/users/{userId}", "/admin/**"));

        assertThat(properties.excludes("/internal/reindex")).isTrue();
        assertThat(properties.excludes("/users/42")).isTrue();
        assertThat(properties.excludes("/admin/users/42")).isTrue();
        assertThat(properties.excludes("/internal/reindex/extra")).isFalse();
        assertThat(properties.excludes("/public/status")).isFalse();
    }

    @Test
    void normalizesPathsBeforeMatching() {
        RetreeverDocumentationExclusionProperties properties = new RetreeverDocumentationExclusionProperties();
        properties.setSkip(List.of("internal/reindex/", "admin/**"));

        assertThat(properties.excludes("/internal/reindex")).isTrue();
        assertThat(properties.excludes("/admin/users")).isTrue();
    }

    @Test
    void excludesRegexPatternsWithExplicitRegexPrefix() {
        RetreeverDocumentationExclusionProperties properties = new RetreeverDocumentationExclusionProperties();
        properties.setSkip(List.of("regex:^/reports/[0-9]+/export$"));

        assertThat(properties.excludes("/reports/42/export")).isTrue();
        assertThat(properties.excludes("/reports/current/export")).isFalse();
    }

    @Test
    void rejectsInvalidRegexPatterns() {
        RetreeverDocumentationExclusionProperties properties = new RetreeverDocumentationExclusionProperties();
        properties.setSkip(List.of("regex:["));

        assertThatThrownBy(() -> properties.excludes("/anything"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Retreever docs skip regex");
    }
}
