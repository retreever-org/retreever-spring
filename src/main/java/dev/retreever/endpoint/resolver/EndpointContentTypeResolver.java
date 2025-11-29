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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Resolves the media types an endpoint consumes and produces.
 * Reads values from method-level Spring mapping annotations such as
 * {@link GetMapping}, {@link PostMapping}, {@link PutMapping},
 * {@link PatchMapping}, {@link DeleteMapping}, and {@link RequestMapping}.
 */
public class EndpointContentTypeResolver {

    /**
     * Populates consume and produce media types for the given endpoint.
     *
     * @param endpoint the endpoint model to enrich
     * @param method   the controller method being inspected
     */
    public static void resolve(ApiEndpoint endpoint, Method method) {
        endpoint.setConsumes(resolveConsumes(method));
        endpoint.setProduces(resolveProduces(method));
    }

    /**
     * Resolves the list of media types the method consumes.
     * Checks all Spring mapping annotations in common priority order.
     */
    private static List<String> resolveConsumes(Method method) {

        // GET usually doesn't specify consumes, but still checked
        GetMapping get = method.getAnnotation(GetMapping.class);
        if (get != null && get.consumes().length > 0) {
            return Arrays.asList(get.consumes());
        }

        PostMapping post = method.getAnnotation(PostMapping.class);
        if (post != null && post.consumes().length > 0) {
            return Arrays.asList(post.consumes());
        }

        PutMapping put = method.getAnnotation(PutMapping.class);
        if (put != null && put.consumes().length > 0) {
            return Arrays.asList(put.consumes());
        }

        PatchMapping patch = method.getAnnotation(PatchMapping.class);
        if (patch != null && patch.consumes().length > 0) {
            return Arrays.asList(patch.consumes());
        }

        DeleteMapping delete = method.getAnnotation(DeleteMapping.class);
        if (delete != null && delete.consumes().length > 0) {
            return Arrays.asList(delete.consumes());
        }

        RequestMapping req = method.getAnnotation(RequestMapping.class);
        if (req != null && req.consumes().length > 0) {
            return Arrays.asList(req.consumes());
        }

        // No consumes declared
        return Collections.emptyList();
    }

    /**
     * Resolves the list of media types the method produces.
     * Follows the same pattern as {@link #resolveConsumes(Method)}.
     */
    private static List<String> resolveProduces(Method method) {

        GetMapping get = method.getAnnotation(GetMapping.class);
        if (get != null && get.produces().length > 0) {
            return Arrays.asList(get.produces());
        }

        PostMapping post = method.getAnnotation(PostMapping.class);
        if (post != null && post.produces().length > 0) {
            return Arrays.asList(post.produces());
        }

        PutMapping put = method.getAnnotation(PutMapping.class);
        if (put != null && put.produces().length > 0) {
            return Arrays.asList(put.produces());
        }

        PatchMapping patch = method.getAnnotation(PatchMapping.class);
        if (patch != null && patch.produces().length > 0) {
            return Arrays.asList(patch.produces());
        }

        DeleteMapping delete = method.getAnnotation(DeleteMapping.class);
        if (delete != null && delete.produces().length > 0) {
            return Arrays.asList(delete.produces());
        }

        RequestMapping req = method.getAnnotation(RequestMapping.class);
        if (req != null && req.produces().length > 0) {
            return Arrays.asList(req.produces());
        }

        return Collections.emptyList();
    }
}
