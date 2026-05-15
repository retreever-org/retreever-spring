package dev.retreever.auth;

import org.junit.jupiter.api.Test;

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
    void ignoresSecretAndTtlSettingsWhenAuthIsDisabled() {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();
        authProperties.setSecret("not-a-uuid");
        authProperties.setAccessTokenTtl(java.time.Duration.ofMinutes(-1));
        authProperties.setRefreshTokenTtl(java.time.Duration.ofDays(-1));

        authProperties.afterPropertiesSet();

        assertThat(authProperties.isDisabled()).isTrue();
    }

    @Test
    void disablesAuthForPartialCredentialConfiguration() {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();
        authProperties.setUsername("admin");

        authProperties.afterPropertiesSet();

        assertThat(authProperties.isDisabled()).isTrue();
    }
}
