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
 * Safely resolves servlet context-path.
 */
public class ContextPathResolver {

    public String resolve(HttpServletRequest request) {
        if (request == null) return "";
        String ctx = request.getContextPath();
        return (ctx == null) ? "" : ctx.trim();
    }
}
