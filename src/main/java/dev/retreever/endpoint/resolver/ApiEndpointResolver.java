/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.resolver;

import dev.retreever.domain.annotation.ApiEndpoint;
import dev.retreever.repo.ApiErrorRegistry;
import dev.retreever.repo.ApiHeaderRegistry;
import dev.retreever.repo.SchemaRegistry;
import dev.retreever.schema.resolver.JsonSchemaResolver;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Main coordinator responsible for resolving a complete {@link dev.retreever.domain.model.ApiEndpoint}
 * from a controller method. Aggregates metadata, path/method info,
 * content types, IO schemas, parameters, and error references.
 */
public class ApiEndpointResolver {

    private final ApiEndpointIOResolver endpointIOResolver;
    private final ApiErrorRegistry apiErrorRegistry;

    public ApiEndpointResolver(SchemaRegistry schemaRegistry,
                               ApiHeaderRegistry headerRegistry,
                               ApiErrorRegistry apiErrorRegistry,
                               JsonSchemaResolver schemaResolver) {
        this.apiErrorRegistry = apiErrorRegistry;
        this.endpointIOResolver = new ApiEndpointIOResolver(schemaRegistry, headerRegistry, schemaResolver);
    }

    /**
     * Resolves and constructs an {@link dev.retreever.domain.model.ApiEndpoint} representation
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
    public dev.retreever.domain.model.ApiEndpoint resolve(Method method) {
        dev.retreever.domain.model.ApiEndpoint endpoint = new dev.retreever.domain.model.ApiEndpoint();

        // 1. Name, secured flag, status, description, deprecated
        EndpointMetadataResolver.resolve(endpoint, method);

        // 2. Path and HTTP method
        EndpointPathAndMethodResolver.resolve(endpoint, method);

        // 3. Consumes/produces media types
        EndpointContentTypeResolver.resolve(endpoint, method);

        // 4. Request schema, response schema, path vars, query params, headers
        endpointIOResolver.resolve(endpoint, method);

        // 5. Apply error refs from annotation, only if already registered
        ApiEndpoint ann =
                method.getAnnotation(ApiEndpoint.class);

        if (ann != null) {
            Class<? extends Throwable>[] errors = ann.errors();
            List<String> errorRefs = apiErrorRegistry.getErrorRefs(errors);
            endpoint.setErrorRefs(errorRefs == null ? List.of() : errorRefs);
        }

        return endpoint;
    }
}
