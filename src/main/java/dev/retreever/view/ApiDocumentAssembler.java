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
 */
public class ApiDocumentAssembler {

    private final SchemaRegistry schemaRegistry;
    private final ApiErrorRegistry errorRegistry;

    public ApiDocumentAssembler(SchemaRegistry schemaRegistry, ApiErrorRegistry errorRegistry) {
        this.schemaRegistry = schemaRegistry;
        this.errorRegistry = errorRegistry;
    }

    // PUBLIC ENTRY POINT
    public ApiDocument assemble(ApiDoc apiDoc) {
        List<ApiDocument.ApiGroup> groups = apiDoc.getGroups().stream()
                .map(this::mapGroup)
                .collect(Collectors.toList());

        return new ApiDocument(
                apiDoc.getName(),
                apiDoc.getDescription(),
                apiDoc.getVersion(),
                apiDoc.getUriPrefix(),
                Instant.now(),
                groups
        );
    }

    // GROUP MAPPING
    private ApiDocument.ApiGroup mapGroup(ApiGroup group) {
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

    // SCHEMA RENDERING
    private Map<String, Object> renderRequest(Type type) {
        if (type == null) return null;
        Schema schema = schemaRegistry.getSchema(type);
        return schema != null ? SchemaViewRenderer.renderRequest(schema) : null;
    }

    private Map<String, Object> renderResponse(Type type) {
        if (type == null) return null;
        Schema schema = schemaRegistry.getSchema(type);
        return schema != null ? SchemaViewRenderer.renderResponse(schema) : null;
    }

    // ERROR MAPPING (ApiErrorRegistry INTEGRATED)
    private List<ApiDocument.Error> mapErrors(ApiEndpoint endpoint) {
        return endpoint.getErrorBodyTypes().stream()
                .map(this::renderError)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ApiDocument.Error renderError(Type errorType) {
        if (errorType == null) return null;

        // 1. Extract exception class from error body type
        Class<?> classType = SchemaResolver.extractRawClass(errorType);
        if (classType == null || !Throwable.class.isAssignableFrom(classType)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Throwable> exceptionClass = (Class<? extends Throwable>) classType;

        // 2. Lookup ApiError from registry
        ApiError apiError = errorRegistry.get(exceptionClass);
        if (apiError == null) return null;

        // 3. Render error body schema (if present)
        Schema schema = null;
        Type errorBodyType = apiError.getErrorBodyType();
        if (errorBodyType != null) {
            schema = schemaRegistry.getSchema(errorBodyType);
        }

        Map<String, Object> response = schema != null
                ? SchemaViewRenderer.renderResponse(schema)
                : null;

        // 4. Map to final DTO
        return new ApiDocument.Error(
                apiError.getStatus().name(),
                apiError.getStatus().value(),
                apiError.getDescription(),
                apiError.getErrorCode(),
                response
        );
    }

    // PARAMETER MAPPINGS
    private List<ApiDocument.PathVariable> mapPathVariables(List<ApiPathVariable> vars) {
        if (vars == null || vars.isEmpty()) return List.of();
        return vars.stream()
                .map(v -> new ApiDocument.PathVariable(
                        v.getName(),
                        v.getType().displayName(),
                        v.getDescription(),
                        new ArrayList<>(v.getConstraints())
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
                        String.valueOf(p.isRequired()),
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
                        String.valueOf(h.isRequired()),
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
}
