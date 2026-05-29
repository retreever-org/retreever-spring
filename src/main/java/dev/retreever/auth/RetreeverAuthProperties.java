package dev.retreever.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "retreever.auth")
public class RetreeverAuthProperties implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RetreeverAuthProperties.class);
    private static final Duration DEFAULT_ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration DEFAULT_REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private String username;
    private String password;
    private String secret;
    private boolean secureCookies = true;
    private Duration accessTokenTtl = DEFAULT_ACCESS_TOKEN_TTL;
    private Duration refreshTokenTtl = DEFAULT_REFRESH_TOKEN_TTL;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public boolean isSecureCookies() {
        return secureCookies;
    }

    public void setSecureCookies(boolean secureCookies) {
        this.secureCookies = secureCookies;
    }

    /**
     * @deprecated use {@code retreever.auth.secure-cookies}.
     */
    @Deprecated(forRemoval = false)
    public boolean isSecure() {
        return secureCookies;
    }

    /**
     * @deprecated use {@code retreever.auth.secure-cookies}.
     */
    @Deprecated(forRemoval = false)
    public void setSecure(boolean secure) {
        this.secureCookies = secure;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public boolean isDisabled() {
        return !StringUtils.hasText(username) || !StringUtils.hasText(password);
    }

    @Override
    public void afterPropertiesSet() {
        boolean hasUsername = StringUtils.hasText(username);
        boolean hasPassword = StringUtils.hasText(password);

        if (!hasUsername && !hasPassword) {
            return;
        }

        if (hasUsername != hasPassword) {
            disableAuthBecauseInvalid(new IllegalStateException(
                    "Retreever authentication requires both 'retreever.auth.username' and 'retreever.auth.password'."
            ));
            return;
        }

        if (isNegative(accessTokenTtl)) {
            log.error(
                    "Invalid Retreever auth configuration. Falling back to the default access token TTL.",
                    new IllegalStateException("'retreever.auth.access-token-ttl' must be a positive duration.")
            );
            accessTokenTtl = DEFAULT_ACCESS_TOKEN_TTL;
        }

        if (isNegative(refreshTokenTtl)) {
            log.error(
                    "Invalid Retreever auth configuration. Falling back to the default refresh token TTL.",
                    new IllegalStateException("'retreever.auth.refresh-token-ttl' must be a positive duration.")
            );
            refreshTokenTtl = DEFAULT_REFRESH_TOKEN_TTL;
        }

        if (StringUtils.hasText(secret)) {
            try {
                secret = java.util.UUID.fromString(secret.trim()).toString();
            } catch (IllegalArgumentException ex) {
                log.error(
                        "Invalid Retreever auth secret. Retreever will generate a startup-only secret instead.",
                        new IllegalStateException("'retreever.auth.secret' must be a valid UUID string.", ex)
                );
                secret = null;
            }
        }
    }

    private boolean isNegative(Duration duration) {
        return duration == null || duration.compareTo(Duration.ZERO) <= 0;
    }

    private void disableAuthBecauseInvalid(Exception ex) {
        log.error("Invalid Retreever auth configuration. Retreever authentication will be disabled.", ex);
        username = null;
        password = null;
        secret = null;
    }
}
