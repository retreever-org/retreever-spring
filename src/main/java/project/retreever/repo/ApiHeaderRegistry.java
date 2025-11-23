package project.retreever.repo;

import project.retreever.domain.model.ApiHeader;

import java.util.List;

/**
 * Central registry holding reusable ApiHeader definitions.
 * Developers can add static/shared headers here and reference them by name.
 */
public class ApiHeaderRegistry extends DocRegistry<ApiHeader>{

    public List<ApiHeader> getHeaders() {
        return (List<ApiHeader>) getAll().values();
    }

    /**
     * Retrieves a header by name (case-insensitive).
     */
    public ApiHeader getHeader(String name) {
        if (name == null) return null;
        return get(name);
    }

    /**
     * Adds a header only if its name is non-null and not already added.
     */
    public void addHeader(ApiHeader header) {
        if (header == null || header.getName() == null) return;

        if (getHeader(header.getName()) == null) {
            add(header.getName(), header);
        }
    }

    /**
     * Adds multiple headers.
     */
    public void addHeaders(List<ApiHeader> newHeaders) {
        if (newHeaders == null) return;
        newHeaders.forEach(this::addHeader);
    }
}
