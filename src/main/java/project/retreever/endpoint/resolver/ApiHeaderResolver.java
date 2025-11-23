/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.endpoint.resolver;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import project.retreever.domain.annotation.ApiEndpoint;
import project.retreever.domain.model.ApiHeader;
import project.retreever.schema.resolver.JsonPropertyTypeResolver;
import project.retreever.repo.ApiHeaderRegistry;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Resolves all headers for an API endpoint:
 * 1. Spring mapping-level headers
 * 2. @RequestHeader parameters
 * 3. @ApiEndpoint(headers={}) registry lookups
 */
public class ApiHeaderResolver {

    private final ApiHeaderRegistry registry;

    public ApiHeaderResolver(ApiHeaderRegistry registry) {
        this.registry = registry;
    }

    public void resolveHeaders(project.retreever.domain.model.ApiEndpoint endpoint, Method method) {

        List<ApiHeader> result = new ArrayList<>();

        // ---- 1. Mapping Headers ----
        result.addAll(resolveMappingHeaders(method));

        // ---- 2. @RequestHeader params ----
        result.addAll(resolveRequestHeaderParams(method));

        // ---- 3. @ApiEndpoint(headers={}) ----
        result.addAll(resolveCustomHeaderRefs(method));

        // ---- Deduplicate by exact header name ----
        Map<String, ApiHeader> unique = new LinkedHashMap<>();
        for (ApiHeader h : result) {
            if (h.getName() != null) {
                unique.putIfAbsent(h.getName(), h); // case-sensitive
            }
        }

        endpoint.setHeaders(new ArrayList<>(unique.values()));
    }

    // ------------------------------------------------------------------------
    // 1. Headers declared in @RequestMapping / @GetMapping(headers=...)
    // ------------------------------------------------------------------------
    private List<ApiHeader> resolveMappingHeaders(Method method) {

        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
        if (mapping == null || mapping.headers().length == 0) {
            return Collections.emptyList();
        }

        List<ApiHeader> list = new ArrayList<>();

        for (String header : mapping.headers()) {
            // format can be "Key=Value" or "Key"
            String[] parts = header.split("=", 2);
            String name = parts[0].trim();

            ApiHeader h = new ApiHeader()
                    .setName(name)
                    .setType(JsonPropertyTypeResolver.resolve(String.class)) // always STRING
                    .setRequired(true);

            list.add(h);
        }

        return list;
    }

    // ------------------------------------------------------------------------
    // 2. Headers via method parameters: @RequestHeader
    // ------------------------------------------------------------------------
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

            // optional description via your @Description annotation
            project.retreever.domain.annotation.Description desc =
                    p.getAnnotation(project.retreever.domain.annotation.Description.class);
            if (desc != null) header.setDescription(desc.value());

            list.add(header);
        }

        return list;
    }

    // ------------------------------------------------------------------------
    // 3. Custom headers referenced in @ApiEndpoint(headers={})
    //    Resolved using ApiHeaderRegistry by exact name.
    // ------------------------------------------------------------------------
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
            }
        }

        return list;
    }
}
