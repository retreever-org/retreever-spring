package dev.retreever.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.util.WebUtils;

import java.time.Duration;
import java.time.Instant;

public final class RetreeverAuthSupport {

    public static final String RETREEVER_BASE_PATH = "/retreever";
    public static final String LOGIN_PATH = RETREEVER_BASE_PATH + "/login";
    public static final String REFRESH_PATH = RETREEVER_BASE_PATH + "/refresh";
    public static final String LOGOUT_PATH = RETREEVER_BASE_PATH + "/logout";
    public static final String DOC_PATH = RETREEVER_BASE_PATH + "/doc";
    public static final String PING_PATH = RETREEVER_BASE_PATH + "/ping";
    public static final String ENVIRONMENT_PATH = RETREEVER_BASE_PATH + "/environment";
    public static final String ACCESS_TOKEN_COOKIE_NAME = "retreever_at";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "retreever_rt";
    public static final String DEVICE_ID_COOKIE_NAME = "retreever_did";
    public static final String AUTHENTICATED_USER_ATTRIBUTE =
            RetreeverAuthSupport.class.getName() + ".AUTHENTICATED_USER";

    private static final String SAME_SITE_POLICY = "Lax";

    private RetreeverAuthSupport() {
    }

    public static String getAccessToken(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
    }

    public static String getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);
    }

    public static String getDeviceId(HttpServletRequest request) {
        return getCookieValue(request, DEVICE_ID_COOKIE_NAME);
    }

    public static void writeAuthenticationCookies(
            HttpServletRequest request,
            HttpServletResponse response,
            RetreeverTokenService.TokenPair tokenPair) {
        addCookie(
                request,
                response,
                ACCESS_TOKEN_COOKIE_NAME,
                tokenPair.accessToken(),
                tokenPair.accessTokenExpiresAt()
        );
        addCookie(
                request,
                response,
                REFRESH_TOKEN_COOKIE_NAME,
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresAt()
        );
        addCookie(
                request,
                response,
                DEVICE_ID_COOKIE_NAME,
                tokenPair.deviceId(),
                tokenPair.refreshTokenExpiresAt()
        );
    }

    public static void clearAuthenticationCookies(HttpServletRequest request, HttpServletResponse response) {
        addCookie(request, response, ACCESS_TOKEN_COOKIE_NAME, "", Instant.EPOCH);
        addCookie(request, response, REFRESH_TOKEN_COOKIE_NAME, "", Instant.EPOCH);
        addCookie(request, response, DEVICE_ID_COOKIE_NAME, "", Instant.EPOCH);
    }

    private static void addCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String name,
            String value,
            Instant expiresAt) {
        Duration maxAge = expiresAt.isAfter(Instant.now()) ? Duration.between(Instant.now(), expiresAt) : Duration.ZERO;

        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .sameSite(SAME_SITE_POLICY)
                .secure(request.isSecure())
                .path(resolveCookiePath(request))
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static String resolveCookiePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        if (contextPath == null || contextPath.isBlank()) {
            return RETREEVER_BASE_PATH;
        }
        return contextPath + RETREEVER_BASE_PATH;
    }

    private static String getCookieValue(HttpServletRequest request, String cookieName) {
        var cookie = WebUtils.getCookie(request, cookieName);
        return cookie != null ? cookie.getValue() : null;
    }
}
