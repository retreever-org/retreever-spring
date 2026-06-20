package dev.retreever.auth;

public record RetreeverAuthenticationResult(
        String username,
        boolean isAuthenticated
) {

    public static RetreeverAuthenticationResult authenticated(String username) {
        return new RetreeverAuthenticationResult(username, true);
    }

    public static RetreeverAuthenticationResult unauthenticated() {
        return new RetreeverAuthenticationResult(null, false);
    }
}
