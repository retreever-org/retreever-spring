package dev.retreever.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RetreeverTokenService {

    private static final Logger log = LoggerFactory.getLogger(RetreeverTokenService.class);
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final RetreeverAuthProperties properties;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, SessionRecord> sessions = new ConcurrentHashMap<>();
    private final SecretKeySpec secretKey;

    public RetreeverTokenService(RetreeverAuthProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.secretKey = new SecretKeySpec(resolveSecretKey(), "AES");
    }

    public synchronized Optional<TokenPair> login(String username, String password) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }

        if (!Objects.equals(properties.getUsername(), username) || !Objects.equals(properties.getPassword(), password)) {
            return Optional.empty();
        }

        Instant issuedAt = Instant.now();
        String sessionId = UUID.randomUUID().toString();
        String deviceId = UUID.randomUUID().toString();
        String refreshTokenId = UUID.randomUUID().toString();
        Instant refreshTokenExpiresAt = issuedAt.plus(properties.getRefreshTokenTtl());

        SessionRecord session = new SessionRecord(
                sessionId,
                username,
                deviceId,
                refreshTokenId,
                refreshTokenExpiresAt
        );
        sessions.put(sessionId, session);

        return Optional.of(issueTokenPair(session, issuedAt));
    }

    public Optional<AuthenticatedUser> authenticate(String accessToken, String deviceId) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }

        Optional<TokenPayload> payload = decryptToken(accessToken, TokenType.ACCESS);
        if (payload.isEmpty() || !Objects.equals(payload.get().deviceId(), deviceId)) {
            return Optional.empty();
        }

        SessionRecord session = sessions.get(payload.get().sessionId());
        if (session == null) {
            return Optional.empty();
        }

        if (!session.matches(payload.get().username(), payload.get().deviceId())) {
            sessions.remove(session.sessionId(), session);
            return Optional.empty();
        }

        return Optional.of(new AuthenticatedUser(
                session.username(),
                session.sessionId(),
                session.deviceId(),
                payload.get().expiresAt()
        ));
    }

    public synchronized Optional<TokenPair> refresh(String refreshToken, String deviceId) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }

        Optional<TokenPayload> payload = decryptToken(refreshToken, TokenType.REFRESH);
        if (payload.isEmpty() || !Objects.equals(payload.get().deviceId(), deviceId)) {
            return Optional.empty();
        }

        SessionRecord currentSession = sessions.get(payload.get().sessionId());
        if (currentSession == null) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        if (!currentSession.matches(payload.get().username(), payload.get().deviceId())
                || !Objects.equals(currentSession.refreshTokenId(), payload.get().refreshTokenId())
                || !currentSession.refreshTokenExpiresAt().isAfter(now)) {
            sessions.remove(currentSession.sessionId(), currentSession);
            return Optional.empty();
        }

        SessionRecord rotatedSession = currentSession.rotate(
                UUID.randomUUID().toString(),
                now.plus(properties.getRefreshTokenTtl())
        );
        sessions.put(rotatedSession.sessionId(), rotatedSession);

        return Optional.of(issueTokenPair(rotatedSession, now));
    }

    public synchronized void logout(String accessToken, String refreshToken, String deviceId) {
        resolveSessionId(accessToken, TokenType.ACCESS, deviceId)
                .or(() -> resolveSessionId(refreshToken, TokenType.REFRESH, deviceId))
                .ifPresent(sessions::remove);
    }

    private Optional<String> resolveSessionId(String token, TokenType tokenType, String deviceId) {
        Optional<TokenPayload> payload = decryptToken(token, tokenType);
        if (payload.isEmpty() || !Objects.equals(payload.get().deviceId(), deviceId)) {
            return Optional.empty();
        }
        return Optional.of(payload.get().sessionId());
    }

    private TokenPair issueTokenPair(SessionRecord session, Instant issuedAt) {
        Instant accessTokenExpiresAt = issuedAt.plus(properties.getAccessTokenTtl());

        TokenPayload accessPayload = new TokenPayload(
                1,
                TokenType.ACCESS,
                session.sessionId(),
                session.deviceId(),
                session.username(),
                null,
                issuedAt,
                accessTokenExpiresAt
        );
        TokenPayload refreshPayload = new TokenPayload(
                1,
                TokenType.REFRESH,
                session.sessionId(),
                session.deviceId(),
                session.username(),
                session.refreshTokenId(),
                issuedAt,
                session.refreshTokenExpiresAt()
        );

        return new TokenPair(
                session.deviceId(),
                encrypt(accessPayload),
                accessTokenExpiresAt,
                encrypt(refreshPayload),
                session.refreshTokenExpiresAt()
        );
    }

    private Optional<TokenPayload> decryptToken(String token, TokenType expectedType) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        try {
            String[] parts = token.split("\\.", 2);
            if (parts.length != 2) {
                return Optional.empty();
            }

            byte[] iv = Base64.getUrlDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getUrlDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] jsonBytes = cipher.doFinal(encrypted);
            TokenPayload payload = objectMapper.readValue(jsonBytes, TokenPayload.class);

            if (payload.version() != 1 || payload.type() != expectedType || !payload.expiresAt().isAfter(Instant.now())) {
                return Optional.empty();
            }

            return Optional.of(payload);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String encrypt(TokenPayload payload) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] encrypted = cipher.doFinal(objectMapper.writeValueAsBytes(payload));

            return Base64.getUrlEncoder().withoutPadding().encodeToString(iv)
                    + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt Retreever auth token.", ex);
        }
    }

    private byte[] resolveSecretKey() {
        String startupSecret = UUID.randomUUID().toString();
        log.info("Generated a startup Retreever auth secret. Tokens will be invalidated on application restart.");
        return sha256(startupSecret.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to derive Retreever auth secret.", ex);
        }
    }

    public record AuthenticatedUser(
            String username,
            String sessionId,
            String deviceId,
            Instant expiresAt
    ) {
    }

    public record TokenPair(
            String deviceId,
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt
    ) {
    }

    private record SessionRecord(
            String sessionId,
            String username,
            String deviceId,
            String refreshTokenId,
            Instant refreshTokenExpiresAt
    ) {

        private boolean matches(String username, String deviceId) {
            return Objects.equals(this.username, username) && Objects.equals(this.deviceId, deviceId);
        }

        private SessionRecord rotate(String refreshTokenId, Instant refreshTokenExpiresAt) {
            return new SessionRecord(sessionId, username, deviceId, refreshTokenId, refreshTokenExpiresAt);
        }
    }

    private record TokenPayload(
            int version,
            TokenType type,
            String sessionId,
            String deviceId,
            String username,
            String refreshTokenId,
            Instant issuedAt,
            Instant expiresAt
    ) {
    }

    private enum TokenType {
        ACCESS,
        REFRESH
    }
}
