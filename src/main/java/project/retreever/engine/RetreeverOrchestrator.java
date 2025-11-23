/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.engine;

import project.retreever.doc.resolver.ApiDocResolver;
import project.retreever.endpoint.resolver.*;
import project.retreever.group.resolver.ApiGroupResolver;
import project.retreever.repo.ApiErrorRegistry;
import project.retreever.repo.ApiHeaderRegistry;
import project.retreever.repo.SchemaRegistry;
import project.retreever.schema.resolver.JsonSchemaResolver;
import project.retreever.view.ApiDocumentAssembler;
import project.retreever.view.SchemaLookupService;
import project.retreever.view.dto.ApiDocument;

import java.util.Set;

/**
 * Coordinates the Retreever pipeline and drives the full documentation build.
 */
public class RetreeverOrchestrator {

    private final ApiDocumentAssembler assembler;
    private final ApiDocResolver docResolver;

    /**
     * Constructs the orchestrator and wires all core components.
     */
    public RetreeverOrchestrator() {

        // JSON schema generator + registry
        JsonSchemaResolver jsonSchemaResolver = new JsonSchemaResolver();
        SchemaRegistry schemaRegistry = new SchemaRegistry(jsonSchemaResolver);

        // Shared header definitions
        ApiHeaderRegistry headerRegistry = new ApiHeaderRegistry();

        // Error resolver + registry for @ExceptionHandler mappings
        ApiErrorResolver errorResolver = new ApiErrorResolver(jsonSchemaResolver);
        ApiErrorRegistry errorRegistry = new ApiErrorRegistry(errorResolver);

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
    public ApiDocument build(Class<?> applicationClass, Set<Class<?>> controllers) {
        var doc = docResolver.resolve(applicationClass, controllers);
        return assembler.assemble(doc);
    }
}
