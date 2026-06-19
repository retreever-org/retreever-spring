/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.resolver;

import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.*;
import dev.retreever.endpoint.model.ApiEndpoint;

import java.lang.reflect.Method;

/**
 * Resolves the final HTTP path and HTTP method for a controller method.
 * Combines class-level and method-level mappings and normalizes the result.
 */
public class EndpointPathAndMethodResolver {

    private static final StringValueResolver IDENTITY_VALUE_RESOLVER = value -> value;

    /**
     * Populates the endpoint path and HTTP method based on Spring mapping annotations.
     *
     * @param endpoint the endpoint model to fill
     * @param method   the controller method
     */
    public static void resolve(ApiEndpoint endpoint, Method method) {
        resolve(endpoint, method, IDENTITY_VALUE_RESOLVER);
    }

    /**
     * Populates the endpoint path and HTTP method, resolving Spring placeholder
     * expressions in mapping paths before normalizing the final path.
     *
     * @param endpoint      the endpoint model to fill
     * @param method        the controller method
     * @param valueResolver Spring value resolver for embedded placeholders
     */
    public static void resolve(ApiEndpoint endpoint, Method method, StringValueResolver valueResolver) {

        String classPath = resolveValue(resolveClassPath(method), valueResolver);
        String methodPath = resolveValue(resolveMethodPath(method), valueResolver);
        String httpMethod = resolveHttpMethod(method);

        String fullPath = normalizePath(classPath, methodPath);
        endpoint.setPath(fullPath);

        if (httpMethod == null) {
            httpMethod = "GET"; // safe fallback
        }
        endpoint.setHttpMethod(httpMethod.toUpperCase());
    }

    /**
     * Extracts class-level path from @RequestMapping on the controller class.
     */
    private static String resolveClassPath(Method method) {
        RequestMapping mapping = method.getDeclaringClass().getAnnotation(RequestMapping.class);

        if (mapping != null) {
            return firstMappingPath(mapping.value(), mapping.path());
        }
        return "";
    }

    /**
     * Resolves method-level path from mapping annotations.
     */
    public static String resolveMethodPath(Method method) {

        GetMapping get = method.getAnnotation(GetMapping.class);
        if (get != null) {
            return firstMappingPath(get.value(), get.path());
        }

        PostMapping post = method.getAnnotation(PostMapping.class);
        if (post != null) {
            return firstMappingPath(post.value(), post.path());
        }

        PutMapping put = method.getAnnotation(PutMapping.class);
        if (put != null) {
            return firstMappingPath(put.value(), put.path());
        }

        DeleteMapping delete = method.getAnnotation(DeleteMapping.class);
        if (delete != null) {
            return firstMappingPath(delete.value(), delete.path());
        }

        PatchMapping patch = method.getAnnotation(PatchMapping.class);
        if (patch != null) {
            return firstMappingPath(patch.value(), patch.path());
        }

        RequestMapping req = method.getAnnotation(RequestMapping.class);
        if (req != null) {
            return firstMappingPath(req.value(), req.path());
        }

        return "";
    }

    /**
     * Resolves the HTTP method for a given controller method.
     */
    public static String resolveHttpMethod(Method method) {

        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";

        RequestMapping req = method.getAnnotation(RequestMapping.class);
        if (req != null && req.method().length > 0) {
            return req.method()[0].name();
        }

        return null;
    }

    /**
     * Joins class and method paths, ensures leading slashes,
     * and collapses duplicate slashes.
     */
    private static String normalizePath(String base, String sub) {
        if (base == null) base = "";
        if (sub == null) sub = "";

        // ensure leading slash
        if (!base.startsWith("/")) base = "/" + base;
        if (!sub.startsWith("/")) sub = "/" + sub;

        String combined = base + sub;

        // collapse "///" into "/"
        return combined.replaceAll("//+", "/");
    }

    private static String firstMappingPath(String[] valuePaths, String[] pathPaths) {
        String valuePath = firstText(valuePaths);
        if (StringUtils.hasText(valuePath)) {
            return valuePath;
        }
        return firstText(pathPaths);
    }

    private static String firstText(String[] paths) {
        if (paths == null) {
            return "";
        }

        for (String path : paths) {
            if (StringUtils.hasText(path)) {
                return path.trim();
            }
        }
        return "";
    }

    private static String resolveValue(String value, StringValueResolver valueResolver) {
        if (!StringUtils.hasText(value) || valueResolver == null) {
            return value;
        }

        try {
            String resolved = valueResolver.resolveStringValue(value);
            return resolved != null ? resolved : value;
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }
}
