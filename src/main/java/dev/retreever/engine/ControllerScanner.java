/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.engine;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * Scans the Spring application context for controller and advice types that
 * contribute response-body documentation.
 */
public class ControllerScanner {

    /**
     * Collects controller classes by merging handler methods from all
     * {@link RequestMappingHandlerMapping} beans.
     *
     * @param context the active Spring application context
     * @return a set of controller types
     */
    public static Set<Class<?>> scanControllers(ApplicationContext context) {

        Map<String, RequestMappingHandlerMapping> mappings =
                context.getBeansOfType(RequestMappingHandlerMapping.class);

        return mappings.values()
                .stream()
                .flatMap(m -> m.getHandlerMethods().values().stream())
                .map(HandlerMethod::getBeanType)
                .map(ControllerScanner::resolveTargetClass)
                .filter(DocumentationEligibility::isDocumentedController)
                .collect(Collectors.toSet());
    }

    /**
     * Discovers controller-advices that produce response bodies either through
     * {@link RestControllerAdvice}, class-level {@code @ResponseBody}, or
     * method-level {@code @ResponseBody} on exception handlers.
     *
     * @param context the active Spring application context
     * @return a unique set of controller-advice classes
     */
    public static Set<Class<?>> scanControllerAdvices(ApplicationContext context) {
        return Stream.concat(
                        context.getBeansWithAnnotation(ControllerAdvice.class).values().stream(),
                        context.getBeansWithAnnotation(RestControllerAdvice.class).values().stream()
                )
                .map(Object::getClass)
                .map(ControllerScanner::resolveTargetClass)
                .filter(DocumentationEligibility::isDocumentedControllerAdvice)
                .collect(Collectors.toSet());
    }

    /**
     * Resolves the actual user-defined class behind Spring's CGLIB proxy.
     *
     * @param beanOrProxy the bean class returned by Spring
     * @return the underlying user class
     */
    private static Class<?> resolveTargetClass(Class<?> beanOrProxy) {
        Class<?> superClass = beanOrProxy.getSuperclass();
        if (superClass != null && beanOrProxy.getName().contains("$$")) {
            return superClass;
        }
        return beanOrProxy;
    }
}
