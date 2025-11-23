/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.endpoint.resolver;

import project.retreever.domain.model.ApiEndpoint;
import project.retreever.repo.ApiHeaderRegistry;
import project.retreever.repo.SchemaRegistry;

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
                                 ApiHeaderRegistry apiHeaderRegistry) {
        this.bodyResolver = new ApiBodySchemaResolver(schemaRegistry);
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
