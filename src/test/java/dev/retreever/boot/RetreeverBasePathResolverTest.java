package dev.retreever.boot;

import dev.retreever.config.RetreeverContextPathProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RetreeverBasePathResolverTest {

    @Test
    void defaultsToRetreeverBasePath() {
        RetreeverBasePathResolver resolver = resolver(new MockEnvironment(), null);

        assertThat(resolver.resolveContextPath(new MockHttpServletRequest())).isEqualTo("");
        assertThat(resolver.resolveContextPath(new MockEnvironment())).isEqualTo("");
        assertThat(resolver.resolve(new MockHttpServletRequest())).isEqualTo("/retreever");
        assertThat(resolver.resolve(new MockEnvironment())).isEqualTo("/retreever");
    }

    @Test
    void includesConfiguredContextAndServletPath() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.servlet.context-path", "/user-svc")
                .withProperty("spring.mvc.servlet.path", "/api");

        RetreeverBasePathResolver resolver = resolver(environment, null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/user-svc");

        assertThat(resolver.resolveContextPath(request)).isEqualTo("/user-svc/api");
        assertThat(resolver.resolveContextPath(environment)).isEqualTo("/user-svc/api");
        assertThat(resolver.resolve(request)).isEqualTo("/user-svc/api/retreever");
        assertThat(resolver.resolve(environment)).isEqualTo("/user-svc/api/retreever");
    }

    @Test
    void includesForwardedPrefixForStrippedProxyDeployments() {
        RetreeverBasePathResolver resolver = resolver(new MockEnvironment(), null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RetreeverBasePathResolver.FORWARDED_PREFIX_HEADER, "/dist-prod");

        assertThat(resolver.resolveContextPath(request)).isEqualTo("/dist-prod");
        assertThat(resolver.resolve(request)).isEqualTo("/dist-prod/retreever");
    }

    @Test
    void configuredContextPathAppendsRetreeverSegmentAndWinsOverRequestDerivedValues() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.servlet.context-path", "/user-svc");
        RetreeverBasePathResolver resolver = resolver(environment, "/dist-prod/");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/user-svc");
        request.addHeader(RetreeverBasePathResolver.FORWARDED_PREFIX_HEADER, "/proxy");

        assertThat(resolver.resolveContextPath(request)).isEqualTo("/dist-prod");
        assertThat(resolver.resolveContextPath(environment)).isEqualTo("/dist-prod");
        assertThat(resolver.resolve(request)).isEqualTo("/dist-prod/retreever");
        assertThat(resolver.resolve(environment)).isEqualTo("/dist-prod/retreever");
    }

    @Test
    void configuredContextPathWithRetreeverSegmentIsTreatedAsContextPath() {
        RetreeverBasePathResolver resolver = resolver(new MockEnvironment(), "/dist-prod/retreever/");

        assertThat(resolver.resolveContextPath(new MockHttpServletRequest())).isEqualTo("/dist-prod");
        assertThat(resolver.resolve(new MockHttpServletRequest())).isEqualTo("/dist-prod/retreever");
    }

    @Test
    void rootConfiguredContextPathResolvesToRetreeverMount() {
        RetreeverBasePathResolver resolver = resolver(new MockEnvironment(), "/");

        assertThat(resolver.resolveContextPath(new MockHttpServletRequest())).isEqualTo("");
        assertThat(resolver.resolve(new MockHttpServletRequest())).isEqualTo("/retreever");
    }

    private RetreeverBasePathResolver resolver(MockEnvironment environment, String configuredContextPath) {
        RetreeverContextPathProperties properties = new RetreeverContextPathProperties();
        properties.setContextPath(configuredContextPath);
        return new RetreeverBasePathResolver(properties, environment);
    }
}
