package dev.retreever.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetreeverTokenServiceStatelessTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void tokensIssuedOnOneInstanceAuthenticateOnAnotherWhenSecretIsShared() {
        RetreeverAuthProperties authProperties = authProperties("123e4567-e89b-12d3-a456-426614174000");
        RetreeverTokenService issuingInstance = new RetreeverTokenService(authProperties, objectMapper);
        RetreeverTokenService validatingInstance = new RetreeverTokenService(authProperties, objectMapper);

        RetreeverTokenService.TokenPair tokenPair = issuingInstance.login("admin", "secret")
                .orElseThrow(() -> new AssertionError("Expected login to succeed."));

        assertThat(validatingInstance.authenticate(tokenPair.accessToken(), tokenPair.deviceId()))
                .isPresent()
                .get()
                .extracting(RetreeverTokenService.AuthenticatedUser::username)
                .isEqualTo("admin");

        assertThat(validatingInstance.refresh(tokenPair.refreshToken(), tokenPair.deviceId()))
                .isPresent();
    }

    @Test
    void tokensCannotBeValidatedAcrossInstancesWhenSecretsDiffer() {
        RetreeverTokenService issuingInstance = new RetreeverTokenService(
                authProperties("123e4567-e89b-12d3-a456-426614174000"),
                objectMapper
        );
        RetreeverTokenService validatingInstance = new RetreeverTokenService(
                authProperties("123e4567-e89b-12d3-a456-426614174001"),
                objectMapper
        );

        RetreeverTokenService.TokenPair tokenPair = issuingInstance.login("admin", "secret")
                .orElseThrow(() -> new AssertionError("Expected login to succeed."));

        assertThat(validatingInstance.authenticate(tokenPair.accessToken(), tokenPair.deviceId()))
                .isEmpty();
        assertThat(validatingInstance.refresh(tokenPair.refreshToken(), tokenPair.deviceId()))
                .isEmpty();
    }

    private RetreeverAuthProperties authProperties(String secret) {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();
        authProperties.setUsername("admin");
        authProperties.setPassword("secret");
        authProperties.setSecret(secret);
        authProperties.afterPropertiesSet();
        return authProperties;
    }
}
