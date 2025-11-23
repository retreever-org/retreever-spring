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

public class RetreeverOrchestrator {

    private final ApiDocumentAssembler assembler;
    private final ApiDocResolver docResolver;

    public RetreeverOrchestrator() {

        // ─────────────────────────────────────────────
        // Low-level schema
        // ─────────────────────────────────────────────
        JsonSchemaResolver jsonSchemaResolver = new JsonSchemaResolver();
        SchemaRegistry schemaRegistry = new SchemaRegistry(jsonSchemaResolver);

        // ─────────────────────────────────────────────
        // Headers
        // ─────────────────────────────────────────────
        ApiHeaderRegistry headerRegistry = new ApiHeaderRegistry();

        // ─────────────────────────────────────────────
        // Errors
        // ─────────────────────────────────────────────
        ApiErrorResolver errorResolver = new ApiErrorResolver(jsonSchemaResolver);
        ApiErrorRegistry errorRegistry = new ApiErrorRegistry(errorResolver);

        // ─────────────────────────────────────────────
        // Endpoint IO + Endpoint Resolver
        // ─────────────────────────────────────────────
        ApiEndpointIOResolver ioResolver =
                new ApiEndpointIOResolver(schemaRegistry, headerRegistry);

        ApiEndpointResolver endpointResolver =
                new ApiEndpointResolver(schemaRegistry, headerRegistry, errorRegistry);

        // ─────────────────────────────────────────────
        // Group & AppDoc Resolver
        // ─────────────────────────────────────────────
        ApiGroupResolver groupResolver = new ApiGroupResolver(endpointResolver);
        this.docResolver = new ApiDocResolver(groupResolver);

        // ─────────────────────────────────────────────
        // View Layer helpers (lookup + DTO transformation)
        // ─────────────────────────────────────────────
        SchemaLookupService lookup = new SchemaLookupService(schemaRegistry, errorRegistry);
        this.assembler = new ApiDocumentAssembler(lookup);
    }

    // ─────────────────────────────────────────────
    // Builds the full API Document
    // ─────────────────────────────────────────────
    public ApiDocument build(Class<?> applicationClass, Set<Class<?>> controllers) {
        var doc = docResolver.resolve(applicationClass, controllers);
        return assembler.assemble(doc);
    }
}
