/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.endpoint.resolver;

import project.retreever.domain.model.ApiEndpoint;
import project.retreever.repo.ApiErrorRegistry;
import project.retreever.repo.ApiHeaderRegistry;
import project.retreever.repo.SchemaRegistry;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Main coordinator responsible for resolving a complete {@link ApiEndpoint}
 * from a controller method. Aggregates metadata, path/method info,
 * content types, IO schemas, parameters, and error references.
 */
public class ApiEndpointResolver {

    private final ApiEndpointIOResolver endpointIOResolver;
    private final ApiErrorRegistry apiErrorRegistry;

    public ApiEndpointResolver(SchemaRegistry schemaRegistry,
                               ApiHeaderRegistry headerRegistry,
                               ApiErrorRegistry apiErrorRegistry) {
        this.apiErrorRegistry = apiErrorRegistry;
        this.endpointIOResolver = new ApiEndpointIOResolver(schemaRegistry, headerRegistry);
    }

    /**
     * Resolves and constructs an {@link ApiEndpoint} representation
     * from the given controller method.
     * <p>
     * Steps:
     * <ol>
     *     <li>Metadata (name, description, security, status, deprecation)</li>
     *     <li>Path and HTTP method</li>
     *     <li>Consumes/produces media types</li>
     *     <li>Request/response schemas, parameters, headers</li>
     *     <li>Error references from {@code @ApiEndpoint(errors={})}</li>
     * </ol>
     *
     * @param method the controller method to analyze
     * @return fully resolved endpoint model
     */
    public ApiEndpoint resolve(Method method) {
        ApiEndpoint endpoint = new ApiEndpoint();

        // 1. Name, secured flag, status, description, deprecated
        EndpointMetadataResolver.resolve(endpoint, method);

        // 2. Path and HTTP method
        EndpointPathAndMethodResolver.resolve(endpoint, method);

        // 3. Consumes/produces media types
        EndpointContentTypeResolver.resolve(endpoint, method);

        // 4. Request schema, response schema, path vars, query params, headers
        endpointIOResolver.resolve(endpoint, method);

        // 5. Apply error refs from annotation, only if already registered
        project.retreever.domain.annotation.ApiEndpoint ann =
                method.getAnnotation(project.retreever.domain.annotation.ApiEndpoint.class);

        if (ann != null) {
            Class<? extends Throwable>[] errors = ann.errors();
            List<String> errorRefs = apiErrorRegistry.getErrorRefs(errors);
            endpoint.setErrorRefs(errorRefs == null ? List.of() : errorRefs);
        }

        return endpoint;
    }
}
