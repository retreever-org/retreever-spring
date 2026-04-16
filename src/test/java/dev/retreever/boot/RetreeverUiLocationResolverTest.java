package dev.retreever.boot;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class RetreeverUiLocationResolverTest {

    private final RetreeverUiLocationResolver resolver = new RetreeverUiLocationResolver();

    @Test
    void resolvesRelativePathWhenServerPortIsUnavailable() {
        MockEnvironment environment = new MockEnvironment();

        assertThat(resolver.resolve(environment, null)).isEqualTo("/retreever");
    }

    @Test
    void resolvesAbsoluteUrlFromBoundServerPort() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.servlet.context-path", "/api")
                .withProperty("spring.mvc.servlet.path", "/app");

        assertThat(resolver.resolve(environment, 8080)).isEqualTo("http://localhost:8080/api/app/retreever");
    }

    @Test
    void usesConfiguredAddressAndHttpsWhenAvailable() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.address", "docs.retreever.dev")
                .withProperty("server.ssl.enabled", "true");

        assertThat(resolver.resolve(environment, 443)).isEqualTo("https://docs.retreever.dev/retreever");
    }

    @Test
    void mapsWildcardBindAddressesBackToLocalhost() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.address", "0.0.0.0");

        assertThat(resolver.resolve(environment, 8080)).isEqualTo("http://localhost:8080/retreever");
    }
}
