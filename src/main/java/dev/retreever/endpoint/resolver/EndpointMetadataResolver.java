/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.resolver;

import org.springframework.http.HttpStatus;
import dev.retreever.annotation.ApiEndpoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Resolves endpoint-level metadata such as name, security flags,
 * HTTP status, description, and deprecation markers.
 * Reads metadata from {@link ApiEndpoint}, Spring Security annotations,
 * and Java reflection.
 */
public class EndpointMetadataResolver {

    private static final Class<? extends Annotation> PRE_AUTHORIZE_ANNOTATION = loadPreAuthorize();

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadPreAuthorize() {
        try {
            return (Class<? extends Annotation>)
                    Class.forName("org.springframework.security.access.prepost.PreAuthorize");
        } catch (ClassNotFoundException e) {
            return null; // Spring Security not in classpath
        }
    }

    /**
     * Populates the core metadata fields of an {@link dev.retreever.endpoint.model.ApiEndpoint}.
     * Handles:
     * <ul>
     *     <li>Endpoint name (from annotation or prettified method name)</li>
     *     <li>Security flag (via @ApiEndpoint or @PreAuthorize)</li>
     *     <li>HTTP status and description</li>
     *     <li>Deprecated marker</li>
     * </ul>
     *
     * @param endpoint the endpoint model to enrich
     * @param method   the controller method
     */
    public static void resolve(
            dev.retreever.endpoint.model.ApiEndpoint endpoint,
            Method method
    ) {

        // Read @ApiEndpoint if present
        ApiEndpoint annotation = method.getAnnotation(ApiEndpoint.class);

        // Name
        if (annotation != null && !annotation.name().isBlank()) {
            endpoint.setName(annotation.name());
        } else {
            endpoint.setName(prettifyName(method.getName()));
        }

        // Security checks
        if (annotation != null && annotation.secured()) {
            endpoint.secure();
        }
        else if (PRE_AUTHORIZE_ANNOTATION != null &&
                method.isAnnotationPresent(PRE_AUTHORIZE_ANNOTATION)) {
            endpoint.secure();
        }

        // Status & description
        if (annotation != null) {
            endpoint.setStatus(annotation.status());
            endpoint.setDescription(annotation.description());
        } else {
            endpoint.setStatus(HttpStatus.OK);
        }

        // Deprecated marker
        if (method.isAnnotationPresent(Deprecated.class)) {
            endpoint.deprecate();
        }
    }

    /**
     * Converts a raw method name into a readable title.
     * Example: "getUserDetails" → "Get User Details".
     */
    private static String prettifyName(String raw) {
        if (raw == null || raw.isBlank()) return raw;

        String spaced = raw
                .replace("_", " ")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .trim();

        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }
}
