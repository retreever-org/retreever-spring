/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.resolver;

import org.springframework.web.bind.annotation.*;
import dev.retreever.endpoint.model.ApiEndpoint;

import java.lang.reflect.Method;

/**
 * Resolves the final HTTP path and HTTP method for a controller method.
 * Combines class-level and method-level mappings and normalizes the result.
 */
public class EndpointPathAndMethodResolver {

    /**
     * Populates the endpoint path and HTTP method based on Spring mapping annotations.
     *
     * @param endpoint the endpoint model to fill
     * @param method   the controller method
     */
    public static void resolve(ApiEndpoint endpoint, Method method) {

        String classPath  = resolveClassPath(method);
        String methodPath = resolveMethodPath(method);
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

        if (mapping != null && mapping.value().length > 0) {
            return mapping.value()[0].trim();
        }
        return "";
    }

    /**
     * Resolves method-level path from mapping annotations.
     */
    public static String resolveMethodPath(Method method) {

        GetMapping get = method.getAnnotation(GetMapping.class);
        if (get != null && get.value().length > 0) {
            return get.value()[0].trim();
        }

        PostMapping post = method.getAnnotation(PostMapping.class);
        if (post != null && post.value().length > 0) {
            return post.value()[0].trim();
        }

        PutMapping put = method.getAnnotation(PutMapping.class);
        if (put != null && put.value().length > 0) {
            return put.value()[0].trim();
        }

        DeleteMapping delete = method.getAnnotation(DeleteMapping.class);
        if (delete != null && delete.value().length > 0) {
            return delete.value()[0].trim();
        }

        PatchMapping patch = method.getAnnotation(PatchMapping.class);
        if (patch != null && patch.value().length > 0) {
            return patch.value()[0].trim();
        }

        RequestMapping req = method.getAnnotation(RequestMapping.class);
        if (req != null && req.value().length > 0) {
            return req.value()[0].trim();
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
}
