package dev.retreever.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetreeverCorsPropertiesTest {

    @Test
    void enablesDevCorsWhenOriginsAreConfiguredForLocalDevelopmentRuntime() throws Exception {
        RetreeverCorsProperties properties = new RetreeverCorsProperties(true);
        properties.setAllowCrossOrigin(List.of("http://localhost:5173"));

        properties.afterPropertiesSet();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isAllowed("http://localhost:5173")).isTrue();
    }

    @Test
    void disablesDevCorsWhenRetreeverRunsFromPackagedDependency() throws Exception {
        RetreeverCorsProperties properties = new RetreeverCorsProperties(false);
        properties.setAllowCrossOrigin(List.of("http://localhost:5173"));

        properties.afterPropertiesSet();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.isAllowed("http://localhost:5173")).isTrue();
    }

    @Test
    void rejectsWildcardOrigins() {
        RetreeverCorsProperties properties = new RetreeverCorsProperties(true);
        properties.setAllowCrossOrigin(List.of("*"));

        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retreever.dev.allow-cross-origin");
    }
}
