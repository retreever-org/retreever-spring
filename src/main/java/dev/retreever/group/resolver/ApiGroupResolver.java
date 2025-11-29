/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.group.resolver;

import dev.retreever.annotation.ApiGroup;
import org.springframework.web.bind.annotation.RestController;
import dev.retreever.endpoint.model.ApiEndpoint;
import dev.retreever.endpoint.resolver.ApiEndpointResolver;
import dev.retreever.endpoint.resolver.EndpointPathAndMethodResolver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves a Spring {@code @RestController} into an {@link dev.retreever.endpoint.model.ApiGroup}.
 * Detects controller metadata, resolves its endpoints, and groups them
 * under a common name and description.
 */
public class ApiGroupResolver {

    private final ApiEndpointResolver endpointResolver;

    public ApiGroupResolver(ApiEndpointResolver endpointResolver) {
        this.endpointResolver = endpointResolver;
    }

    /**
     * Builds an {@link dev.retreever.endpoint.model.ApiGroup} from a controller class.
     * <p>
     * Steps:
     * <ul>
     *     <li>Verify class is annotated with {@code @RestController}</li>
     *     <li>Read {@code @ApiGroup} name/description or derive fallback</li>
     *     <li>Mark deprecated groups if needed</li>
     *     <li>Resolve all valid API endpoints within the class</li>
     * </ul>
     *
     * @param controllerClass Spring REST controller class
     * @return resolved ApiGroup or {@code null} if class is not a controller
     */
    public dev.retreever.endpoint.model.ApiGroup resolve(Class<?> controllerClass) {

        // Must be a Spring controller
        if (!controllerClass.isAnnotationPresent(RestController.class)) {
            return null;
        }

        dev.retreever.endpoint.model.ApiGroup group = new dev.retreever.endpoint.model.ApiGroup();

        // Group name & description
        ApiGroup ann =
                controllerClass.getAnnotation(ApiGroup.class);

        if (ann != null) {
            group.setName(ann.name());
            group.setDescription(ann.description());
        } else {
            // fallback: prettify class name
            group.setName(prettifyName(controllerClass.getSimpleName()));
            group.setDescription("");
        }

        // Deprecated marker
        if (controllerClass.isAnnotationPresent(Deprecated.class)) {
            group.deprecate();
        }

        // Resolve endpoints
        List<ApiEndpoint> endpoints = new ArrayList<>();

        for (Method method : controllerClass.getDeclaredMethods()) {
            // Only consider methods with a valid HTTP mapping
            if (EndpointPathAndMethodResolver.resolveHttpMethod(method) != null) {
                ApiEndpoint ep = endpointResolver.resolve(method);
                endpoints.add(ep);
            }
        }

        group.setEndpoints(endpoints);
        return group;
    }

    /**
     * Converts a controller class name into a clean API group name.
     * Example:
     * <pre>
     *     UserController -> User APIs
     *     AccountRestController -> Account APIs
     * </pre>
     */
    private String prettifyName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "API Group";
        }

        // Remove common controller suffixes
        String name = raw
                .replaceAll("(RestController|ApiController|Controller|Ctrl|Resource|Handler)$", "")
                .replaceAll("_+$", "")
                .trim();

        // Insert spacing for camelCase / PascalCase
        name = name.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2")
                .trim();

        // Capitalize first letter
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        return name + " APIs";
    }
}
