package dev.retreever.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
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
                "retreever.dev.allow-cross-origin=http://localhost:5173"
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
                .andExpect(forwardedUrl("/index.html"))
                .andExpect(header().string(
                        "Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'self'")
                ))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));

        mockMvc.perform(get("/retreever/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void retreeverSecurityHeadersDoNotApplyToHostApplicationRoutes() throws Exception {
        mockMvc.perform(get("/sample"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }

    @Test
    void imageAssetsRemainPublic() throws Exception {
        Resource[] imageResources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:/META-INF/resources/images/*");

        Assumptions.assumeTrue(imageResources.length > 0, "No /images assets packaged in the current UI build");

        Resource imageResource = imageResources[0];
        String imageName = imageResource.getFilename();
        Assumptions.assumeTrue(imageName != null && !imageName.isBlank(), "Packaged image asset must have a filename");

        mockMvc.perform(get("/images/" + imageName))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.startsWith("image/")));
    }

    @Test
    void iconAssetsRemainPublicAndCacheable() throws Exception {
        mockMvc.perform(get("/assets/icons/icon192v2.png"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString("image/png")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("max-age=")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("immutable")));
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
    void loginIssuesUniqueTokensForSeparateLogins() throws Exception {
        AuthCookies firstLogin = login();
        AuthCookies secondLogin = login();

        assertThat(firstLogin.accessToken().getValue()).isNotEqualTo(secondLogin.accessToken().getValue());
        assertThat(firstLogin.refreshToken().getValue()).isNotEqualTo(secondLogin.refreshToken().getValue());
        assertThat(firstLogin.deviceId().getValue()).isNotEqualTo(secondLogin.deviceId().getValue());
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
    void refreshReissuesTokensAndLogoutClearsBrowserSession() throws Exception {
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

        MvcResult logoutResult = mockMvc.perform(post("/retreever/logout")
                        .cookie(refreshedCookies.accessToken(), refreshedCookies.refreshToken(), refreshedCookies.deviceId()))
                .andExpect(status().isNoContent())
                .andReturn();

        String clearedAccessCookieHeader = findCookieHeader(
                logoutResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
                RetreeverAuthSupport.ACCESS_TOKEN_COOKIE_NAME
        );
        String clearedRefreshCookieHeader = findCookieHeader(
                logoutResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
                RetreeverAuthSupport.REFRESH_TOKEN_COOKIE_NAME
        );
        String clearedDeviceCookieHeader = findCookieHeader(
                logoutResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
                RetreeverAuthSupport.DEVICE_ID_COOKIE_NAME
        );

        assertThat(clearedAccessCookieHeader).contains("Max-Age=0");
        assertThat(clearedRefreshCookieHeader).contains("Max-Age=0");
        assertThat(clearedDeviceCookieHeader).contains("Max-Age=0");

        mockMvc.perform(get("/retreever/environment")
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
                .andExpect(jsonPath("$.error").value("invalid_credentials"))
                .andExpect(jsonPath("$.attemptsLeft").value(4))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString(
                        RetreeverLoginGuardService.LOGIN_GUARD_COOKIE_NAME + "="
                )));
    }

    @Test
    void locksLoginAfterEveryFifthFailedAttemptForTheSameBrowser() throws Exception {
        Cookie loginGuardCookie = null;

        for (int attempt = 1; attempt <= 4; attempt++) {
            var loginRequest = post("/retreever/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username":"admin","password":"wrong"}
                            """)
                    .accept(MediaType.APPLICATION_JSON);
            if (loginGuardCookie != null) {
                loginRequest.cookie(loginGuardCookie);
            }

            MvcResult result = mockMvc.perform(loginRequest)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("invalid_credentials"))
                    .andExpect(jsonPath("$.attemptsLeft").value(5 - attempt))
                    .andReturn();

            loginGuardCookie = loginGuardCookie(result);
        }

        MvcResult lockedResult = mockMvc.perform(post("/retreever/login")
                        .cookie(loginGuardCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong"}
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
                .andExpect(jsonPath("$.error").value("login_locked"))
                .andExpect(jsonPath("$.attemptsLeft").value(0))
                .andExpect(jsonPath("$.retryAfterSeconds").value(30))
                .andReturn();

        loginGuardCookie = loginGuardCookie(lockedResult);

        mockMvc.perform(post("/retreever/login")
                        .cookie(loginGuardCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"secret"}
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("login_locked"));
    }

    @Test
    void successfulLoginClearsLoginGuardCookie() throws Exception {
        MvcResult failedResult = mockMvc.perform(post("/retreever/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong"}
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Cookie loginGuardCookie = loginGuardCookie(failedResult);

        MvcResult successfulResult = mockMvc.perform(post("/retreever/login")
                        .cookie(loginGuardCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"secret"}
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andReturn();

        String clearedLoginGuardCookieHeader = findCookieHeader(
                successfulResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
                RetreeverLoginGuardService.LOGIN_GUARD_COOKIE_NAME
        );

        assertThat(clearedLoginGuardCookieHeader).contains("Max-Age=0");
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
        assertThat(authCookies.accessHeader()).contains("Secure");
        assertThat(authCookies.refreshHeader()).contains("Secure");
        assertThat(authCookies.deviceHeader()).contains("Secure");

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

    private Cookie loginGuardCookie(MvcResult result) {
        return toCookie(findCookieHeader(
                result.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
                RetreeverLoginGuardService.LOGIN_GUARD_COOKIE_NAME
        ));
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
