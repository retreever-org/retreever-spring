/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.engine;

import dev.retreever.doc.resolver.ApiDocResolver;
import dev.retreever.endpoint.resolver.ApiEndpointIOResolver;
import dev.retreever.endpoint.resolver.ApiEndpointResolver;
import dev.retreever.endpoint.resolver.ApiErrorResolver;
import dev.retreever.endpoint.resolver.*;
import dev.retreever.group.resolver.ApiGroupResolver;
import dev.retreever.repo.ApiErrorRegistry;
import dev.retreever.repo.ApiHeaderRegistry;
import dev.retreever.repo.SchemaRegistry;
import dev.retreever.schema.resolver.JsonSchemaResolver;
import dev.retreever.view.ApiDocumentAssembler;
import dev.retreever.view.SchemaLookupService;
import dev.retreever.view.dto.ApiDocument;

import java.util.List;
import java.util.Set;

/**
 * Coordinates the Retreever pipeline and drives the full documentation build.
 */
public class RetreeverOrchestrator {

    private final ApiDocumentAssembler assembler;
    private final ApiErrorResolver errorResolver;
    private final ApiDocResolver docResolver;

    /**
     * Constructs the orchestrator and wires all core components.
     */
    public RetreeverOrchestrator(List<String> basePackages) {

        // JSON schema generator + registry
        JsonSchemaResolver jsonSchemaResolver = new JsonSchemaResolver(basePackages);
        SchemaRegistry schemaRegistry = new SchemaRegistry(jsonSchemaResolver);

        // Shared header definitions
        ApiHeaderRegistry headerRegistry = new ApiHeaderRegistry();

        // Error resolver + registry for @ExceptionHandler mappings
        ApiErrorRegistry errorRegistry = new ApiErrorRegistry();
        this.errorResolver = new ApiErrorResolver(jsonSchemaResolver, errorRegistry);

        // Request/response schema + header/param/path-var resolution
        ApiEndpointIOResolver ioResolver =
                new ApiEndpointIOResolver(schemaRegistry, headerRegistry);

        // Full endpoint resolver (metadata + IO + errors)
        ApiEndpointResolver endpointResolver =
                new ApiEndpointResolver(schemaRegistry, headerRegistry, errorRegistry);

        // Controller → ApiGroup resolver
        ApiGroupResolver groupResolver = new ApiGroupResolver(endpointResolver);

        // Application-level documentation resolver
        this.docResolver = new ApiDocResolver(groupResolver);

        // Converts internal schemas to renderable model/example/metadata DTO
        SchemaLookupService lookup =
                new SchemaLookupService(schemaRegistry, errorRegistry);

        // Builds final ApiDocument DTO for serialization
        this.assembler = new ApiDocumentAssembler(lookup);
    }

    /**
     * Builds the full API documentation output.
     *
     * @param applicationClass root application class
     * @param controllers      detected controller classes
     * @return full assembled ApiDocument DTO
     */
    public ApiDocument build(Class<?> applicationClass, Set<Class<?>> controllers, Set<Class<?>> controllerAdvices) {
        errorResolver.resolve(controllerAdvices);
        var doc = docResolver.resolve(applicationClass, controllers);
        return assembler.assemble(doc);
    }
}
