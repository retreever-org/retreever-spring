/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.resolver;

import dev.retreever.domain.model.ApiEndpoint;
import dev.retreever.repo.ApiHeaderRegistry;
import dev.retreever.repo.SchemaRegistry;
import dev.retreever.schema.resolver.JsonSchemaResolver;

import java.lang.reflect.Method;

/**
 * Aggregates all input/output metadata resolution for an API endpoint.
 * Delegates to resolvers for:
 * <ul>
 *     <li>Path variables</li>
 *     <li>Query parameters</li>
 *     <li>Headers</li>
 *     <li>Request/Response body schemas</li>
 * </ul>
 */
public class ApiEndpointIOResolver {

    private final ApiBodySchemaResolver bodyResolver;
    private final ApiHeaderResolver headerResolver;

    public ApiEndpointIOResolver(SchemaRegistry schemaRegistry,
                                 ApiHeaderRegistry apiHeaderRegistry,
                                 JsonSchemaResolver schemaResolver) {
        this.bodyResolver = new ApiBodySchemaResolver(schemaRegistry, schemaResolver);
        this.headerResolver = new ApiHeaderResolver(apiHeaderRegistry);
    }

    /**
     * Resolves all I/O components for the given endpoint:
     * path variables, query params, headers, and body schemas.
     *
     * @param endpoint the endpoint model to populate
     * @param method   the controller method being inspected
     */
    public void resolve(ApiEndpoint endpoint, Method method) {
        ApiPathVariableResolver.resolvePathVariables(endpoint, method);
        ApiQueryParamResolver.resolveQueryParams(endpoint, method);
        headerResolver.resolveHeaders(endpoint, method);
        bodyResolver.resolve(endpoint, method);
    }
}
