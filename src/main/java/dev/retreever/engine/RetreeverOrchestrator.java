/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.engine;

import dev.retreever.config.RetreeverDocumentationExclusionProperties;
import dev.retreever.auth.RetreeverAuthProperties;
import dev.retreever.config.RetreeverStudioProperties;
import dev.retreever.config.SchemaConfig;
import dev.retreever.doc.resolver.ApiDocResolver;
import dev.retreever.endpoint.model.ApiHeader;
import dev.retreever.endpoint.resolver.ApiEndpointResolver;
import dev.retreever.group.resolver.ApiGroupResolver;
import dev.retreever.repo.ApiErrorRegistry;
import dev.retreever.repo.ApiHeaderRegistry;
import dev.retreever.repo.SchemaRegistry;
import dev.retreever.view.ApiDocumentAssembler;
import dev.retreever.view.dto.ApiDocument;
import org.springframework.util.StringValueResolver;

import java.util.List;
import java.util.Set;

/**
 * Top-level orchestrator coordinating the complete Retreever documentation pipeline.
 * Executes in precise order: Errors → Schemas → Endpoints → Document Assembly.
 */
public class RetreeverOrchestrator {

    private final ApiErrorResolutionOrchestrator apiErrorResolutionOrchestrator;
    private final SchemaResolutionOrchestrator schemaResolutionOrchestrator;
    private final ApiDocumentAssembler assembler;
    private final ApiDocResolver docResolver;
    private final List<String> basePackages; // ✅ Singleton

    public List<String> getBasePackages() {
        return basePackages;
    }

    public RetreeverOrchestrator(
            List<String> basePackages,
            List<ApiHeader> headers,
            RetreeverDocumentationExclusionProperties exclusionProperties,
            RetreeverAuthProperties authProperties,
            RetreeverStudioProperties studioProperties) {
        this(basePackages, headers, exclusionProperties, authProperties, studioProperties, null);
    }

    public RetreeverOrchestrator(
            List<String> basePackages,
            List<ApiHeader> headers,
            RetreeverDocumentationExclusionProperties exclusionProperties,
            RetreeverAuthProperties authProperties,
            RetreeverStudioProperties studioProperties,
            StringValueResolver valueResolver) {
        this.basePackages = basePackages;

        // 1. Initialise config
        SchemaConfig.init(basePackages);

        // 2. Registries (singletons where applicable)
        ApiErrorRegistry errorRegistry = ApiErrorRegistry.getInstance(); // ✅ Singleton
        ApiHeaderRegistry headerRegistry = ApiHeaderRegistry.init(headers);
        SchemaRegistry schemaRegistry = SchemaRegistry.getInstance();

        // 3. Resolver chain (endpoint → group → doc)
        ApiEndpointResolver endpointResolver = new ApiEndpointResolver(headerRegistry, valueResolver);
        ApiGroupResolver groupResolver = new ApiGroupResolver(endpointResolver, exclusionProperties);

        // 4. Orchestrators & Assemblers
        this.apiErrorResolutionOrchestrator = new ApiErrorResolutionOrchestrator(errorRegistry);
        this.schemaResolutionOrchestrator = new SchemaResolutionOrchestrator(
                schemaRegistry,
                exclusionProperties,
                valueResolver
        );
        this.assembler = new ApiDocumentAssembler(schemaRegistry, errorRegistry, authProperties, studioProperties);
        this.docResolver = new ApiDocResolver(groupResolver);
    }

    /**
     * Executes the COMPLETE documentation pipeline in precise order:
     * 1. ApiErrors (ControllerAdvices → ApiErrorRegistry)
     * 2. Schemas (Controllers + Advices → SchemaRegistry)
     * 3. Endpoints (Controllers → ApiEndpoint models)
     * 4. Document Assembly (ApiDoc → ApiDocument DTO)
     */
    public ApiDocument build(Class<?> applicationClass,
                             Set<Class<?>> controllers,
                             Set<Class<?>> controllerAdvices) {

        // === STEP 1: RESOLVE API ERRORS ===
        apiErrorResolutionOrchestrator.resolveAllErrors(controllerAdvices);

        // === STEP 2: RESOLVE SCHEMAS ===
        schemaResolutionOrchestrator.resolveAllSchema(applicationClass, controllers, controllerAdvices);

        // === STEP 3: RESOLVE ENDPOINTS & DOCUMENT ===
        dev.retreever.endpoint.model.ApiDoc apiDoc =
                docResolver.resolve(applicationClass, controllers);

        // === STEP 4: ASSEMBLE FINAL DTO ===
        return assembler.assemble(apiDoc);
    }
}
