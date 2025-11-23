/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.endpoint.resolver;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import project.retreever.domain.annotation.Description;
import project.retreever.domain.model.ApiEndpoint;
import project.retreever.domain.model.ApiParam;
import project.retreever.domain.model.JsonPropertyType;
import project.retreever.schema.resolver.JsonPropertyTypeResolver;
import project.retreever.schema.resolver.util.ConstraintResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Resolves query parameters for an endpoint. Extracts name, type,
 * default value, required flag, description, and validation constraints
 * from parameters annotated with {@link RequestParam}.
 */
public class ApiQueryParamResolver {

    /**
     * Populates the endpoint's query parameter metadata based on
     * @RequestParam annotations declared on method parameters.
     *
     * @param endpoint the endpoint to enrich
     * @param method   the controller method to inspect
     */
    public static void resolveQueryParams(ApiEndpoint endpoint, Method method) {

        List<ApiParam> params = new ArrayList<>();

        for (Parameter param : method.getParameters()) {

            RequestParam rp = param.getAnnotation(RequestParam.class);
            if (rp == null) continue;

            ApiParam qp = new ApiParam();

            // Determine parameter name
            String name = rp.name().isBlank()
                    ? (!rp.value().isBlank() ? rp.value() : param.getName())
                    : rp.name();
            qp.setName(name);

            // Determine JSON type
            Class<?> raw = param.getType();
            JsonPropertyType jsonType = JsonPropertyTypeResolver.resolve(raw);
            qp.setType(jsonType);

            // Required flag from annotation
            qp.setRequired(rp.required());

            // Default value
            if (!rp.defaultValue().equals(ValueConstants.DEFAULT_NONE)) {
                qp.setDefaultValue(rp.defaultValue());
            }

            // Optional description from @Description
            Description desc = param.getAnnotation(Description.class);
            if (desc != null) {
                qp.setDescription(desc.value());
            }

            // Validation constraints
            Set<String> constraints = ConstraintResolver.resolve(param.getAnnotations());
            constraints.forEach(qp::addConstraint);

            params.add(qp);
        }

        endpoint.setQueryParams(params);
    }
}
