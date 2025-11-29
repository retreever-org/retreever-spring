package dev.retreever.endpoint.resolver;

import dev.retreever.endpoint.model.ApiEndpoint;
import dev.retreever.repo.ApiHeaderRegistry;

import java.lang.reflect.Method;

/**
 * Aggregates all input/output metadata resolution for an API endpoint.
 * Delegates to resolvers for:
 * <ul>
 *     <li>Path variables</li>
 *     <li>Query parameters</li>
 *     <li>Headers</li>
 *     <li>Request/Response body Types (NOT schemas)</li>
 * </ul>
 */
public class ApiEndpointIOResolver {

    private final ApiBodySchemaResolver bodyResolver;
    private final ApiHeaderResolver headerResolver;

    public ApiEndpointIOResolver(ApiHeaderRegistry apiHeaderRegistry) {
        this.bodyResolver = new ApiBodySchemaResolver();
        this.headerResolver = new ApiHeaderResolver(apiHeaderRegistry);
    }

    /**
     * Resolves all I/O components for the given endpoint:
     * path variables, query params, headers, and body types.
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
