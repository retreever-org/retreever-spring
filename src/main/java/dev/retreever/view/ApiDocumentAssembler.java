package dev.retreever.view;

import dev.retreever.endpoint.model.*;
import dev.retreever.repo.ApiErrorRegistry;
import dev.retreever.repo.SchemaRegistry;
import dev.retreever.schema.model.Schema;
import dev.retreever.view.dto.ApiDocument;

import java.time.Instant;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiDocumentAssembler {

    private final SchemaRegistry registry;
    private final ApiErrorRegistry errorRegistry;

    public ApiDocumentAssembler(SchemaRegistry registry, ApiErrorRegistry errorRegistry) {
        this.registry = registry;
        this.errorRegistry = errorRegistry;
    }

    // PUBLIC ENTRY
    public ApiDocument assemble(ApiDoc doc) {

        List<ApiDocument.ApiGroup> groups = new ArrayList<>();

        for (ApiGroup g : doc.getGroups()) {
            groups.add(mapGroup(g));
        }

        return new ApiDocument(
                doc.getName(),
                doc.getDescription(),
                doc.getVersion(),
                doc.getUriPrefix(),
                Instant.now(),
                groups
        );
    }

    // GROUP
    private ApiDocument.ApiGroup mapGroup(ApiGroup group) {

        List<ApiDocument.Endpoint> endpoints = new ArrayList<>();

        for (ApiEndpoint ep : group.getEndpoints()) {
            endpoints.add(mapEndpoint(ep));
        }

        return new ApiDocument.ApiGroup(
                group.getName(),
                group.getDescription(),
                group.isDeprecated(),
                endpoints
        );
    }

    // ENDPOINT
    private ApiDocument.Endpoint mapEndpoint(ApiEndpoint ep) {

        Map<String, Object> request = renderRequestSchema(ep.getRequestBodyType());
        Map<String, Object> response = renderResponseSchema(ep.getResponseBodyType());
        List<ApiDocument.Error> errors = renderErrors(ep.getErrorBodyTypes());

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
                mapQueryParams(ep.getQueryParams()),
                mapHeaders(ep.getHeaders()),
                request,
                response,
                errors
        );
    }

    // SCHEMA RENDERING
    private Map<String, Object> renderRequestSchema(Type t) {
        if (t == null) return null;
        Schema s = registry.getResolved(t);
        if (s == null) return null;
        return SchemaViewRenderer.renderRequest(s);
    }

    private Map<String, Object> renderResponseSchema(Type t) {
        if (t == null) return null;
        Schema s = registry.getResolved(t);
        if (s == null) return null;
        return SchemaViewRenderer.renderResponse(s);
    }

    private List<ApiDocument.Error> renderErrors(List<Type> types) {
        if (types == null || types.isEmpty()) return List.of();

        List<ApiDocument.Error> list = new ArrayList<>();

        for (Type t : types) {

            // Lookup key = exception type FQN
            String key = t.getTypeName();

            ApiError apiError = errorRegistry.get(key);

            if (apiError == null) {
                // Unknown error → minimal fallback block
                list.add(new ApiDocument.Error(
                        key,          // status
                        0,            // status_code
                        "",           // description
                        key,          // error_code
                        null          // response model
                ));
                continue;
            }

            // Get the schema of the error body (already resolved in registry)
            Schema bodySchema = null;
            if (apiError.getErrorBodyType() != null) {
                bodySchema = registry.getResolved(apiError.getErrorBodyType());
            }

            Map<String, Object> rendered =
                    (bodySchema == null)
                            ? null
                            : SchemaViewRenderer.renderResponse(bodySchema);

            list.add(new ApiDocument.Error(
                    apiError.getStatus().toString(),
                    apiError.getStatus().value(),
                    apiError.getDescription(),
                    apiError.getErrorCode(),
                    rendered
            ));
        }

        return list;
    }


    // PROPS / PARAMS
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

    private List<ApiDocument.Param> mapQueryParams(List<ApiParam> params) {
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
}
