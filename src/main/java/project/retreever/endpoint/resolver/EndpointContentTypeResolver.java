/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.endpoint.resolver;

import org.springframework.web.bind.annotation.*;
import project.retreever.domain.model.ApiEndpoint;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EndpointContentTypeResolver {

    public static void resolve(ApiEndpoint endpoint, Method method) {
        endpoint.setConsumes(resolveConsumes(method));
        endpoint.setProduces(resolveProduces(method));
    }

    private static List<String> resolveConsumes(Method method) {

        // GET mapping rarely defines consumes, but we check anyway
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

        // default: no consumes defined
        return Collections.emptyList();
    }

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
