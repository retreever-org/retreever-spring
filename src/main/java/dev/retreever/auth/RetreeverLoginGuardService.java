package dev.retreever.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.retreever.boot.RetreeverBasePathResolver;
import dev.retreever.json.RetreeverJsonMapper;
import dev.retreever.json.RetreeverJsonMappers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Component
public class RetreeverLoginGuardService {

    public static final String LOGIN_GUARD_COOKIE_NAME = "retreever_lg";
    public static final int ATTEMPTS_PER_CYCLE = 5;

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int CURRENT_VERSION = 1;
    private static final Duration GUARD_COOKIE_TTL = Duration.ofDays(7);
    private static final String SAME_SITE_POLICY = "Lax";

    private final RetreeverJsonMapper jsonMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec secretKey;
    private final Clock clock;
    private final RetreeverBasePathResolver basePathResolver;

    @Autowired
    public RetreeverLoginGuardService(
            RetreeverAuthProperties properties,
            RetreeverAuthenticationService authenticationService,
            RetreeverJsonMapper jsonMapper,
            RetreeverBasePathResolver basePathResolver) {
        this(properties, authenticationService, jsonMapper, Clock.systemUTC(), basePathResolver);
    }

    RetreeverLoginGuardService(RetreeverAuthProperties properties, RetreeverJsonMapper jsonMapper, Clock clock) {
        this(properties, new RetreeverAuthenticationService(properties, java.util.List.of()), jsonMapper, clock, null);
    }

    RetreeverLoginGuardService(RetreeverAuthProperties properties, Object mapper, Clock clock) {
        this(properties, RetreeverJsonMappers.wrap(mapper), clock);
    }

    RetreeverLoginGuardService(
            RetreeverAuthProperties properties,
            RetreeverAuthenticationService authenticationService,
            RetreeverJsonMapper jsonMapper,
            Clock clock,
            RetreeverBasePathResolver basePathResolver) {
        this.jsonMapper = jsonMapper.copyWithNonNullInclusion();
        this.secretKey = authenticationService.isEnabled() ? new SecretKeySpec(resolveSecretKey(properties), "AES") : null;
        this.clock = clock;
        this.basePathResolver = basePathResolver;
    }

    public GuardStatus status(HttpServletRequest request) {
        LoginGuardState state = readState(request).orElseGet(this::newState);
        Instant now = now();

        if (state.lockedUntil() != null && state.lockedUntil().isAfter(now)) {
            return GuardStatus.locked(state, Duration.between(now, state.lockedUntil()));
        }

        return GuardStatus.allowed(state);
    }

    public FailedLoginResult recordFailedAttempt(LoginGuardState state) {
        Instant now = now();
        int failedAttempts = state.failedAttempts() + 1;
        int attemptsLeft = attemptsLeftInCycle(failedAttempts);
        Duration retryAfter = attemptsLeft == 0 ? lockoutDurationFor(failedAttempts) : Duration.ZERO;
        Instant lockedUntil = attemptsLeft == 0 ? now.plus(retryAfter) : null;

        LoginGuardState updatedState = new LoginGuardState(
                CURRENT_VERSION,
                StringUtils.hasText(state.guardId()) ? state.guardId() : UUID.randomUUID().toString(),
                failedAttempts,
                lockedUntil,
                now,
                now.plus(GUARD_COOKIE_TTL)
        );

        return new FailedLoginResult(updatedState, attemptsLeft, lockedUntil, retryAfter);
    }

