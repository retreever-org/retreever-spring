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
