package dev.retreever.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetreeverAuthSupportTest {

    @Test
    void writesCookiesWithoutSecureWhenDisabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        RetreeverAuthSupport.writeAuthenticationCookies(request, response, tokenPair(), false);

        assertThat(response.getHeaders("Set-Cookie")).allSatisfy(header -> assertThat(header).doesNotContain("Secure"));
    }

    @Test
    void writesCookiesWithSecureWhenEnabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        RetreeverAuthSupport.writeAuthenticationCookies(request, response, tokenPair(), true);

        assertThat(response.getHeaders("Set-Cookie")).allSatisfy(header -> assertThat(header).contains("Secure"));
    }

    @Test
    void clearsCookiesUsingConfiguredSecureFlag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        RetreeverAuthSupport.clearAuthenticationCookies(request, response, true);

        List<String> headers = response.getHeaders("Set-Cookie");
        assertThat(headers).hasSize(3);
        assertThat(headers).allSatisfy(header -> {
            assertThat(header).contains("Max-Age=0");
            assertThat(header).contains("Secure");
        });
    }

    private RetreeverTokenService.TokenPair tokenPair() {
        Instant now = Instant.now();
        return new RetreeverTokenService.TokenPair(
                "device-id",
                "access-token",
                now.plus(30, ChronoUnit.MINUTES),
                "refresh-token",
                now.plus(7, ChronoUnit.DAYS)
        );
    }
}
