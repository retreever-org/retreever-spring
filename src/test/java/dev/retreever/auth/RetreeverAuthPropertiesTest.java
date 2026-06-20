package dev.retreever.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class RetreeverAuthPropertiesTest {

    @Test
    void normalizesValidUuidSecret() {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();
        authProperties.setUsername("admin");
        authProperties.setPassword("secret");
        authProperties.setSecret(" 123e4567-e89b-12d3-a456-426614174000 ");

        authProperties.afterPropertiesSet();

        assertThat(authProperties.getSecret()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
        assertThat(authProperties.isSecureCookies()).isTrue();
    }

    @Test
    void allowsSecureCookiesToBeDisabledExplicitly() {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();

        authProperties.setSecureCookies(false);

        assertThat(authProperties.isSecureCookies()).isFalse();
    }

    @Test
    void bindsSecureCookiesPropertyName() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("retreever.auth.secure-cookies", "false");

        RetreeverAuthProperties authProperties = Binder.get(environment)
                .bind("retreever.auth", RetreeverAuthProperties.class)
                .orElseThrow(() -> new AssertionError("Expected Retreever auth properties to bind."));

        assertThat(authProperties.isSecureCookies()).isFalse();
    }

    @Test
    @SuppressWarnings("deprecation")
    void keepsSecureAsBackwardCompatibleAlias() {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();

        authProperties.setSecure(false);

        assertThat(authProperties.isSecure()).isFalse();
        assertThat(authProperties.isSecureCookies()).isFalse();
    }

    @Test
    void ignoresNonUuidSecretAndKeepsAuthEnabled() {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();
        authProperties.setUsername("admin");
        authProperties.setPassword("secret");
        authProperties.setSecret("not-a-uuid");

        authProperties.afterPropertiesSet();

        assertThat(authProperties.isDisabled()).isFalse();
        assertThat(authProperties.getSecret()).isNull();
    }

    @Test
    void validatesSecretAndTtlSettingsEvenWhenStaticAuthIsDisabled() {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();
        authProperties.setSecret("not-a-uuid");
        authProperties.setAccessTokenTtl(java.time.Duration.ofMinutes(-1));
        authProperties.setRefreshTokenTtl(java.time.Duration.ofDays(-1));

        authProperties.afterPropertiesSet();

        assertThat(authProperties.isDisabled()).isTrue();
        assertThat(authProperties.getSecret()).isNull();
        assertThat(authProperties.getAccessTokenTtl()).isEqualTo(java.time.Duration.ofMinutes(30));
        assertThat(authProperties.getRefreshTokenTtl()).isEqualTo(java.time.Duration.ofDays(7));
    }

    @Test
    void disablesAuthForPartialCredentialConfiguration() {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();
        authProperties.setUsername("admin");

        authProperties.afterPropertiesSet();

        assertThat(authProperties.isDisabled()).isTrue();
    }
}
