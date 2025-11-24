/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.view;

import dev.retreever.domain.model.ApiEndpoint;
import dev.retreever.domain.model.ApiError;
import dev.retreever.domain.model.JsonProperty;
import dev.retreever.repo.ApiErrorRegistry;
import dev.retreever.repo.SchemaRegistry;

import java.util.*;

/**
 * Provides lookup utilities for converting schema references stored in an
 * {@link ApiEndpoint} into fully rendered documentation structures using
 * {@link ApiSchemaRenderer}.
 *
 * <p>Supports lookup for:
 * <ul>
 *     <li>Request body schema</li>
 *     <li>Response body schema</li>
 *     <li>Error response schemas</li>
 * </ul>
 */
public class SchemaLookupService {

    private final SchemaRegistry schemaRegistry;
    private final ApiErrorRegistry apiErrorRegistry;

    /**
     * Creates a new lookup service using the provided registries.
     *
     * @param schemaRegistry   registry holding DTO schema definitions
     * @param apiErrorRegistry registry holding resolved ApiError definitions
     */
    public SchemaLookupService(SchemaRegistry schemaRegistry,
                               ApiErrorRegistry apiErrorRegistry) {
        this.schemaRegistry = schemaRegistry;
        this.apiErrorRegistry = apiErrorRegistry;
    }

    /**
     * Resolves and renders the request body schema for the given endpoint.
     *
     * @param endpoint API endpoint descriptor
     * @return rendered schema map or {@code null} if no request schema exists
     */
    public Map<String, Object> resolveRequest(ApiEndpoint endpoint) {
        String ref = endpoint.getRequestSchemaRef();
        if (ref == null) return null;

        List<JsonProperty> props = schemaRegistry.get(ref);
        if (props == null) return null;

        return ApiSchemaRenderer.execute(props, ApiSchemaRenderer.SchemaType.REQUEST);
    }

    /**
     * Resolves and renders the response body schema for the given endpoint.
     *
     * @param endpoint API endpoint descriptor
     * @return rendered schema map or {@code null} if no response schema exists
     */
    public Map<String, Object> resolveResponse(ApiEndpoint endpoint) {
        String ref = endpoint.getResponseSchemaRef();
        if (ref == null) return null;

        List<JsonProperty> props = schemaRegistry.get(ref);
        if (props == null) return null;

        return ApiSchemaRenderer.execute(props, ApiSchemaRenderer.SchemaType.RESPONSE);
    }

    /**
     * Resolves and renders all error response schemas associated with the endpoint.
     *
     * @param endpoint API endpoint descriptor
     * @return list of rendered error maps, empty if no errors defined
     */
    public List<Map<String, Object>> resolveErrors(ApiEndpoint endpoint) {

        List<String> refs = endpoint.getErrorRefs();
        if (refs == null || refs.isEmpty()) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();

        for (String ref : refs) {
            ApiError err = apiErrorRegistry.get(ref);
            if (err == null) continue;
            result.add(renderError(err));
        }

        return result;
    }

    /**
     * Builds the rendered structure for a single {@link ApiError}.
     *
     * @param err ApiError definition model
     * @return rendered error documentation structure
     */
    private Map<String, Object> renderError(ApiError err) {

        Map<String, Object> out = new LinkedHashMap<>();

        out.put("status", err.getStatus().toString());
        out.put("status_code", err.getStatus().value());
        out.put("description", err.getDescription());
        out.put("error_code", err.getErrorCode());

        List<JsonProperty> props = err.getErrorBody();
        if (props == null || props.isEmpty()) {
            out.put("response", null);
        } else {
            out.put("response", ApiSchemaRenderer.execute(props, ApiSchemaRenderer.SchemaType.RESPONSE));
        }

        return out;
    }
}