    public void writeGuardCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            LoginGuardState state,
            boolean secureCookies) {
        addCookie(
                request,
                response,
                LOGIN_GUARD_COOKIE_NAME,
                encrypt(state),
                state.expiresAt(),
                secureCookies
        );
    }

    public void clearGuardCookie(HttpServletRequest request, HttpServletResponse response, boolean secureCookies) {
        addCookie(request, response, LOGIN_GUARD_COOKIE_NAME, "", Instant.EPOCH, secureCookies);
    }

    private Optional<LoginGuardState> readState(HttpServletRequest request) {
        String cookieValue = RetreeverAuthSupport.getCookieValue(request, LOGIN_GUARD_COOKIE_NAME);
        if (!StringUtils.hasText(cookieValue) || secretKey == null) {
            return Optional.empty();
        }

        try {
            String[] parts = cookieValue.split("\\.", 2);
            if (parts.length != 2) {
                return Optional.empty();
            }

            byte[] iv = Base64.getUrlDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getUrlDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] jsonBytes = cipher.doFinal(encrypted);
            LoginGuardState state = jsonMapper.readValue(jsonBytes, LoginGuardState.class);

            if (state.version() != CURRENT_VERSION || !StringUtils.hasText(state.guardId())) {
                return Optional.empty();
            }

            if (state.expiresAt() == null || !state.expiresAt().isAfter(now())) {
                return Optional.empty();
            }

            return Optional.of(state);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private LoginGuardState newState() {
        Instant now = now();
        return new LoginGuardState(
                CURRENT_VERSION,
                UUID.randomUUID().toString(),
                0,
                null,
                now,
                now.plus(GUARD_COOKIE_TTL)
        );
    }

    private int attemptsLeftInCycle(int failedAttempts) {
        int usedInCycle = failedAttempts % ATTEMPTS_PER_CYCLE;
        return usedInCycle == 0 ? 0 : ATTEMPTS_PER_CYCLE - usedInCycle;
    }

    private Duration lockoutDurationFor(int failedAttempts) {
        int completedCycles = failedAttempts / ATTEMPTS_PER_CYCLE;
        if (completedCycles <= 1) {
            return Duration.ofSeconds(30);
        }
        if (completedCycles == 2) {
            return Duration.ofMinutes(5);
        }
        if (completedCycles == 3) {
            return Duration.ofMinutes(30);
        }
        return Duration.ofHours(1);
    }

    private String encrypt(LoginGuardState state) {
        try {
            if (secretKey == null) {
                throw new IllegalStateException("Retreever auth is not configured.");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] encrypted = cipher.doFinal(jsonMapper.writeValueAsBytes(state));

            return Base64.getUrlEncoder().withoutPadding().encodeToString(iv)
                    + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt Retreever login guard.", ex);
        }
    }

    private byte[] resolveSecretKey(RetreeverAuthProperties authProperties) {
        if (StringUtils.hasText(authProperties.getSecret())) {
            return sha256(authProperties.getSecret().trim().getBytes(StandardCharsets.UTF_8));
        }

        return sha256(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to derive Retreever login guard secret.", ex);
        }
    }

    private void addCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String name,
            String value,
            Instant expiresAt,
            boolean secureCookies) {
        Duration maxAge = expiresAt.isAfter(now()) ? Duration.between(now(), expiresAt) : Duration.ZERO;

        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .sameSite(SAME_SITE_POLICY)
                .secure(secureCookies)
                .path(resolveCookiePath(request))
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private String resolveCookiePath(HttpServletRequest request) {
        return basePathResolver != null
                ? basePathResolver.resolveCookiePath(request)
                : RetreeverAuthSupport.resolveCookiePath(request);
    }

    public record GuardStatus(
            boolean locked,
            LoginGuardState state,
            Duration retryAfter
    ) {
        static GuardStatus allowed(LoginGuardState state) {
            return new GuardStatus(false, state, Duration.ZERO);
        }

        static GuardStatus locked(LoginGuardState state, Duration retryAfter) {
            return new GuardStatus(true, state, retryAfter);
        }

        public int retryAfterSeconds() {
            return toWholeSeconds(retryAfter);
        }
    }

    public record FailedLoginResult(
            LoginGuardState state,
            int attemptsLeft,
            Instant lockedUntil,
            Duration retryAfter
    ) {
        public boolean locked() {
            return lockedUntil != null;
        }

        public int retryAfterSeconds() {
            if (retryAfter.isZero()) {
                return 0;
            }
            return toWholeSeconds(retryAfter);
        }
    }

    public record LoginGuardState(
            int version,
            String guardId,
            int failedAttempts,
            Instant lockedUntil,
            Instant lastFailedAt,
            Instant expiresAt
    ) {
    }

    private static int toWholeSeconds(Duration duration) {
        return (int) Math.max(1, (duration.toMillis() + 999) / 1000);
    }
}
