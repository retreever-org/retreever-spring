/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 * https://opensource.org/licenses/MIT
 */

package dev.retreever.view;

import dev.retreever.endpoint.model.*;
import dev.retreever.repo.ApiErrorRegistry;
import dev.retreever.repo.SchemaRegistry;
import dev.retreever.schema.model.Schema;
import dev.retreever.schema.resolver.SchemaResolver;
import dev.retreever.view.dto.ApiDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Assembles internal ApiDoc → final ApiDocument DTO for JSON serialization.
 * Integrates SchemaRegistry + ApiErrorRegistry + SchemaViewRenderer.
 * FULL DEBUG LOGGING to diagnose schema resolution issues.
 */
public class ApiDocumentAssembler {

    private static final Logger log = LoggerFactory.getLogger(ApiDocumentAssembler.class);

    private final SchemaRegistry schemaRegistry;
    private final ApiErrorRegistry errorRegistry;

    public ApiDocumentAssembler(SchemaRegistry schemaRegistry, ApiErrorRegistry errorRegistry) {
        this.schemaRegistry = schemaRegistry;
        this.errorRegistry = errorRegistry;
        log.debug("ApiDocumentAssembler initialized - SchemaRegistry: {}, ErrorRegistry: {}",
                schemaRegistry.size(), errorRegistry.size());
    }

    // PUBLIC ENTRY POINT
    public ApiDocument assemble(ApiDoc apiDoc) {
        log.debug("Assembling ApiDocument: {} groups, {} total endpoints",
                apiDoc.getGroups().size(), countTotalEndpoints(apiDoc));

        List<ApiDocument.ApiGroup> groups = apiDoc.getGroups().stream()
                .map(this::mapGroup)
                .collect(Collectors.toList());

        ApiDocument doc = new ApiDocument(
                apiDoc.getName(),
                apiDoc.getDescription(),
                apiDoc.getVersion(),
                apiDoc.getUriPrefix(),
                Instant.now(),
                groups
        );

        log.debug("ApiDocument assembled successfully");
        return doc;
    }

    // GROUP MAPPING
    private ApiDocument.ApiGroup mapGroup(ApiGroup group) {
        log.debug("Mapping group: {} ({} endpoints)", group.getName(), group.getEndpoints().size());
        List<ApiDocument.Endpoint> endpoints = group.getEndpoints().stream()
                .map(this::mapEndpoint)
                .collect(Collectors.toList());

        return new ApiDocument.ApiGroup(
                group.getName(),
                group.getDescription(),
                group.isDeprecated(),
                endpoints
        );
    }

    // ENDPOINT MAPPING (FULL SPEC ALIGNMENT)
    private ApiDocument.Endpoint mapEndpoint(ApiEndpoint endpoint) {
        log.debug("Mapping endpoint: {} {}", endpoint.getHttpMethod(), endpoint.getPath());

        return new ApiDocument.Endpoint(
                endpoint.getName(),
                endpoint.isDeprecated(),
                endpoint.getDescription(),
                endpoint.isSecured(),
                endpoint.getHttpMethod(),
                endpoint.getPath(),
                formatStatus(endpoint.getStatus()),
                endpoint.getStatus().value(),
                safeList(endpoint.getConsumes()),
                safeList(endpoint.getProduces()),
                mapPathVariables(endpoint.getPathVariables()),
                mapQueryParams(endpoint.getQueryParams()),
                mapHeaders(endpoint.getHeaders()),
                renderRequest(endpoint.getRequestBodyType()),
                renderResponse(endpoint.getResponseBodyType()),
                mapErrors(endpoint)
        );
    }

    // SCHEMA RENDERING (WITH FULL DEBUG)
    private Map<String, Object> renderRequest(Type type) {
        return renderSchema(type, "REQUEST");
    }

    private Map<String, Object> renderResponse(Type type) {
        return renderSchema(type, "RESPONSE");
    }

