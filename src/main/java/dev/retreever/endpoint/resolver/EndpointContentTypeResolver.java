/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.resolver;

import dev.retreever.endpoint.model.ApiEndpoint;
import dev.retreever.schema.model.JsonPropertyType;
import dev.retreever.schema.resolver.JsonPropertyTypeResolver;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Resolves the media types an endpoint consumes and produces.
 * Reads values from method-level Spring mapping annotations such as
 * {@link GetMapping}, {@link PostMapping}, {@link PutMapping},
 * {@link PatchMapping}, {@link DeleteMapping}, and {@link RequestMapping}.
 */
public class EndpointContentTypeResolver {

    private static final List<Class<? extends Annotation>> mappingTypes = List.of(
            GetMapping.class,
            PostMapping.class,
            PutMapping.class,
            PatchMapping.class,
            DeleteMapping.class,
            RequestMapping.class
    );

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

        String[] explicit = extractConsumes(method);
        List<String> consumes = toMutable(explicit);

        // infer only if empty and only for methods that accept bodies (POST/PUT/PATCH)
        if (consumes.isEmpty() && methodAllowsBody(method)) {
            resolveConsumesIfEmpty(consumes, method);
        }

        return consumes;
    }

    private static String[] extractConsumes(Method method) {
        for (Class<? extends Annotation> type : mappingTypes) {
            if (method.isAnnotationPresent(type)) {
                Annotation ann = method.getAnnotation(type);
                try {
                    return (String[]) type.getMethod("consumes").invoke(ann);
                } catch (Exception ignored) {
                }
            }
        }

        return new String[0];
    }

    private static boolean methodAllowsBody(Method method) {

        if (method.isAnnotationPresent(PostMapping.class)) return true;
        if (method.isAnnotationPresent(PutMapping.class)) return true;
        if (method.isAnnotationPresent(PatchMapping.class)) return true;

        RequestMapping req = method.getAnnotation(RequestMapping.class);
        if (req != null) {
            List<RequestMethod> m = Arrays.asList(req.method());
            return m.contains(RequestMethod.POST)
                    || m.contains(RequestMethod.PUT)
                    || m.contains(RequestMethod.PATCH);
        }

        return false;
    }

    /**
     * Resolves the consumes type by parameter level annotations such as {@code @RequestBody} or
     * resolves by parameter type such as {@code MultipartFile} or defaults to {@code MediaType.APPLICATION_FORM_URLENCODED_VALUE}.
     * Resolves only if the consumes list is empty
     *
     * @param consumes A list of MediaType
     * @param method   method
     */
    private static void resolveConsumesIfEmpty(List<String> consumes, Method method) {

        if (!consumes.isEmpty()) return;

        Annotation[][] annMatrix = method.getParameterAnnotations();
        Class<?>[] types = method.getParameterTypes();

        for (int i = 0; i < types.length; i++) {

            Class<?> type = types[i];
            Annotation[] anns = annMatrix[i];
            JsonPropertyType jsonType = JsonPropertyTypeResolver.resolve(type);

            boolean hasRequestBody = Arrays.stream(anns)
                    .anyMatch(a -> a.annotationType() == RequestBody.class);

            boolean hasModelAttr = Arrays.stream(anns)
                    .anyMatch(a -> a.annotationType() == ModelAttribute.class);

            if (MultipartFile.class.isAssignableFrom(type)) {
                consumes.add(MediaType.MULTIPART_FORM_DATA_VALUE);
                continue;
            }

            if (hasModelAttr) {
                consumes.add(MediaType.MULTIPART_FORM_DATA_VALUE);
                continue;
            }

            if (jsonType == JsonPropertyType.OBJECT) {

                if (hasRequestBody) {
                    consumes.add(MediaType.APPLICATION_JSON_VALUE);
                } else {
                    consumes.add(MediaType.MULTIPART_FORM_DATA_VALUE);
                }
            }
        }
    }


    /**
     * Resolves the list of media types the method produces.
     * Follows the same pattern as {@link #resolveConsumes(Method)}.
     */
    private static List<String> resolveProduces(Method method) {

        String[] explicit = extractProduces(method);
        List<String> produces = toMutable(explicit);

        // Always infer when explicit produces is empty
        if (produces.isEmpty()) {
            resolveProducesIfEmpty(produces, method);
        }

        return produces;
    }

    private static String[] extractProduces(Method method) {
        for (Class<? extends Annotation> type : mappingTypes) {
            if (method.isAnnotationPresent(type)) {
                Annotation ann = method.getAnnotation(type);
                try {
                    return (String[]) type.getMethod("produces").invoke(ann);
                } catch (Exception ignored) {
                }
            }
        }

        return new String[0];
    }

    /**
     * Resolves the produces type.
     * Resolves only if the produces list is empty.
     *
     * @param produces A list of MediaType
     * @param method   method
     */
    private static void resolveProducesIfEmpty(List<String> produces, Method method) {

        if (!produces.isEmpty()) return;

        Class<?> controllerClass = method.getDeclaringClass();
        Class<?> returnType = method.getReturnType();

        boolean isRest = controllerClass.isAnnotationPresent(RestController.class);
        boolean isController = controllerClass.isAnnotationPresent(Controller.class);

        if (returnType != String.class) {
            produces.add(MediaType.APPLICATION_JSON_VALUE);
            return;
        }

        if (isRest) {
            produces.add(MediaType.APPLICATION_JSON_VALUE);
            return;
        }

        if (isController) {
            produces.add(MediaType.TEXT_HTML_VALUE);
            produces.add("text/*");
        }
    }

    private static List<String> toMutable(String[] arr) {
        if (arr == null || arr.length == 0) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(arr));
    }
}
