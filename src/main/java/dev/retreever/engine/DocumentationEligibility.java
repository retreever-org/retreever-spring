/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.engine;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Centralizes the rules that decide whether a controller or advice contributes
 * response-body documentation to Retreever.
 */
public final class DocumentationEligibility {

    private static final List<Class<? extends Annotation>> REQUEST_MAPPING_TYPES = List.of(
            RequestMapping.class,
            GetMapping.class,
            PostMapping.class,
            PutMapping.class,
            DeleteMapping.class,
            PatchMapping.class
    );

    private DocumentationEligibility() {
    }

    public static boolean isDocumentedController(Class<?> controllerClass) {
        if (controllerClass == null) {
            return false;
        }

        if (hasAnnotation(controllerClass, RestController.class)) {
            return true;
        }

        if (!hasAnnotation(controllerClass, Controller.class)) {
            return false;
        }

        if (hasAnnotation(controllerClass, ResponseBody.class)) {
            return true;
        }

        return Arrays.stream(controllerClass.getDeclaredMethods())
                .anyMatch(DocumentationEligibility::isDocumentedControllerMethod);
    }

    public static boolean isDocumentedControllerMethod(Method method) {
        if (method == null || !isRequestMappingMethod(method)) {
            return false;
        }

        Class<?> controllerClass = method.getDeclaringClass();
        if (hasAnnotation(controllerClass, RestController.class)) {
            return true;
        }

        return hasAnnotation(controllerClass, Controller.class) &&
                (hasAnnotation(controllerClass, ResponseBody.class) || hasAnnotation(method, ResponseBody.class));
    }

    public static boolean isDocumentedControllerAdvice(Class<?> adviceClass) {
        if (adviceClass == null) {
            return false;
        }

        if (hasAnnotation(adviceClass, RestControllerAdvice.class)) {
            return true;
        }

        if (!hasAnnotation(adviceClass, ControllerAdvice.class)) {
            return false;
        }

        if (hasAnnotation(adviceClass, ResponseBody.class)) {
            return true;
        }

        return Arrays.stream(adviceClass.getDeclaredMethods())
                .anyMatch(DocumentationEligibility::isDocumentedExceptionHandlerMethod);
    }

    public static boolean isDocumentedExceptionHandlerMethod(Method method) {
        if (method == null || !hasAnnotation(method, ExceptionHandler.class)) {
            return false;
        }

        Class<?> adviceClass = method.getDeclaringClass();
        if (hasAnnotation(adviceClass, RestControllerAdvice.class)) {
            return true;
        }

        return hasAnnotation(adviceClass, ControllerAdvice.class) &&
                (hasAnnotation(adviceClass, ResponseBody.class) || hasAnnotation(method, ResponseBody.class));
    }

    private static boolean isRequestMappingMethod(Method method) {
        return REQUEST_MAPPING_TYPES.stream().anyMatch(type -> hasAnnotation(method, type));
    }

    private static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return element != null && AnnotatedElementUtils.hasAnnotation(element, annotationType);
    }
}
