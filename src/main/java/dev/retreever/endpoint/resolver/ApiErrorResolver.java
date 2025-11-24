/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.resolver;

import dev.retreever.domain.annotation.ApiError;
import dev.retreever.repo.ApiErrorRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import dev.retreever.domain.model.JsonProperty;
import dev.retreever.schema.resolver.JsonSchemaResolver;
import dev.retreever.schema.resolver.TypeResolver;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * Resolves {@link dev.retreever.domain.model.ApiError} models from controller advice methods.
 * Processes only methods annotated with {@link ExceptionHandler},
 * extracts declared exception types, determines HTTP status and
 * description (via @ApiError), and resolves the error body schema
 * from the handler's return type.
 */
public class ApiErrorResolver {

    private final JsonSchemaResolver jsonSchemaResolver;
    private final ApiErrorRegistry errorRegistry;

    public ApiErrorResolver(
            JsonSchemaResolver jsonSchemaResolver,
            ApiErrorRegistry errorRegistry
    ) {
        this.jsonSchemaResolver = jsonSchemaResolver;
        this.errorRegistry = errorRegistry;
    }

    public List<dev.retreever.domain.model.ApiError> resolve(Set<Class<?>> controllerAdvices) {

        List<dev.retreever.domain.model.ApiError> allErrors = new ArrayList<>();

        for (Class<?> adviceClass : controllerAdvices) {
            Method[] methods = adviceClass.getDeclaredMethods();

            // Delegate to existing resolver (already implemented)
            List<dev.retreever.domain.model.ApiError> resolved =
                    resolve(Arrays.asList(methods));

            if (resolved != null && !resolved.isEmpty()) {
                allErrors.addAll(resolved);
            }
        }

        // Register all resolved errors
        allErrors.forEach(errorRegistry::registerApiError);
        return allErrors;
    }


    /**
     * Scans the provided methods and produces ApiError objects for each
     * declared exception in @ExceptionHandler annotations.
     *
     * @param methods a list of potential exception handler methods
     * @return resolved error models
     */
    public List<dev.retreever.domain.model.ApiError> resolve(List<Method> methods) {

        List<dev.retreever.domain.model.ApiError> result = new ArrayList<>();

        for (Method method : methods) {

            ExceptionHandler handlerAnn = method.getAnnotation(ExceptionHandler.class);
            if (handlerAnn == null) {
                continue; // skip non-handlers
            }

            // Each declared exception type produces one ApiError entry
            Class<?>[] exceptionTypes = handlerAnn.value();
            if (exceptionTypes.length == 0) {
                continue;
            }

            // Resolve error schema from return type (once per handler)
            Class<?> returnType = extractReturnType(method);
            List<JsonProperty> resolvedSchema = resolveErrorSchema(returnType);

            // Optional @ApiError annotation (status + description)
            ApiError apiAnn =
                    method.getAnnotation(ApiError.class);

            HttpStatus status = (apiAnn != null)
                    ? apiAnn.status()
                    : HttpStatus.INTERNAL_SERVER_ERROR;

            String description = (apiAnn != null)
                    ? apiAnn.description()
                    : "";

            for (Class<?> exceptionType : exceptionTypes) {

                dev.retreever.domain.model.ApiError error = dev.retreever.domain.model.ApiError.create(
                        status,
                        description,
                        exceptionType.getName()
                );

                if (!resolvedSchema.isEmpty()) {
                    error.setErrorBody(resolvedSchema);
                }

                result.add(error);
            }
        }

        return result;
    }

    private List<JsonProperty> resolveErrorSchema(Class<?> type) {
        if (type == null || type == Void.class || type == void.class) {
            return List.of();
        }
        return jsonSchemaResolver.resolve(type);
    }

    private Class<?> extractReturnType(Method method) {

        var generic = method.getGenericReturnType();
        Class<?> raw = TypeResolver.extractRawClass(generic);

        if (raw == ResponseEntity.class && generic instanceof ParameterizedType p) {
            var inner = p.getActualTypeArguments()[0];
            return TypeResolver.extractRawClass(inner);
        }

        return raw;
    }
}
