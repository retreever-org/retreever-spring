package dev.retreever.boot;

import dev.retreever.config.RetreeverContextPathProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class RetreeverUiLocationResolverTest {

    @Test
    void resolvesRelativePathWhenServerPortIsUnavailable() {
        MockEnvironment environment = new MockEnvironment();
        RetreeverUiLocationResolver resolver = resolver(environment, null);

        assertThat(resolver.resolve(environment, null)).isEqualTo("/retreever");
    }

    @Test
    void resolvesAbsoluteUrlFromBoundServerPort() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.servlet.context-path", "/api")
                .withProperty("spring.mvc.servlet.path", "/app");
        RetreeverUiLocationResolver resolver = resolver(environment, null);

        assertThat(resolver.resolve(environment, 8080)).isEqualTo("http://localhost:8080/api/app/retreever");
    }

    @Test
    void usesConfiguredAddressAndHttpsWhenAvailable() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.address", "docs.retreever.dev")
                .withProperty("server.ssl.enabled", "true");
        RetreeverUiLocationResolver resolver = resolver(environment, null);

        assertThat(resolver.resolve(environment, 443)).isEqualTo("https://docs.retreever.dev/retreever");
    }

    @Test
    void mapsWildcardBindAddressesBackToLocalhost() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.address", "0.0.0.0");
        RetreeverUiLocationResolver resolver = resolver(environment, null);

        assertThat(resolver.resolve(environment, 8080)).isEqualTo("http://localhost:8080/retreever");
    }

    @Test
    void usesConfiguredRetreeverContextPathWhenPresent() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.servlet.context-path", "/api");
        RetreeverUiLocationResolver resolver = resolver(environment, "/dist-prod/retreever");

        assertThat(resolver.resolve(environment, null)).isEqualTo("/dist-prod/retreever");
    }

    private RetreeverUiLocationResolver resolver(MockEnvironment environment, String configuredContextPath) {
        RetreeverContextPathProperties properties = new RetreeverContextPathProperties();
        properties.setContextPath(configuredContextPath);
        return new RetreeverUiLocationResolver(new RetreeverBasePathResolver(properties, environment));
    }
}
