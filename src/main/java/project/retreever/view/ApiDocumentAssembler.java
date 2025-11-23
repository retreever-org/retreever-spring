/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.view;

import project.retreever.domain.model.*;
import project.retreever.view.dto.ApiDocument;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts ApiDoc (internal model) into ApiDocument (DTO for serialization).
 */
public class ApiDocumentAssembler {

    private final SchemaLookupService schemaLookupService;

    public ApiDocumentAssembler(SchemaLookupService schemaLookupService) {
        this.schemaLookupService = schemaLookupService;
    }

    /**
     * Converts entire ApiDoc into ApiDocument ready for JSON serialization.
     */
    public ApiDocument assemble(ApiDoc source) {

        List<ApiDocument.ApiGroup> groups = new ArrayList<>();

        for (ApiGroup g : source.getGroups()) {
            ApiDocument.ApiGroup dto = mapGroup(g);
            groups.add(dto);
        }

        return new ApiDocument(
                source.getName(),
                source.getDescription(),
                source.getVersion(),
                source.getUriPrefix(),
                Instant.now(),
                groups
        );
    }

    // ----------------------------------------------------------------------
    // GROUP MAPPING
    // ----------------------------------------------------------------------
    private ApiDocument.ApiGroup mapGroup(ApiGroup g) {

        List<ApiDocument.Endpoint> endpoints = new ArrayList<>();

        for (ApiEndpoint ep : g.getEndpoints()) {
            endpoints.add(mapEndpoint(ep));
        }

        return new ApiDocument.ApiGroup(
                g.getName(),
                g.getDescription(),
                g.isDeprecated(),
                endpoints
        );
    }

    // ----------------------------------------------------------------------
    // ENDPOINT MAPPING
    // ----------------------------------------------------------------------
    private ApiDocument.Endpoint mapEndpoint(ApiEndpoint ep) {

        Map<String, Object> requestSchema =
                schemaLookupService.resolveRequest(ep);

        Map<String, Object> responseSchema =
                schemaLookupService.resolveResponse(ep);

        List<Map<String, Object>> errorSchemas =
                schemaLookupService.resolveErrors(ep);

        return new ApiDocument.Endpoint(
                ep.getName(),
                ep.isDeprecated(),
                ep.getDescription(),
                ep.isSecured(),
                ep.getHttpMethod(),
                ep.getPath(),
                ep.getStatus().toString(),
                ep.getStatus().value(),
                ep.getConsumes(),
                ep.getProduces(),
                mapPathVariables(ep.getPathVariables()),
                mapParams(ep.getQueryParams()),
                mapHeaders(ep.getHeaders()),
                requestSchema,
                responseSchema,
                mapErrors(errorSchemas)
        );
    }

    // ----------------------------------------------------------------------
    // MAPPING: PATH VARIABLES
    // ----------------------------------------------------------------------
    private List<ApiDocument.PathVariable> mapPathVariables(List<ApiPathVariable> vars) {

        if (vars == null) return List.of();

        List<ApiDocument.PathVariable> list = new ArrayList<>();

        for (ApiPathVariable v : vars) {
            list.add(new ApiDocument.PathVariable(
                    v.getName(),
                    v.getType().name().toLowerCase(),
                    v.getDescription(),
                    new ArrayList<>(v.getConstraints())
            ));
        }

        return list;
    }

    // ----------------------------------------------------------------------
    // MAPPING: QUERY PARAMS
    // ----------------------------------------------------------------------
    private List<ApiDocument.Param> mapParams(List<ApiParam> params) {

        if (params == null) return List.of();

        List<ApiDocument.Param> list = new ArrayList<>();

        for (ApiParam p : params) {
            list.add(new ApiDocument.Param(
                    p.getName(),
                    p.getDescription(),
                    p.getType().name().toLowerCase(),
                    p.isRequired() ? "true" : "false",
                    p.getDefaultValue(),
                    new ArrayList<>(p.getConstraints())
            ));
        }

        return list;
    }

    // ----------------------------------------------------------------------
    // MAPPING: HEADERS
    // ----------------------------------------------------------------------
    private List<ApiDocument.Header> mapHeaders(List<ApiHeader> headers) {

        if (headers == null) return List.of();

        List<ApiDocument.Header> list = new ArrayList<>();

        for (ApiHeader h : headers) {
            list.add(new ApiDocument.Header(
                    h.getName(),
                    h.getType().name().toLowerCase(),
                    h.isRequired() ? "true" : "false",
                    h.getDescription()
            ));
        }

        return list;
    }

    // ----------------------------------------------------------------------
    // MAPPING: ERRORS
    // ----------------------------------------------------------------------
    private List<ApiDocument.Error> mapErrors(List<Map<String, Object>> errors) {

        if (errors == null) return List.of();

        List<ApiDocument.Error> list = new ArrayList<>();

        for (Map<String, Object> e : errors) {
            list.add(new ApiDocument.Error(
                    (String) e.get("status"),
                    (Integer) e.get("status_code"),
                    (String) e.get("description"),
                    (String) e.get("error_code"),
                    safeMap(e.get("response"))
            ));
        }

        return list;
    }

    private static Map<String, Object> safeMap(Object obj) {
        if (obj instanceof Map<?, ?> raw) {
            Map<String, Object> result = new LinkedHashMap<>();
            raw.forEach((k, v) -> {
                if (k instanceof String key) {
                    result.put(key, v);
                }
            });
            return result;
        }
        return null; // or Collections.emptyMap()
    }
}
