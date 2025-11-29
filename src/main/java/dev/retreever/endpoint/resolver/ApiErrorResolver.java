/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.resolver;

import dev.retreever.annotation.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves {@link dev.retreever.endpoint.model.ApiError} models from controller advice methods.
 * Processes only methods annotated with {@link ExceptionHandler},
 * extracts declared exception types, determines HTTP status and
 * description (via @ApiError), and resolves the error body schema
 * from the handler's return type.
 */
public class ApiErrorResolver {

    /**
     * Scans the provided methods and produces ApiError objects for each
     * declared exception in @ExceptionHandler annotations.
     *
     * @param methods a list of potential exception handler methods
     * @return resolved error models
     */
    public List<dev.retreever.endpoint.model.ApiError> resolve(List<Method> methods) {

        List<dev.retreever.endpoint.model.ApiError> result = new ArrayList<>();

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

            // Optional @ApiError annotation (status + description)
            ApiError apiAnn =
                    method.getAnnotation(ApiError.class);

            HttpStatus status = (apiAnn != null)
                    ? apiAnn.status()
                    : HttpStatus.INTERNAL_SERVER_ERROR;

            String description = (apiAnn != null)
                    ? apiAnn.description()
                    : "";

            // Extract return Type (NOT schema)
            Type returnType = extractReturnType(method);

            // Create one ApiError per declared exception type
            for (Class<?> ex : exceptionTypes) {

                dev.retreever.endpoint.model.ApiError err = dev.retreever.endpoint.model.ApiError.create(
                        status,
                        description,
                        ex.getName()
                );

                // Store TYPE only, no schema resolving
                err.setErrorBodyType(returnType);

                result.add(err);
            }
        }

        return result;
    }

    private Type extractReturnType(Method method) {

        Type generic = method.getGenericReturnType();

        // unwrap ResponseEntity<T>
        if (generic instanceof ParameterizedType p) {
            Type raw = p.getRawType();
            if (raw instanceof Class<?> c && c == ResponseEntity.class) {
                return p.getActualTypeArguments()[0];
            }
        }

        return generic;
    }
}