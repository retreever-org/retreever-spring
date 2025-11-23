package project.retreever.endpoint.resolver;

import org.springframework.http.HttpStatus;
import project.retreever.domain.annotation.ApiEndpoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

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

    public static void resolve(
            project.retreever.domain.model.ApiEndpoint endpoint,
            Method method
    ) {

        // ---- 1. Resolve @ApiEndpoint annotation ----
        ApiEndpoint annotation = method.getAnnotation(ApiEndpoint.class);

        // ---- NAME ----
        if (annotation != null && !annotation.name().isBlank()) {
            endpoint.setName(annotation.name());
        } else {
            endpoint.setName(prettifyName(method.getName()));
        }

        // ---- SECURED ----
        if (annotation != null && annotation.secured()) {
            endpoint.secure();
        }
        else if (PRE_AUTHORIZE_ANNOTATION != null &&
                method.isAnnotationPresent(PRE_AUTHORIZE_ANNOTATION)) {
            endpoint.secure();
        }

        // ---- STATUS & DESCRIPTION ----
        if (annotation != null) {
            endpoint.setStatus(annotation.status());
            endpoint.setDescription(annotation.description());
        } else {
            endpoint.setStatus(HttpStatus.OK);
        }

        // ---- DEPRECATED ----
        if (method.isAnnotationPresent(Deprecated.class)) {
            endpoint.deprecate();
        }
    }

    private static String prettifyName(String raw) {
        if (raw == null || raw.isBlank()) return raw;

        String spaced = raw
                .replace("_", " ")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .trim();

        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }
}