    private Map<String, Object> renderSchema(Type type, String typeName) {
        if (type == null) {
            log.debug("{} TYPE NULL", typeName);
            return null;
        }

        log.debug("Looking for {} schema: {}", typeName, type.getTypeName());

        Schema schema = schemaRegistry.getSchema(type);
        if (schema == null) {
            log.debug("{} SCHEMA MISSING: {} (SchemaRegistry size: {})",
                    typeName, type.getTypeName(), schemaRegistry.size());
            return null;
        }

        log.debug("{} SCHEMA FOUND: {}", typeName, type.getTypeName());
        Map<String, Object> rendered = typeName.equals("REQUEST")
                ? SchemaViewRenderer.renderRequest(schema)
                : SchemaViewRenderer.renderResponse(schema);

        log.debug("{} RENDERED SUCCESSFULLY", typeName);
        return rendered;
    }

    // ERROR MAPPING (ApiErrorRegistry INTEGRATED)
    private List<ApiDocument.Error> mapErrors(ApiEndpoint endpoint) {
        log.debug("Mapping {} errors", endpoint.getErrorTypes().size());
        return endpoint.getErrorTypes().stream()
                .map(this::renderError)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ApiDocument.Error renderError(Type errorType) {
        if (errorType == null) return null;

        // 2. Lookup ApiError from registry
        ApiError apiError = errorRegistry.get(errorType);
        if (apiError == null) {
            log.debug("No ApiError for: {}", errorType.getTypeName());
            return null;
        }

        // 3. Render error body schema (if present)
        Type errorBodyType = apiError.getErrorBodyType();
        Schema schema = schemaRegistry.getSchema(errorBodyType);

        Map<String, Object> response = null;
        if(schema != null) {
            response = SchemaViewRenderer.renderResponse(schema);
            log.debug("Error schema rendered, for type: {}", apiError.getErrorType().getTypeName());
        }
        else log.debug("No Schema found in registry for: {}", errorBodyType.getTypeName());

        // 4. Map to final DTO
        ApiDocument.Error error = new ApiDocument.Error(
                apiError.getStatus().getReasonPhrase(),  // Fixed: getReasonPhrase()
                apiError.getStatus().value(),
                apiError.getDescription(),
                apiError.getErrorCode(),
                response
        );

        log.debug("Error mapped: {} -> {}", apiError.getErrorType().getTypeName(), apiError.getStatus());
        return error;
    }

    // PARAMETER MAPPINGS (OPTIMIZED)
    private List<ApiDocument.PathVariable> mapPathVariables(List<ApiPathVariable> vars) {
        if (vars == null || vars.isEmpty()) return List.of();
        return vars.stream()
                .map(v -> new ApiDocument.PathVariable(
                        v.getName(),
                        v.getType().displayName(),
                        v.getRequired(),
                        new ArrayList<>(v.getConstraints()),
                        v.getDescription()
                ))
                .collect(Collectors.toList());
    }

    private List<ApiDocument.Param> mapQueryParams(List<ApiParam> params) {
        if (params == null || params.isEmpty()) return List.of();
        return params.stream()
                .map(p -> new ApiDocument.Param(
                        p.getName(),
                        p.getDescription(),
                        p.getType().displayName(),
                        p.isRequired(),
                        p.getDefaultValue(),
                        new ArrayList<>(p.getConstraints())
                ))
                .collect(Collectors.toList());
    }

    private List<ApiDocument.Header> mapHeaders(List<ApiHeader> headers) {
        if (headers == null || headers.isEmpty()) return List.of();
        return headers.stream()
                .map(h -> new ApiDocument.Header(
                        h.getName(),
                        h.getType().displayName(),
                        h.isRequired(),
                        h.getDescription()
                ))
                .collect(Collectors.toList());
    }

    // UTILITIES
    private String formatStatus(HttpStatus status) {
        return status != null ? status.getReasonPhrase() : "UNKNOWN";
    }

    private <T> List<T> safeList(List<T> list) {
        return list != null ? list : List.of();
    }

    private int countTotalEndpoints(ApiDoc apiDoc) {
        return apiDoc.getGroups().stream()
                .mapToInt(g -> g.getEndpoints().size())
                .sum();
    }
}
