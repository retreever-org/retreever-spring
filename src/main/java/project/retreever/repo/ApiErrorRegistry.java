/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.repo;

import project.retreever.domain.model.ApiError;
import project.retreever.endpoint.resolver.ApiErrorResolver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Registry for storing resolved {@link ApiError} definitions.
 * Ensures each exception type is documented once and provides
 * lookup utilities for endpoints referencing specific errors.
 */
public class ApiErrorRegistry extends DocRegistry<ApiError> {

    private final ApiErrorResolver apiErrorResolver;

    public ApiErrorRegistry(ApiErrorResolver apiErrorResolver) {
        this.apiErrorResolver = apiErrorResolver;
    }

    /**
     * Resolves error models from the given handler methods,
     * registers them, and returns their reference keys.
     *
     * @param methods controller advice handler methods
     * @return list of registered error reference names
     */
    public List<String> registerApiErrors(List<Method> methods) {

        List<ApiError> errors = apiErrorResolver.resolve(methods);

        return errors.stream()
                .map(this::registerApiError)
                .toList();
    }

    /**
     * Registers a single ApiError using its exception class name as the key.
     *
     * @param apiError resolved error model
     * @return reference key used for lookup
     */
    public String registerApiError(ApiError apiError) {

        String ref = apiError.getExceptionName();

        if (!contains(ref)) {
            add(ref, apiError);
        }

        return ref;
    }

    /**
     * Returns reference keys for exception types declared on @ApiEndpoint(errors = {...}),
     * but **only if** those errors have already been registered through handler resolution.
     *
     * @param exceptionTypes exception classes referenced on an endpoint
     * @return list of matching error reference keys
     */
    public List<String> getErrorRefs(Class<? extends Throwable>[] exceptionTypes) {

        List<String> refs = new ArrayList<>();

        if (exceptionTypes == null) {
            return refs;
        }

        for (Class<? extends Throwable> ex : exceptionTypes) {
            String key = ex.getName();
            if (contains(key)) {
                refs.add(key);
            }
        }

        return refs;
    }
}
