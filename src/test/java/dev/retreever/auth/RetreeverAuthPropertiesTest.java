package dev.retreever.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetreeverAuthPropertiesTest {

    @Test
    void normalizesValidUuidSecret() {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();
        authProperties.setUsername("admin");
        authProperties.setPassword("secret");
        authProperties.setSecret(" 123e4567-e89b-12d3-a456-426614174000 ");

        authProperties.afterPropertiesSet();

        assertThat(authProperties.getSecret()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
        assertThat(authProperties.isSecureCookies()).isFalse();
    }

    @Test
    void rejectsNonUuidSecret() {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();
        authProperties.setUsername("admin");
        authProperties.setPassword("secret");
        authProperties.setSecret("not-a-uuid");

        assertThatThrownBy(authProperties::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'retreever.auth.secret' must be a valid UUID string.");
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
    void rejectsPartialCredentialConfiguration() {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();
        authProperties.setUsername("admin");

        assertThatThrownBy(authProperties::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Retreever authentication requires both");
    }
}
