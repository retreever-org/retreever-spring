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
 * Resolves Input/Output schema and parameter metadata for a controller method.
 * Handles request body resolution, response body resolution, and provides
 * stubs for path variables, query parameters, and headers.
 */
public class ApiEndpointIOResolver {

    private final ApiBodySchemaResolver bodyResolver;
    private final ApiHeaderResolver headerResolver;

    public ApiEndpointIOResolver(SchemaRegistry schemaRegistry,
                                 ApiHeaderRegistry apiHeaderRegistry) {
        this.bodyResolver = new ApiBodySchemaResolver(schemaRegistry);
        this.headerResolver = new ApiHeaderResolver(apiHeaderRegistry);
    }

    public void resolve(ApiEndpoint endpoint, Method method) {
        ApiPathVariableResolver.resolvePathVariables(endpoint, method);
        ApiQueryParamResolver.resolveQueryParams(endpoint, method);

        headerResolver.resolveHeaders(endpoint, method);
        bodyResolver.resolve(endpoint, method);
    }
}