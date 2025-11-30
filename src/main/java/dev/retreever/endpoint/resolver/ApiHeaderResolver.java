/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.resolver;

import dev.retreever.annotation.Description;
import dev.retreever.schema.model.JsonPropertyType;
import dev.retreever.schema.resolver.JsonPropertyTypeResolver;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import dev.retreever.annotation.ApiEndpoint;
import dev.retreever.endpoint.model.ApiHeader;
import dev.retreever.repo.ApiHeaderRegistry;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Resolves all headers used by an API endpoint.
 * Combines headers declared in Spring mappings, method parameters,
 * and custom header references from @ApiEndpoint.
 */
public class ApiHeaderResolver {

    private final ApiHeaderRegistry registry;

    public ApiHeaderResolver(ApiHeaderRegistry registry) {
        this.registry = registry;
    }

    /**
     * Resolves and attaches all headers for the given endpoint.
     * Processing order:
     * <ol>
     *     <li>Headers declared in @RequestMapping</li>
     *     <li>Method parameters annotated with @RequestHeader</li>
     *     <li>Custom headers referenced via @ApiEndpoint</li>
     * </ol>
     *
     * @param endpoint the endpoint being enriched
     * @param method   the controller method being inspected
     */
    public void resolveHeaders(dev.retreever.endpoint.model.ApiEndpoint endpoint, Method method) {
        List<ApiHeader> mappingHeaders = resolveMappingHeaders(method);
        List<ApiHeader> requestHeaders = resolveRequestHeaderParams(method);
        List<ApiHeader> customHeaders = resolveCustomHeaderRefs(method);

        // Priority: customHeaders for description, mapping/request for required
        Map<String, ApiHeader> merged = mergeHeaders(mappingHeaders, requestHeaders, customHeaders);

        endpoint.setHeaders(new ArrayList<>(merged.values()));
    }

    private Map<String, ApiHeader> mergeHeaders(
            List<ApiHeader> mappingHeaders,
            List<ApiHeader> requestHeaders,
            List<ApiHeader> customHeaders
    ) {
        Map<String, ApiHeader> result = new LinkedHashMap<>();

        // 1. Start with ALL headers from all sources (preserve order)
        mappingHeaders.stream().filter(h -> h.getName() != null).forEach(h -> initializeHeader(result, h));
        requestHeaders.stream().filter(h -> h.getName() != null).forEach(h -> initializeHeader(result, h));
        customHeaders.stream().filter(h -> h.getName() != null).forEach(h -> initializeHeader(result, h));

        // 2. Override descriptions from customHeaders (highest priority)
        for (ApiHeader custom : customHeaders) {
            if (custom.getName() != null && custom.getDescription() != null) {
                ApiHeader mergedHeader = result.get(custom.getName());
                if (mergedHeader != null) {
                    mergedHeader.setDescription(custom.getDescription());
                }
            }
        }

        // 3. Override required status from mapping/request (highest priority)
        for (ApiHeader mapping : mappingHeaders) {
            if (mapping.getName() != null) {
                ApiHeader mergedHeader = result.get(mapping.getName());
                if (mergedHeader != null) {
                    mergedHeader.setRequired(mapping.isRequired());
                }
            }
        }

        for (ApiHeader request : requestHeaders) {
            if (request.getName() != null) {
                ApiHeader mergedHeader = result.get(request.getName());
                if (mergedHeader != null) {
                    mergedHeader.setRequired(request.isRequired());
                }
            }
        }

        return result;
    }

    private void initializeHeader(Map<String, ApiHeader> result, ApiHeader header) {
        String name = header.getName();
        ApiHeader existing = result.get(name);

        if (existing == null) {
            // New header - copy all fields
            result.put(name, new ApiHeader()
                    .setName(name)
                    .setType(header.getType())
                    .setRequired(header.isRequired())
                    .setDescription(header.getDescription()));
        }
    }


    /**
     * Extracts header definitions declared directly on
     * mapping annotations (e.g. @RequestMapping(headers="X-Auth")).
     *
     * @param method the controller method
     * @return list of resolved mapping-level headers
     */
    private List<ApiHeader> resolveMappingHeaders(Method method) {

        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
        if (mapping == null || mapping.headers().length == 0) {
            return Collections.emptyList();
        }

        List<ApiHeader> list = new ArrayList<>();

        for (String header : mapping.headers()) {
            String[] parts = header.split("=", 2);
            String name = parts[0].trim();

            ApiHeader h = new ApiHeader()
                    .setName(name)
                    .setType(JsonPropertyTypeResolver.resolve(String.class))
                    .setRequired(true);

            list.add(h);
        }

        return list;
    }

    /**
     * Resolves headers defined via method parameters annotated
     * with @RequestHeader, including optional descriptions.
     *
     * @param method controller method
     * @return list of resolved parameter headers
     */
    private List<ApiHeader> resolveRequestHeaderParams(Method method) {

        List<ApiHeader> list = new ArrayList<>();

        for (Parameter p : method.getParameters()) {

            RequestHeader ann = p.getAnnotation(RequestHeader.class);
            if (ann == null) continue;

            String name = ann.name().isEmpty() ? ann.value() : ann.name();

            ApiHeader header = new ApiHeader()
                    .setName(name)
                    .setRequired(ann.required())
                    .setType(JsonPropertyTypeResolver.resolve(p.getType()));

            Description desc =
                    p.getAnnotation(Description.class);
            if (desc != null) header.setDescription(desc.value());

            list.add(header);
        }

        return list;
    }

    /**
     * Resolves custom headers referenced via @ApiEndpoint(headers={}),
     * pulling them from the shared ApiHeaderRegistry.
     *
     * @param method controller method
     * @return list of resolved custom headers
     */
    private List<ApiHeader> resolveCustomHeaderRefs(Method method) {

        ApiEndpoint api = method.getAnnotation(ApiEndpoint.class);
        if (api == null || api.headers().length == 0) {
            return Collections.emptyList();
        }

        List<ApiHeader> list = new ArrayList<>();

        for (String name : api.headers()) {
            ApiHeader ref = registry.getHeader(name);
            if (ref != null) {
                list.add(ref);
            } else {
                ApiHeader header = new ApiHeader()
                        .setName(name)
                        .setType(JsonPropertyType.STRING)
                        .setRequired(true);
                list.add(header);
            }
        }

        return list;
    }
}
