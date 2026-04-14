package dev.retreever.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = RetreeverAuthenticationIntegrationTest.TestApplication.class,
        properties = {
                "retreever.auth.username=admin",
                "retreever.auth.password=secret",
                "retreever.allow-cross-origin=http://localhost:5173"
        }
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RetreeverAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uiRoutesRemainPublic() throws Exception {
        mockMvc.perform(get("/retreever").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/retreever/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void imageAssetsRemainPublic() throws Exception {
        mockMvc.perform(get("/images/retreever-logo.svg"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString("image/svg+xml")));
    }

    @Test
    void allowsConfiguredCrossOriginPreflightRequests() throws Exception {
        mockMvc.perform(options("/retreever/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, org.hamcrest.Matchers.containsString("content-type")));
    }

    @Test
    void returnsSpecificCorsHeadersForCrossOriginApiRequests() throws Exception {
        mockMvc.perform(get("/retreever/doc")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void rejectsUnauthenticatedApiRequestsWithoutClearingCookies() throws Exception {
        mockMvc.perform(get("/retreever/doc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void loginIssuesUniqueTokensForSeparateSessions() throws Exception {
        AuthCookies firstSession = login();
        AuthCookies secondSession = login();

        assertThat(firstSession.accessToken().getValue()).isNotEqualTo(secondSession.accessToken().getValue());
        assertThat(firstSession.refreshToken().getValue()).isNotEqualTo(secondSession.refreshToken().getValue());
        assertThat(firstSession.deviceId().getValue()).isNotEqualTo(secondSession.deviceId().getValue());
    }

    @Test
    void loginIssuesCookiesAndAllowsProtectedRequests() throws Exception {
        AuthCookies authCookies = login();

        mockMvc.perform(get("/retreever/ping")
                        .cookie(authCookies.accessToken(), authCookies.deviceId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void refreshRotatesTokensAndLogoutRevokesSession() throws Exception {
        AuthCookies authCookies = login();

        MvcResult refreshResult = mockMvc.perform(post("/retreever/refresh")
                        .cookie(authCookies.refreshToken(), authCookies.deviceId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andReturn();

        AuthCookies refreshedCookies = extractAuthCookies(refreshResult);

        assertThat(refreshedCookies.accessToken().getValue()).isNotEqualTo(authCookies.accessToken().getValue());
        assertThat(refreshedCookies.refreshToken().getValue()).isNotEqualTo(authCookies.refreshToken().getValue());
        assertThat(refreshedCookies.deviceId().getValue()).isEqualTo(authCookies.deviceId().getValue());

        mockMvc.perform(get("/retreever/environment")
                        .cookie(refreshedCookies.accessToken(), refreshedCookies.deviceId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/retreever/logout")
                        .cookie(refreshedCookies.accessToken(), refreshedCookies.refreshToken(), refreshedCookies.deviceId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/retreever/environment")
                        .cookie(refreshedCookies.accessToken(), refreshedCookies.deviceId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/retreever/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong"}
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    private AuthCookies login() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/retreever/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"secret"}
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andReturn();

        AuthCookies authCookies = extractAuthCookies(loginResult);

        assertThat(authCookies.accessHeader()).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/retreever");
        assertThat(authCookies.refreshHeader()).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/retreever");
        assertThat(authCookies.deviceHeader()).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/retreever");

        return authCookies;
    }

    private AuthCookies extractAuthCookies(MvcResult result) {
        List<String> cookieHeaders = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        String accessCookieHeader = findCookieHeader(cookieHeaders, RetreeverAuthSupport.ACCESS_TOKEN_COOKIE_NAME);
        String refreshCookieHeader = findCookieHeader(cookieHeaders, RetreeverAuthSupport.REFRESH_TOKEN_COOKIE_NAME);
        String deviceCookieHeader = findCookieHeader(cookieHeaders, RetreeverAuthSupport.DEVICE_ID_COOKIE_NAME);

        return new AuthCookies(
                accessCookieHeader,
                refreshCookieHeader,
                deviceCookieHeader,
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
            String accessHeader,
            String refreshHeader,
            String deviceHeader,
            Cookie accessToken,
            Cookie refreshToken,
            Cookie deviceId
    ) {
    }

    @SpringBootApplication
    static class TestApplication {
    }

    @RestController
    static class SampleController {

        @GetMapping("/sample")
        String sample() {
            return "ok";
        }
    }
}
