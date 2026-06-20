package retreever.hostauth;

import dev.retreever.auth.RetreeverAuthSupport;
import dev.retreever.auth.RetreeverAuthenticator;
import dev.retreever.auth.RetreeverAuthenticationResult;
import dev.retreever.auth.RetreeverLoginGuardService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = RetreeverHostAuthenticationIntegrationTest.TestApplication.class,
        properties = "retreever.auth.secret=123e4567-e89b-12d3-a456-426614174000"
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RetreeverHostAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void hostAuthenticatorEnablesRetreeverAuthWithoutStaticCredentials() throws Exception {
        AuthCookies authCookies = login("dev@example.com", "host-secret");

        mockMvc.perform(get("/retreever/ping")
                        .cookie(authCookies.accessToken(), authCookies.deviceId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void rejectedHostAuthenticationReturnsInvalidCredentialsAndRecordsGuardAttempt() throws Exception {
        mockMvc.perform(post("/retreever/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"dev@example.com","password":"wrong"}
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"))
                .andExpect(jsonPath("$.attemptsLeft").value(4))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString(
                        RetreeverLoginGuardService.LOGIN_GUARD_COOKIE_NAME + "="
                )));
    }

    private AuthCookies login(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/retreever/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andReturn();

        return extractAuthCookies(loginResult);
    }

    private AuthCookies extractAuthCookies(MvcResult result) {
        List<String> cookieHeaders = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        String accessCookieHeader = findCookieHeader(cookieHeaders, RetreeverAuthSupport.ACCESS_TOKEN_COOKIE_NAME);
        String refreshCookieHeader = findCookieHeader(cookieHeaders, RetreeverAuthSupport.REFRESH_TOKEN_COOKIE_NAME);
        String deviceCookieHeader = findCookieHeader(cookieHeaders, RetreeverAuthSupport.DEVICE_ID_COOKIE_NAME);

        assertThat(accessCookieHeader).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/retreever");
        assertThat(refreshCookieHeader).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/retreever");
        assertThat(deviceCookieHeader).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/retreever");

        return new AuthCookies(
                toCookie(accessCookieHeader),
                toCookie(refreshCookieHeader),
                toCookie(deviceCookieHeader)
        );
    }

    private String findCookieHeader(List<String> headers, String cookieName) {
        return headers.stream()
                .filter(header -> header.startsWith(cookieName + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing cookie header for " + cookieName));
    }

    private Cookie toCookie(String setCookieHeader) {
        String[] nameAndValue = setCookieHeader.split(";", 2)[0].split("=", 2);
        return new Cookie(nameAndValue[0], nameAndValue[1]);
    }

    private record AuthCookies(
            Cookie accessToken,
            Cookie refreshToken,
            Cookie deviceId
    ) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        RetreeverAuthenticator retreeverAuthenticator() {
            return request -> {
                if ("dev@example.com".equals(request.principal())
                        && "host-secret".equals(request.credential())) {
                    return RetreeverAuthenticationResult.authenticated("host-user");
                }
                return RetreeverAuthenticationResult.unauthenticated();
            };
        }
    }
}
