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

@Component
public class RetreeverTokenService {

    private static final Logger log = LoggerFactory.getLogger(RetreeverTokenService.class);
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final RetreeverAuthProperties properties;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec secretKey;

    public RetreeverTokenService(RetreeverAuthProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.secretKey = properties.isDisabled() ? null : new SecretKeySpec(resolveSecretKey(properties), "AES");
    }

    public Optional<TokenPair> login(String username, String password) {
        if (properties.isDisabled()) {
            return Optional.empty();
        }

        if (!Objects.equals(properties.getUsername(), username) || !Objects.equals(properties.getPassword(), password)) {
            return Optional.empty();
        }

        Instant issuedAt = Instant.now();
        String deviceId = UUID.randomUUID().toString();
        return Optional.of(issueTokenPair(username, deviceId, issuedAt));
    }

    public Optional<AuthenticatedUser> authenticate(String accessToken, String deviceId) {
        if (properties.isDisabled()) {
            return Optional.empty();
        }

        return decryptToken(accessToken, TokenType.ACCESS)
                .filter(payload -> Objects.equals(payload.deviceId(), deviceId))
                .map(payload -> new AuthenticatedUser(
                        payload.username(),
                        payload.deviceId(),
                        payload.expiresAt()
                ));
    }

    public Optional<TokenPair> refresh(String refreshToken, String deviceId) {
        if (properties.isDisabled()) {
            return Optional.empty();
        }

        return decryptToken(refreshToken, TokenType.REFRESH)
                .filter(payload -> Objects.equals(payload.deviceId(), deviceId))
                .map(payload -> issueTokenPair(payload.username(), payload.deviceId(), Instant.now()));
    }

    public void logout() {
        // Stateless auth cannot revoke previously issued tokens without shared server-side state.
    }

    private TokenPair issueTokenPair(String username, String deviceId, Instant issuedAt) {
        Instant accessTokenExpiresAt = issuedAt.plus(properties.getAccessTokenTtl());
        Instant refreshTokenExpiresAt = issuedAt.plus(properties.getRefreshTokenTtl());

        TokenPayload accessPayload = new TokenPayload(
                1,
                TokenType.ACCESS,
                UUID.randomUUID().toString(),
                deviceId,
                username,
                issuedAt,
                accessTokenExpiresAt
        );
        TokenPayload refreshPayload = new TokenPayload(
                1,
                TokenType.REFRESH,
                UUID.randomUUID().toString(),
                deviceId,
                username,
                issuedAt,
                refreshTokenExpiresAt
        );

        return new TokenPair(
                deviceId,
                encrypt(accessPayload),
                accessTokenExpiresAt,
                encrypt(refreshPayload),
                refreshTokenExpiresAt
        );
    }

    private Optional<TokenPayload> decryptToken(String token, TokenType expectedType) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        try {
            if (secretKey == null) {
                return Optional.empty();
            }

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
            if (secretKey == null) {
                throw new IllegalStateException("Retreever auth is not configured.");
            }

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

    private byte[] resolveSecretKey(RetreeverAuthProperties authProperties) {
        if (StringUtils.hasText(authProperties.getSecret())) {
            return sha256(authProperties.getSecret().trim().getBytes(StandardCharsets.UTF_8));
        }

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

    private record TokenPayload(
            int version,
            TokenType type,
            String tokenId,
            String deviceId,
            String username,
            Instant issuedAt,
            Instant expiresAt
    ) {
    }

    private enum TokenType {
        ACCESS,
        REFRESH
    }
}
