/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.resolver;

import dev.retreever.schema.resolver.JsonPropertyTypeResolver;
import org.springframework.web.bind.annotation.PathVariable;
import dev.retreever.annotation.Description;
import dev.retreever.endpoint.model.ApiEndpoint;
import dev.retreever.endpoint.model.ApiPathVariable;
import dev.retreever.schema.model.JsonPropertyType;
import dev.retreever.schema.resolver.util.ConstraintResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Resolves path variable metadata for an endpoint.
 * Extracts name, type, description, and validation constraints
 * from parameters annotated with {@link PathVariable}.
 */
public class ApiPathVariableResolver {

    /**
     * Populates the endpoint's path variable list by scanning parameters
     * annotated with {@link PathVariable}.
     *
     * @param endpoint the endpoint model to enrich
     * @param method   the controller method being inspected
     */
    public static void resolvePathVariables(ApiEndpoint endpoint, Method method) {

        List<ApiPathVariable> vars = new ArrayList<>();

        for (Parameter param : method.getParameters()) {

            PathVariable pv = param.getAnnotation(PathVariable.class);
            if (pv == null) continue;

            ApiPathVariable var = new ApiPathVariable();

            // Determine variable name
            String name = pv.name().isBlank()
                    ? (!pv.value().isBlank() ? pv.value() : param.getName())
                    : pv.name();
            var.setName(name);

            // Resolve JSON type
            Class<?> rawType = param.getType();
            JsonPropertyType jsonType = JsonPropertyTypeResolver.resolve(rawType);
            var.setType(jsonType);

            // Optional description
            Description desc = param.getAnnotation(Description.class);
            if (desc != null) {
                var.setDescription(desc.value());
            }

            // Validation constraints
            Set<String> constraints = ConstraintResolver.resolve(param.getAnnotations());
            constraints.forEach(var::addConstraint);

            vars.add(var);
        }

        endpoint.setPathVariables(vars);
    }
}
