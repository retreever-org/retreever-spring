package dev.retreever.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RetreeverAuthenticationFilter extends OncePerRequestFilter {

    private final RetreeverAuthenticationService authenticationService;
    private final RetreeverTokenService tokenService;

    public RetreeverAuthenticationFilter(
            RetreeverAuthenticationService authenticationService,
            RetreeverTokenService tokenService) {
        this.authenticationService = authenticationService;
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !authenticationService.isEnabled();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        var authenticatedUser = tokenService.authenticate(
                RetreeverAuthSupport.getAccessToken(request),
                RetreeverAuthSupport.getDeviceId(request)
        );

        if (authenticatedUser.isPresent()) {
            request.setAttribute(RetreeverAuthSupport.AUTHENTICATED_USER_ATTRIBUTE, authenticatedUser.get());
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"error\":\"unauthorized\",\"message\":\"Authentication is required for Retreever.\"}"
        );
    }
}
