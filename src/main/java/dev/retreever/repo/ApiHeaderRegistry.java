/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.repo;

import dev.retreever.endpoint.model.ApiHeader;

import java.util.List;

/**
 * Registry for storing reusable {@link ApiHeader} definitions.
 * Allows shared headers to be declared once and referenced by name
 * across multiple endpoints.
 */
public class ApiHeaderRegistry extends DocRegistry<ApiHeader> {

    @SuppressWarnings("unchecked")
    public List<ApiHeader> getHeaders() {
        return (List<ApiHeader>) getAll().values();
    }

    /**
     * Retrieves a stored header by name (case-sensitive).
     *
     * @param name the header name
     * @return the matching ApiHeader or null
     */
    public ApiHeader getHeader(String name) {
        if (name == null) return null;
        return get(name);
    }

    /**
     * Adds a header if it has a valid name and is not already present.
     *
     * @param header the header to add
     */
    public void addHeader(ApiHeader header) {
        if (header == null || header.getName() == null) return;

        if (getHeader(header.getName()) == null) {
            add(header.getName(), header);
        }
    }

    /**
     * Adds multiple headers to the registry.
     *
     * @param newHeaders list of headers to add
     */
    public void addHeaders(List<ApiHeader> newHeaders) {
        if (newHeaders == null) return;
        newHeaders.forEach(this::addHeader);
    }
}
