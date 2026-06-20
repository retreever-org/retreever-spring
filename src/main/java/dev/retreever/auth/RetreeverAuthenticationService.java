package dev.retreever.auth;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class RetreeverAuthenticationService {

    private final RetreeverAuthProperties properties;
    private final RetreeverAuthenticator hostAuthenticator;

    public RetreeverAuthenticationService(
            RetreeverAuthProperties properties,
            List<RetreeverAuthenticator> hostAuthenticators) {
        if (hostAuthenticators.size() > 1) {
            throw new IllegalStateException("Retreever authentication requires at most one RetreeverAuthenticator bean.");
        }

        this.properties = properties;
        this.hostAuthenticator = hostAuthenticators.isEmpty() ? null : hostAuthenticators.get(0);
    }

    public boolean isEnabled() {
        return hostAuthenticator != null || properties.isStaticAuthenticationConfigured();
    }

    public boolean isHostManaged() {
        return hostAuthenticator != null;
    }

    public Optional<String> authenticate(String principal, String credential) {
        if (hostAuthenticator != null) {
            return authenticateWithHost(principal, credential);
        }

        if (!properties.isStaticAuthenticationConfigured()) {
            return Optional.empty();
        }

        if (!Objects.equals(properties.getUsername(), principal) || !Objects.equals(properties.getPassword(), credential)) {
            return Optional.empty();
        }

        return Optional.of(properties.getUsername());
    }

    private Optional<String> authenticateWithHost(String principal, String credential) {
        RetreeverAuthenticationResult result = hostAuthenticator.authenticate(
                new RetreeverAuthenticationRequest(principal, credential)
        );

        if (result == null || !result.isAuthenticated() || !StringUtils.hasText(result.username())) {
            return Optional.empty();
        }

        return Optional.of(result.username());
    }
}
