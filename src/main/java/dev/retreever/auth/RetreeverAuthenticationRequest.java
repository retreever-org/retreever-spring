package dev.retreever.auth;

public record RetreeverAuthenticationRequest(
        String principal,
        String credential
) {
}
