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

public class ApiEndpointResolver {

    private final ApiEndpointIOResolver endpointIOResolver;
    private final ApiErrorRegistry apiErrorRegistry;

    public ApiEndpointResolver(SchemaRegistry schemaRegistry,
                               ApiHeaderRegistry headerRegistry,
                               ApiErrorRegistry apiErrorRegistry) {
        this.apiErrorRegistry = apiErrorRegistry;
        this.endpointIOResolver = new ApiEndpointIOResolver(schemaRegistry, headerRegistry);
    }

    public ApiEndpoint resolve(Method method) {
        ApiEndpoint endpoint = new ApiEndpoint();

        // 1. Resolving name, secure?, status, description, deprecated values
        EndpointMetadataResolver.resolve(endpoint, method);

        // 2. Resolving path and method values
        EndpointPathAndMethodResolver.resolve(endpoint, method);

        // 3. Resolving consumes and produces values
        EndpointContentTypeResolver.resolve(endpoint, method);

        /*
        4. Resolving responseSchemaRef, requestSchemaRef, pathVariables, queryParams, and headers
        (while ensuring the schema registration in to the SchemaRegistry)
         */
        endpointIOResolver.resolve(endpoint, method);

        // 5. Mapping ApiError Refs to Endpoint via pre-resolved set of ApiErrors in ApiErrorRegistry.
        project.retreever.domain.annotation.ApiEndpoint ann = method.getAnnotation(project.retreever.domain.annotation.ApiEndpoint.class);
        if (ann != null) {
            Class<? extends Throwable>[] errors = ann.errors();
            List<String> errorRefs = apiErrorRegistry.getErrorRefs(errors);
            endpoint.setErrorRefs(errorRefs == null ? List.of() : errorRefs);
        }

        return endpoint;
    }


}

