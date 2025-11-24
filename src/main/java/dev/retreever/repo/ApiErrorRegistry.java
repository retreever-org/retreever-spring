/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.repo;

import dev.retreever.domain.model.ApiError;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry for storing resolved {@link ApiError} definitions.
 * Ensures each exception type is documented once and provides
 * lookup utilities for endpoints referencing specific errors.
 */
public class ApiErrorRegistry extends DocRegistry<ApiError> {

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

        if (exceptionTypes == null || exceptionTypes.length == 0) {
            return List.of();
        }

        List<String> refs = new ArrayList<>(exceptionTypes.length);

        for (Class<? extends Throwable> ex : exceptionTypes) {
            refs.add(ex.getName());
        }

        return refs;
    }

}
