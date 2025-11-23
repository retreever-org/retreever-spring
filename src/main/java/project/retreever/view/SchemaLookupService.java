/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.view;

import project.retreever.domain.model.ApiEndpoint;
import project.retreever.domain.model.ApiError;
import project.retreever.domain.model.JsonProperty;
import project.retreever.repo.ApiErrorRegistry;
import project.retreever.repo.SchemaRegistry;

import java.util.*;

public class SchemaLookupService {

    private final SchemaRegistry schemaRegistry;
    private final ApiErrorRegistry apiErrorRegistry;

    public SchemaLookupService(SchemaRegistry schemaRegistry,
                               ApiErrorRegistry apiErrorRegistry) {
        this.schemaRegistry = schemaRegistry;
        this.apiErrorRegistry = apiErrorRegistry;
    }

    // --------------------------------------------------------------------
    // MAIN FUNCTIONS
    // --------------------------------------------------------------------

    /**
     * Resolves request schema into rendered {model, example_model, metadata}
     */
    public Map<String, Object> resolveRequest(ApiEndpoint endpoint) {
        String ref = endpoint.getRequestSchemaRef();
        if (ref == null) return null;

        List<JsonProperty> props = schemaRegistry.get(ref);
        if (props == null) return null;

        return ApiSchemaRenderer.execute(props);
    }

    /**
     * Resolves response schema into rendered {model, example_model, metadata}
     */
    public Map<String, Object> resolveResponse(ApiEndpoint endpoint) {
        String ref = endpoint.getResponseSchemaRef();
        if (ref == null) return null;

        List<JsonProperty> props = schemaRegistry.get(ref);
        if (props == null) return null;

        return ApiSchemaRenderer.execute(props);
    }

    /**
     * Resolves all error responses (mapped by endpoint.getErrorRefs())
     */
    public List<Map<String, Object>> resolveErrors(ApiEndpoint endpoint) {

        List<String> refs = endpoint.getErrorRefs();
        if (refs == null || refs.isEmpty()) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();

        for (String ref : refs) {
            ApiError err = apiErrorRegistry.get(ref);
            if (err == null) continue;

            Map<String, Object> dto = renderError(err);
            result.add(dto);
        }

        return result;
    }

    // --------------------------------------------------------------------
    // INTERNAL — Build Error DTO
    // --------------------------------------------------------------------

    private Map<String, Object> renderError(ApiError err) {

        Map<String, Object> out = new LinkedHashMap<>();

        out.put("status", err.getStatus().toString());
        out.put("status_code", err.getStatus().value());
        out.put("description", err.getDescription());
        out.put("error_code", err.getErrorCode());

        // build schema for error response body
        List<JsonProperty> props = err.getErrorBody();
        if (props == null || props.isEmpty()) {
            out.put("response", null);
        } else {
            out.put("response", ApiSchemaRenderer.execute(props));
        }

        return out;
    }
}

