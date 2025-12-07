/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.repo;

import dev.retreever.endpoint.model.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Thread-safe singleton registry storing ApiError definitions resolved from @ExceptionHandler methods.
 * Keyed by the exception's fully qualified class name.
 * <p>
 * No schema resolving is done here.
 */
public final class ApiErrorRegistry extends DocRegistry<ApiError> {

    private static final ApiErrorRegistry INSTANCE = new ApiErrorRegistry();
    private static final Map<String, ApiError> errors = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(ApiErrorRegistry.class);

    private ApiErrorRegistry() {
    }

    public static ApiErrorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers an ApiError using its exception class name. Deduplicates automatically.
     */
    public void register(ApiError error) {
        String key = error.getErrorType().getTypeName();
        if (!contains(key)) {
            log.debug("Registering ApiError: {}", error);
            add(key, error);
        }
    }

    /**
     * Look up an ApiError by exception class.
     */
    public ApiError get(Type exceptionType) {
        if (exceptionType == null) return null;
        log.debug("Looking up ApiError for: {}", exceptionType.getTypeName());
        return get(exceptionType.getTypeName());
    }

    /**
     * Retrieves all registered ApiErrors.
     */
    public Collection<ApiError> values() {
        return getAll().values();
    }

    /**
     * Optimizes registry: log stats.
     */
    public void optimize() {
        System.out.println("ApiErrorRegistry: " + errors.size() + " unique errors registered");
    }

    /**
     * Clears all registered errors.
     */
    public void clear() {
        errors.clear();
    }

    public int size() {
        return errors.size();
    }
}
