/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.doc.resolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the servlet context path from an {@link HttpServletRequest}.
 * Always returns a safe, trimmed string, falling back to an empty value when
 * the request or its context path is unavailable.
 */
public class ContextPathResolver {

    /**
     * Returns the context path of the incoming request.
     *
     * @param request the servlet request
     * @return the resolved context-path, or an empty string if unavailable
     */
    public String resolve(HttpServletRequest request) {
        if (request == null) return "";
        String ctx = request.getContextPath();
        return (ctx == null) ? "" : ctx.trim();
    }
}
