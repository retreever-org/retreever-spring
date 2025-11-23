/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.group.resolver;

import org.springframework.web.bind.annotation.RestController;
import project.retreever.domain.model.ApiEndpoint;
import project.retreever.domain.model.ApiGroup;
import project.retreever.endpoint.resolver.ApiEndpointResolver;
import project.retreever.endpoint.resolver.EndpointPathAndMethodResolver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ApiGroupResolver {

    private final ApiEndpointResolver endpointResolver;

    public ApiGroupResolver(ApiEndpointResolver endpointResolver) {
        this.endpointResolver = endpointResolver;
    }

    /**
     * Resolves a Controller class into an ApiGroup model.
     */
    public ApiGroup resolve(Class<?> controllerClass) {

        // Must be a Spring controller
        if (!controllerClass.isAnnotationPresent(RestController.class)) {
            return null;
        }

        ApiGroup group = new ApiGroup();

        // ---- Group name & description ----
        project.retreever.domain.annotation.ApiGroup ann = controllerClass.getAnnotation(project.retreever.domain.annotation.ApiGroup.class);

        if (ann != null) {
            group.setName(ann.name());
            group.setDescription(ann.description());
        } else {
            // fallback: prettify class name
            group.setName(prettifyName(controllerClass.getSimpleName()));
            group.setDescription("");
        }

        // ---- Deprecated? ----
        if (controllerClass.isAnnotationPresent(Deprecated.class)) {
            group.deprecate();
        }

        // ---- Resolve endpoints ----
        List<ApiEndpoint> endpoints = new ArrayList<>();

        for (Method method : controllerClass.getDeclaredMethods()) {
            if (EndpointPathAndMethodResolver.resolveHttpMethod(method) != null) {
                ApiEndpoint ep = endpointResolver.resolve(method);
                endpoints.add(ep);
            }
        }

        group.setEndpoints(endpoints);

        return group;
    }

    /**
     * Converts "UserController" -> "User Controller APIs"
     */
    private String prettifyName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "API Group";
        }

        // Remove common controller suffixes
        String name = raw
                .replaceAll("(RestController|ApiController|Controller|Ctrl|Resource|Handler)$", "")
                .replaceAll("_+$", "") // remove trailing underscores
                .trim();

        // Insert spaces between camelCase or PascalCase transitions
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

