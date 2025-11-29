/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.repo;

import dev.retreever.endpoint.model.ApiError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Registry storing ApiError definitions resolved from @ExceptionHandler methods.
 * Keyed by the exception's fully qualified class name.
 * <p>
 * No schema resolving is done here.
 * No JsonProperty is used anymore.
 */
public class ApiErrorRegistry extends DocRegistry<ApiError> {

    /**
     * Registers an ApiError using its exception class name.
     */
    public void register(ApiError error) {
        String key = error.getExceptionName();
        if (!contains(key)) {
            add(key, error);
        }
    }

    /**
     * Look up an ApiError by exception class.
     */
    public ApiError get(Class<? extends Throwable> exceptionType) {
        if (exceptionType == null) return null;
        return get(exceptionType.getName());
    }

    /**
     * Converts @ApiEndpoint(errors={...}) exception classes
     * into ApiError lookups that actually exist.
     * <p>
     * Instead of returning Strings (old model),
     * we return the resolved ApiError models directly.
     */
    public List<ApiError> resolveErrors(Class<? extends Throwable>[] exceptionTypes) {

        if (exceptionTypes == null || exceptionTypes.length == 0) {
            return List.of();
        }

        List<ApiError> out = new ArrayList<>();

        for (Class<? extends Throwable> ex : exceptionTypes) {
            ApiError err = get(ex);
            if (err != null) {
                out.add(err);
            }
        }

        return out;
    }

    public Collection<ApiError> values() {
        return getAll().values();
    }
}

