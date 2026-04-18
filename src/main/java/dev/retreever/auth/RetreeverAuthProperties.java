package dev.retreever.auth;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "retreever.auth")
public class RetreeverAuthProperties implements InitializingBean {

    private String username;
    private String password;
    private String secret;
    private boolean secureCookies;
    private Duration accessTokenTtl = Duration.ofMinutes(30);
    private Duration refreshTokenTtl = Duration.ofDays(7);

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
            throw new IllegalStateException(
                    "Retreever authentication requires both 'retreever.auth.username' and 'retreever.auth.password'."
            );
        }

        if (StringUtils.hasText(secret)) {
            try {
                secret = java.util.UUID.fromString(secret.trim()).toString();
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("'retreever.auth.secret' must be a valid UUID string.", ex);
            }
        }

        if (isNegative(accessTokenTtl)) {
            throw new IllegalStateException("'retreever.auth.access-token-ttl' must be a positive duration.");
        }

        if (isNegative(refreshTokenTtl)) {
            throw new IllegalStateException("'retreever.auth.refresh-token-ttl' must be a positive duration.");
        }
    }

    private boolean isNegative(Duration duration) {
        return duration == null || duration.compareTo(Duration.ZERO) <= 0;
    }
}
