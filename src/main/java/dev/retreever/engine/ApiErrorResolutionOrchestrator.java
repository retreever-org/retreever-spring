/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * https://opensource.org/licenses/MIT
 */

package dev.retreever.engine;

import dev.retreever.config.SchemaConfig;
import dev.retreever.endpoint.model.ApiError;
import dev.retreever.endpoint.resolver.ApiErrorResolver;
import dev.retreever.repo.ApiErrorRegistry;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Orchestrates complete ApiError resolution from ControllerAdvices.
 * Scans @ExceptionHandler methods → extracts exceptions → registers in ApiErrorRegistry.
 */
public class ApiErrorResolutionOrchestrator {

    private final ApiErrorRegistry errorRegistry;
    private final Predicate<Class<?>> basePackageFilter;

    public ApiErrorResolutionOrchestrator(ApiErrorRegistry errorRegistry) {
        this.errorRegistry = errorRegistry;
        this.basePackageFilter = createBasePackageFilter();
    }

    /**
     * Scans ALL ControllerAdvice classes for @ExceptionHandler methods.
     */
    public void resolveAllErrors(Set<Class<?>> controllerAdvices) {
        for (Class<?> advice : controllerAdvices) {
            if (!basePackageFilter.test(advice)) continue;

            List<Method> handlerMethods = getExceptionHandlerMethods(advice);
            List<ApiError> errors = new ApiErrorResolver().resolve(handlerMethods);

            // Register ALL resolved errors
            errors.forEach(errorRegistry::register);
        }
    }

    private List<Method> getExceptionHandlerMethods(Class<?> adviceClass) {
        return Stream.of(adviceClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(ExceptionHandler.class))
                .toList();
    }

    private Predicate<Class<?>> createBasePackageFilter() {
        return clazz -> {
            if (clazz == null) return false;
            String pkg = clazz.getPackageName();
            return SchemaConfig.getBasePackages().stream()
                    .anyMatch(pkg::startsWith);
        };
    }
}

