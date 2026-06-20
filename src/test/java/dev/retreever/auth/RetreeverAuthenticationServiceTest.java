package dev.retreever.auth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetreeverAuthenticationServiceTest {

    @Test
    void rejectsAuthenticatedHostResultWithBlankUsername() {
        RetreeverAuthProperties properties = new RetreeverAuthProperties();
        RetreeverAuthenticationService authenticationService = new RetreeverAuthenticationService(
                properties,
                List.of(request -> new RetreeverAuthenticationResult(" ", true))
        );

        assertThat(authenticationService.isEnabled()).isTrue();
        assertThat(authenticationService.authenticate("principal", "credential")).isEmpty();
    }

    @Test
    void failsClearlyWhenMultipleHostAuthenticatorsExist() {
        RetreeverAuthProperties properties = new RetreeverAuthProperties();
        RetreeverAuthenticator first = request -> RetreeverAuthenticationResult.unauthenticated();
        RetreeverAuthenticator second = request -> RetreeverAuthenticationResult.unauthenticated();

        assertThatThrownBy(() -> new RetreeverAuthenticationService(properties, List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at most one RetreeverAuthenticator bean");
    }
}
