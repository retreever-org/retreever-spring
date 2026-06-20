package dev.retreever.auth;

/**
 * Host-provided authentication port for Retreever login.
 */
@FunctionalInterface
public interface RetreeverAuthenticator {

    RetreeverAuthenticationResult authenticate(RetreeverAuthenticationRequest request);
}
