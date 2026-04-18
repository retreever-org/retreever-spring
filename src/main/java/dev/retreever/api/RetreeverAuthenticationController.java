package dev.retreever.api;

import dev.retreever.auth.RetreeverAuthProperties;
import dev.retreever.auth.RetreeverAuthSupport;
import dev.retreever.auth.RetreeverTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping(RetreeverAuthSupport.RETREEVER_BASE_PATH)
public class RetreeverAuthenticationController {

    private final RetreeverAuthProperties authProperties;
    private final RetreeverTokenService tokenService;

    public RetreeverAuthenticationController(
            RetreeverAuthProperties authProperties,
            RetreeverTokenService tokenService) {
        this.authProperties = authProperties;
        this.tokenService = tokenService;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(
            @RequestBody RetreeverLoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (authProperties.isDisabled()) {
            return ResponseEntity.notFound().build();
        }

        return tokenService.login(loginRequest.username(), loginRequest.password())
                .<ResponseEntity<?>>map(tokenPair -> {
                    RetreeverAuthSupport.writeAuthenticationCookies(
                            request,
                            response,
                            tokenPair,
                            authProperties.isSecureCookies()
                    );
                    return ResponseEntity.ok(toResponseBody(tokenPair));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "invalid_credentials",
                        "message", "Invalid username or password."
                )));
    }

    @PostMapping(path = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        if (authProperties.isDisabled()) {
            return ResponseEntity.notFound().build();
        }

        return tokenService.refresh(
                        RetreeverAuthSupport.getRefreshToken(request),
                        RetreeverAuthSupport.getDeviceId(request)
                )
                .<ResponseEntity<?>>map(tokenPair -> {
                    RetreeverAuthSupport.writeAuthenticationCookies(
                            request,
                            response,
                            tokenPair,
                            authProperties.isSecureCookies()
                    );
                    return ResponseEntity.ok(toResponseBody(tokenPair));
                })
                .orElseGet(() -> {
                    RetreeverAuthSupport.clearAuthenticationCookies(
                            request,
                            response,
                            authProperties.isSecureCookies()
                    );
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                            "error", "invalid_refresh_token",
                            "message", "Refresh token is missing, expired, or invalid."
                    ));
                });
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        if (authProperties.isDisabled()) {
            return ResponseEntity.notFound().build();
        }

        tokenService.logout();
        RetreeverAuthSupport.clearAuthenticationCookies(request, response, authProperties.isSecureCookies());
        return ResponseEntity.noContent().build();
    }

    private RetreeverAuthResponse toResponseBody(RetreeverTokenService.TokenPair tokenPair) {
        return new RetreeverAuthResponse(
                true,
                tokenPair.accessTokenExpiresAt(),
                tokenPair.refreshTokenExpiresAt()
        );
    }

    public record RetreeverLoginRequest(String username, String password) {
    }

    public record RetreeverAuthResponse(
            boolean authenticated,
            Instant accessTokenExpiresAt,
            Instant refreshTokenExpiresAt
    ) {
    }
}
