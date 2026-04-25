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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Resolves endpoint-level metadata such as name, security flags,
 * HTTP status, description, and deprecation markers.
 * Reads metadata from {@link ApiEndpoint}, Spring Security annotations,
 * and Java reflection.
 */
public class EndpointMetadataResolver {

    private static final Class<? extends Annotation> PRE_AUTHORIZE_ANNOTATION =
            loadAnnotation("org.springframework.security.access.prepost.PreAuthorize");
    private static final Class<? extends Annotation> POST_AUTHORIZE_ANNOTATION =
            loadAnnotation("org.springframework.security.access.prepost.PostAuthorize");
    private static final Class<? extends Annotation> SECURED_ANNOTATION =
            loadAnnotation("org.springframework.security.access.annotation.Secured");
    private static final Class<? extends Annotation> JAKARTA_ROLES_ALLOWED_ANNOTATION =
            loadAnnotation("jakarta.annotation.security.RolesAllowed");
    private static final Class<? extends Annotation> JAVAX_ROLES_ALLOWED_ANNOTATION =
            loadAnnotation("javax.annotation.security.RolesAllowed");
    private static final Pattern OR_OPERATOR =
            Pattern.compile("\\|\\||(^|[\\s()])or($|[\\s()])", Pattern.CASE_INSENSITIVE);

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadAnnotation(String className) {
        try {
            return (Class<? extends Annotation>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Populates the core metadata fields of an {@link dev.retreever.endpoint.model.ApiEndpoint}.
     * Handles:
     * <ul>
     *     <li>Endpoint name (from annotation or prettified method name)</li>
     *     <li>Security flag (via @ApiEndpoint or supported security annotations)</li>
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

        // Security checks. A true @ApiEndpoint secured flag is explicit documentation and wins.
        if (annotation != null && annotation.secured()) {
            endpoint.secure();
        } else if (isSecured(method)) {
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
     * Example: "getUserDetails" -> "Get User Details".
     */
    private static String prettifyName(String raw) {
        if (raw == null || raw.isBlank()) return raw;

        String spaced = raw
                .replace("_", " ")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .trim();

        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    private static boolean isSecured(Method method) {
        SecurityResolution methodSecurity = resolveSecurity(method);

        if (methodSecurity != SecurityResolution.UNKNOWN) {
            return methodSecurity == SecurityResolution.SECURED;
        }

        SecurityResolution classSecurity = resolveSecurity(method.getDeclaringClass());
        return classSecurity == SecurityResolution.SECURED;
    }

    private static SecurityResolution resolveSecurity(Method method) {
        return resolveSecurity((AnnotationSource) method::getAnnotation);
    }

    private static SecurityResolution resolveSecurity(Class<?> controllerType) {
        return resolveSecurity((AnnotationSource) controllerType::getAnnotation);
    }

    private static SecurityResolution resolveSecurity(AnnotationSource source) {
        SecurityResolution expressionSecurity = strongest(
                resolveExpressionAnnotation(source, PRE_AUTHORIZE_ANNOTATION),
                resolveExpressionAnnotation(source, POST_AUTHORIZE_ANNOTATION)
        );
        SecurityResolution roleSecurity = strongest(
                resolveSecuredAnnotation(source),
                resolveRolesAllowedAnnotation(source, JAKARTA_ROLES_ALLOWED_ANNOTATION),
                resolveRolesAllowedAnnotation(source, JAVAX_ROLES_ALLOWED_ANNOTATION)
        );

        return strongest(expressionSecurity, roleSecurity);
    }

    private static SecurityResolution resolveExpressionAnnotation(
            AnnotationSource source,
            Class<? extends Annotation> annotationType
    ) {
        if (annotationType == null) {
            return SecurityResolution.UNKNOWN;
        }

        Annotation annotation = source.get(annotationType);
        if (annotation == null) {
            return SecurityResolution.UNKNOWN;
        }

        String expression = readStringValue(annotation);
        if (expression == null || expression.isBlank()) {
            return SecurityResolution.SECURED;
        }

        return resolveAuthorizationExpression(expression);
    }

    private static SecurityResolution resolveAuthorizationExpression(String expression) {
        String compactExpression = expression
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);

        if (isPublicExpression(compactExpression)) {
            return SecurityResolution.PUBLIC;
        }

        if (containsPublicExpression(compactExpression) && containsOrOperator(expression)) {
            return SecurityResolution.PUBLIC;
        }

        return SecurityResolution.SECURED;
    }

    private static boolean isPublicExpression(String expression) {
        return "permitall()".equals(expression) || "isanonymous()".equals(expression);
    }

    private static boolean containsPublicExpression(String expression) {
        return expression.contains("permitall()") || expression.contains("isanonymous()");
    }

    private static boolean containsOrOperator(String expression) {
        return OR_OPERATOR.matcher(expression).find();
    }

    private static SecurityResolution resolveSecuredAnnotation(AnnotationSource source) {
        if (SECURED_ANNOTATION == null) {
            return SecurityResolution.UNKNOWN;
        }

        Annotation annotation = source.get(SECURED_ANNOTATION);
        if (annotation == null) {
            return SecurityResolution.UNKNOWN;
        }

        String[] attributes = readStringArrayValue(annotation);
        if (attributes.length == 0) {
            return SecurityResolution.SECURED;
        }

        return Arrays.stream(attributes).allMatch(EndpointMetadataResolver::isAnonymousAttribute)
                ? SecurityResolution.PUBLIC
                : SecurityResolution.SECURED;
    }

    private static SecurityResolution resolveRolesAllowedAnnotation(
            AnnotationSource source,
            Class<? extends Annotation> annotationType
    ) {
        if (annotationType == null) {
            return SecurityResolution.UNKNOWN;
        }

        Annotation annotation = source.get(annotationType);
        if (annotation == null) {
            return SecurityResolution.UNKNOWN;
        }

        return readStringArrayValue(annotation).length == 0
                ? SecurityResolution.UNKNOWN
                : SecurityResolution.SECURED;
    }

    private static boolean isAnonymousAttribute(String attribute) {
        return "IS_AUTHENTICATED_ANONYMOUSLY".equalsIgnoreCase(attribute);
    }

    private static String readStringValue(Annotation annotation) {
        Object value = readValue(annotation);
        return value instanceof String ? (String) value : null;
    }

    private static String[] readStringArrayValue(Annotation annotation) {
        Object value = readValue(annotation);
        return value instanceof String[] ? (String[]) value : new String[0];
    }

    private static Object readValue(Annotation annotation) {
        try {
            return annotation.annotationType().getMethod("value").invoke(annotation);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private static SecurityResolution strongest(SecurityResolution... resolutions) {
        SecurityResolution strongest = SecurityResolution.UNKNOWN;

        for (SecurityResolution resolution : resolutions) {
            if (resolution == SecurityResolution.SECURED) {
                return SecurityResolution.SECURED;
            }
            if (resolution == SecurityResolution.PUBLIC) {
                strongest = SecurityResolution.PUBLIC;
            }
        }

        return strongest;
    }

    @FunctionalInterface
    private interface AnnotationSource {
        <A extends Annotation> A get(Class<A> annotationType);
    }

    private enum SecurityResolution {
        SECURED,
        PUBLIC,
        UNKNOWN
    }
}
